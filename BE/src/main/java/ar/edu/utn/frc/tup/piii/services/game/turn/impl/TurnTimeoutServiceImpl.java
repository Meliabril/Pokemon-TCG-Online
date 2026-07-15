package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackChoiceService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionResultPublisher;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnService;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnTimeoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TurnTimeoutServiceImpl implements TurnTimeoutService {

    private static final String PROMOTION_TIMEOUT_REASON = "PROMOTION_TIMEOUT";

    private final GameRepository gameRepository;
    private final GameParticipantStateService gameParticipantStateService;
    private final GameStateQueryService gameStateQueryService;
    private final GameSnapshotService gameSnapshotService;
    private final TurnService turnService;
    private final AttackChoiceService attackChoiceService;
    private final GameActionResultPublisher gameActionResultPublisher;
    private final GameEventFactory gameEventFactory;
    private final GameRealtimeEventService gameRealtimeEventService;

    @Override
    @Transactional
    public void applyTimeoutIfDue(UUID gameId) {
        Game game = gameRepository.findDetailByIdForUpdate(gameId).orElse(null);
        if (game == null || !isStillDue(game)) {
            return;
        }

        // A pending bench->active promotion is not a normal turn: the affected player
        // cannot play, attack, draw, attach energy, evolve, use trainers or end the turn -
        // the only legal action is PROMOTE_BENCH_POKEMON (see PromotionValidator). Routing
        // this case through the generic forceEndTurn()/consecutive-timeout logic below would
        // silently pass the turn instead of resolving the obligation, which is exactly the bug
        // being fixed here: if the player fails to promote in time, the game must end
        // immediately in favor of the opponent, not advance to another turn.
        GameStateDto currentState = gameStateQueryService.buildVisibleState(game);
        ResolutionStateDto resolution = currentState.resolution();
        if (resolution != null && resolution.hasPendingPromotion()) {
            UUID playerWaitingForPromotion = resolution.playerToPromoteId();
            UUID opponentOfPromotingPlayer =
                    gameParticipantStateService.findOpponentUserId(gameId, playerWaitingForPromotion);
            finishGameWithReason(game, opponentOfPromotingPlayer, PROMOTION_TIMEOUT_REASON);
            return;
        }

        UUID timedOutUserId = timedOutUserId(game, resolution);
        UUID opponentUserId = gameParticipantStateService.findOpponentUserId(gameId, timedOutUserId);
        int newTimeoutCount = incrementTimeoutCount(gameId, timedOutUserId);
        resetTimeoutCount(gameId, opponentUserId);

        if (newTimeoutCount >= MAX_CONSECUTIVE_TIMEOUTS) {
            finishGameByTimeout(game, opponentUserId);
        } else {
            forceEndTurn(game, timedOutUserId, currentState);
        }
    }

    private UUID timedOutUserId(Game game, ResolutionStateDto resolution) {
        if (resolution != null && resolution.hasPendingAttackChoice()) {
            return resolution.pendingChoicePlayerId();
        }
        return game.getActivePlayerId();
    }

    private boolean isStillDue(Game game) {
        return GameStatus.ACTIVE.equals(game.getStatus())
                && game.getActivePlayerId() != null
                && game.getTurnStartedAt() != null
                && !game.getTurnStartedAt().isAfter(Instant.now().minus(TURN_TIMEOUT));
    }

    private int incrementTimeoutCount(UUID gameId, UUID userId) {
        GameParticipant participant = gameParticipantStateService.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new IllegalStateException("Participant not found for timeout processing"));
        int newCount = (participant.getConsecutiveTimeouts() == null ? 0 : participant.getConsecutiveTimeouts()) + 1;
        participant.setConsecutiveTimeouts(newCount);
        gameParticipantStateService.save(participant);
        return newCount;
    }

    private void resetTimeoutCount(UUID gameId, UUID userId) {
        gameParticipantStateService.resetConsecutiveTimeouts(gameId, userId);
    }

    private void forceEndTurn(Game game, UUID timedOutUserId, GameStateDto currentState) {
        GameActionType actionType = currentState.resolution() != null && currentState.resolution().hasPendingAttackChoice()
                ? GameActionType.RESOLVE_ATTACK_CHOICE
                : GameActionType.END_TURN;
        GameActionRequestDto syntheticRequest = new GameActionRequestDto(
                game.getId(),
                UUID.randomUUID(),
                actionType,
                currentState.stateVersion(),
                Map.of("reason", "TURN_TIMEOUT"));
        GameActionContext context = new GameActionContext(game.getId(), timedOutUserId, syntheticRequest, currentState);

        GameActionExecutionResult executionResult = currentState.resolution() != null
                && currentState.resolution().hasPendingAttackChoice()
                ? attackChoiceService.resolveAttackChoiceForTimeout(context)
                : turnService.expireTimedOutTurn(context);

        gameActionResultPublisher.publish(
                game.getId(),
                game,
                currentState,
                executionResult,
                timedOutUserId,
                syntheticRequest.clientActionId(),
                syntheticRequest.payload(),
                actionType.name());
    }

    private void finishGameByTimeout(Game game, UUID winnerUserId) {
        finishGameWithReason(game, winnerUserId, "TURN_TIMEOUT");
    }

    private void finishGameWithReason(Game game, UUID winnerUserId, String reason) {
        int newStateVersion = game.getStateVersion() + 1;
        game.setStatus(GameStatus.FINISHED);
        game.setCurrentPhase(null);
        game.setWinnerPlayerId(winnerUserId);
        game.setResolutionState(Map.of());
        game.setFinishedAt(Instant.now());
        game.setStateVersion(newStateVersion);
        gameRepository.save(game);

        GameStateDto canonicalState = gameStateQueryService.buildVisibleState(game);
        gameSnapshotService.saveSnapshot(game.getId(), newStateVersion, canonicalState, winnerUserId);

        GameEventDto event = gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.GAME_FINISHED,
                newStateVersion,
                Map.of("winnerPlayerId", winnerUserId.toString(), "reason", reason));
        gameRealtimeEventService.dispatch(event);

        for (UUID playerId : gameParticipantStateService.findPlayerIds(game.getId())) {
            GameStateDto visibleState = gameStateQueryService.buildVisibleState(game, playerId);
            gameRealtimeEventService.dispatchStateSync(visibleState, playerId);
        }
    }
}
