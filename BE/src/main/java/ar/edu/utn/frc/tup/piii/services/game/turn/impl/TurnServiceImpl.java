package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateTransitions;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TurnServiceImpl implements TurnService {

    private final GameLookupService gameLookupService;
    private final GameParticipantStateService gameParticipantStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;
    private final BetweenTurnsResolutionService betweenTurnsResolutionService;
    private final AvailableActionsFactory availableActionsFactory;
    private final AbilityUsageTracker abilityUsageTracker;

    @Override
    public GameActionExecutionResult drawCard(GameActionContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();
        GameStateDto currentState = context.currentState();
        int newStateVersion = currentState.stateVersion() + 1;

        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        if (deckCards.isEmpty()) {
            return finishDueToEmptyDeck(context, newStateVersion);
        }

        GameCardInstance drawnCard = deckCards.getFirst();
        int nextHandPosition = gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND);
        drawnCard.setZone(CardZone.HAND);
        drawnCard.setZonePosition(nextHandPosition);
        drawnCard.setFaceDown(false);
        gameCardInstanceStateService.save(drawnCard);

        GameStateDto newState = currentState.toBuilder()
                .stateVersion(newStateVersion)
                .players(playersAfterDraw(currentState, actorUserId, drawnCard))
                .turn(currentState.turn().toBuilder()
                        .currentPhase(TurnPhase.MAIN)
                        .build())
                .board(boardAfterDraw(currentState, actorUserId, drawnCard))
                .actions(currentState.actions().toBuilder()
                        .availableActions(availableActionsFactory.mainPhaseActions(false))
                        .build())
                .updatedAt(Instant.now())
                .build();

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.CARD_DRAWN,
                newStateVersion,
                Map.of("playerId", actorUserId.toString(), "cardsDrawn", 1)));
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.CARD_DRAWN,
                newStateVersion,
                Map.of("playerId", actorUserId.toString(), "cardId", drawnCard.getCardId().toString()),
                actorUserId));
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.PHASE_CHANGED,
                newStateVersion,
                Map.of("phase", TurnPhase.MAIN.name())));

        return new GameActionExecutionResult(newState, events);
    }

    @Override
    public GameActionExecutionResult endTurn(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID opponentUserId = opponentUserId(context.gameId(), actorUserId);
        GameStateDto currentState = context.currentState();
        int newStateVersion = currentState.stateVersion() + 1;

        int nextTurnNumber = currentState.turn().turnNumber() + 1;

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                currentState.gameId(),
                GameEventType.PHASE_CHANGED,
                newStateVersion,
                Map.of("phase", TurnPhase.BETWEEN_TURNS.name(), "activePlayerId", actorUserId.toString())));

        BetweenTurnsResolutionService.BetweenTurnsResolutionResult betweenTurnsResolution = betweenTurnsResolutionService.resolveBetweenTurns(
                context.gameId(),
                actorUserId,
                opponentUserId,
                currentState.turn().turnNumber(),
                nextTurnNumber,
                newStateVersion);
        events.addAll(betweenTurnsResolution.events());

        if (betweenTurnsResolution.gameFinished() || betweenTurnsResolution.promotionPending()) {
            GameStateDto resolvedState = GameStateTransitions.fromCurrentGame(
                    currentState,
                    gameLookupService.getRequiredGame(context.gameId()),
                    newStateVersion);
            return new GameActionExecutionResult(resolvedState, events);
        }

        Game game = gameLookupService.getRequiredGame(context.gameId());
        abilityUsageTracker.clearTurnUsage(game);

        GameStateDto newState = currentState.toBuilder()
                .stateVersion(newStateVersion)
                .players(playersFromBetweenTurns(currentState, betweenTurnsResolution))
                .turn(currentState.turn().toBuilder()
                        .currentPhase(TurnPhase.DRAW)
                        .turnNumber(nextTurnNumber)
                        .activePlayerId(opponentUserId)
                        .turnStartedAt(Instant.now())
                        .energyAttachedThisTurn(false)
                        .supporterPlayedThisTurn(false)
                        .retreatedThisTurn(false)
                        .build())
                .resolution(null)
                .actions(currentState.actions().toBuilder()
                        .availableActions(availableActionsFactory.drawPhaseActions())
                        .build())
                .updatedAt(Instant.now())
                .build();

        events.add(gameEventFactory.publicEvent(
                currentState.gameId(),
                GameEventType.PHASE_CHANGED,
                newStateVersion,
                Map.of("phase", TurnPhase.DRAW.name(), "activePlayerId", opponentUserId.toString())));
        events.add(gameEventFactory.publicEvent(
                currentState.gameId(),
                GameEventType.TURN_STARTED,
                newStateVersion,
                Map.of("playerId", opponentUserId.toString(), "turnNumber", nextTurnNumber)));

        return new GameActionExecutionResult(newState, events);
    }

    @Override
    public GameActionExecutionResult expireTimedOutTurn(GameActionContext context) {
        GameStateDto timeoutState = stateWithoutPendingResolution(context.currentState());
        GameActionContext timeoutContext = new GameActionContext(
                context.gameId(),
                context.actorUserId(),
                context.request(),
                timeoutState);

        if (TurnPhase.DRAW.equals(timeoutState.turn().currentPhase())) {
            GameActionExecutionResult drawResult = drawCard(timeoutContext);
            if (GameStatus.FINISHED.equals(drawResult.gameState().status())) {
                return drawResult;
            }

            GameActionContext endTurnContext = new GameActionContext(
                    context.gameId(),
                    context.actorUserId(),
                    context.request(),
                    drawResult.gameState());
            GameActionExecutionResult endTurnResult = endTurn(endTurnContext);

            List<GameEventDto> combinedEvents = new ArrayList<>(drawResult.emittedEvents());
            combinedEvents.addAll(endTurnResult.emittedEvents());
            return new GameActionExecutionResult(endTurnResult.gameState(), List.copyOf(combinedEvents));
        }

        return endTurn(timeoutContext);
    }

    private GameActionExecutionResult finishDueToEmptyDeck(GameActionContext context, int newStateVersion) {
        UUID actorUserId = context.actorUserId();
        UUID winnerUserId = opponentUserId(context.gameId(), actorUserId);
        Game game = gameLookupService.getRequiredGame(context.gameId());
        game.setStatus(GameStatus.FINISHED);
        game.setCurrentPhase(null);
        game.setWinnerPlayerId(winnerUserId);
        game.setFinishedAt(Instant.now());

        GameStateDto currentState = context.currentState();
        GameStateDto newState = currentState.toBuilder()
                .status(GameStatus.FINISHED)
                .stateVersion(newStateVersion)
                .turn(currentState.turn().toBuilder()
                        .currentPhase(null)
                        .energyAttachedThisTurn(false)
                        .supporterPlayedThisTurn(false)
                        .retreatedThisTurn(false)
                        .build())
                .resolution(null)
                .actions(currentState.actions().toBuilder()
                        .availableActions(availableActionsFactory.noActions())
                        .build())
                .updatedAt(Instant.now())
                .build();

        return new GameActionExecutionResult(
                newState,
                List.of(gameEventFactory.publicEvent(
                        context.gameId(),
                        GameEventType.GAME_FINISHED,
                        newStateVersion,
                        Map.of("winnerPlayerId", winnerUserId.toString(), "reason", "EMPTY_DECK"))));
    }

    private GameStateDto stateWithoutPendingResolution(GameStateDto currentState) {
        ResolutionStateDto resolution = currentState.resolution();
        if (resolution == null || resolution.resolutionType() == null) {
            return currentState;
        }

        return currentState.toBuilder()
                .resolution(null)
                .build();
    }

    private UUID opponentUserId(UUID gameId, UUID actorUserId) {
        return gameParticipantStateService.findOpponentUserId(gameId, actorUserId);
    }

    private Map<UUID, PlayerStateDto> playersAfterDraw(
            GameStateDto currentState,
            UUID actorUserId,
            GameCardInstance drawnCard) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>(currentState.players());
        PlayerStateDto playerState = players.get(actorUserId);
        if (playerState == null) {
            playerState = PlayerStateDto.builder().build();
        }

        List<UUID> cardIdsInHand = new ArrayList<>(playerState.cardIdsInHand());
        cardIdsInHand.add(drawnCard.getCardId());
        List<UUID> cardInstanceIdsInHand = new ArrayList<>(playerState.cardInstanceIdsInHand());
        cardInstanceIdsInHand.add(drawnCard.getId());

        players.put(actorUserId, playerState.toBuilder()
                .cardIdsInHand(List.copyOf(cardIdsInHand))
                .cardInstanceIdsInHand(List.copyOf(cardInstanceIdsInHand))
                .build());
        return Map.copyOf(players);
    }

    private BoardStateDto boardAfterDraw(GameStateDto currentState, UUID actorUserId, GameCardInstance drawnCard) {
        Map<UUID, CardZone> zones = new LinkedHashMap<>(currentState.board().zoneByCardReferenceId());
        zones.put(drawnCard.getId(), CardZone.HAND);
        zones.put(drawnCard.getCardId(), CardZone.HAND);

        Map<UUID, UUID> owners = new LinkedHashMap<>(currentState.board().ownerByCardReferenceId());
        owners.put(drawnCard.getId(), actorUserId);
        owners.put(drawnCard.getCardId(), actorUserId);

        return currentState.board().toBuilder()
                .zoneByCardReferenceId(Map.copyOf(zones))
                .ownerByCardReferenceId(Map.copyOf(owners))
                .build();
    }

    private Map<UUID, PlayerStateDto> playersFromBetweenTurns(
            GameStateDto currentState,
            BetweenTurnsResolutionService.BetweenTurnsResolutionResult betweenTurnsResolution) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>(currentState.players());
        for (UUID playerId : currentState.playerIds()) {
            PlayerStateDto playerState = players.get(playerId);
            if (playerState == null) {
                playerState = PlayerStateDto.builder().build();
            }

            Integer benchCount = betweenTurnsResolution.benchCountByPlayer().get(playerId);
            int safeBenchCount = 0;
            if (benchCount != null) {
                safeBenchCount = benchCount;
            }

            List<SpecialConditionType> activeConditions = betweenTurnsResolution.activeConditionsByPlayer().get(playerId);
            players.put(playerId, playerState.toBuilder()
                    .benchPokemonCount(safeBenchCount)
                    .activePokemonConditions(activeConditions)
                    .build());
        }

        return Map.copyOf(players);
    }
}
