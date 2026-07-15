package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ConcurrentGameStateException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutor;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionResultPublisher;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameService;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.RuleValidator;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Default orchestration service for game actions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameServiceImpl implements GameService {

    private final GameLookupService gameLookupService;
    private final GameDataService gameDataService;
    private final GameStateQueryService gameStateQueryService;
    private final GameSnapshotService gameSnapshotService;
    private final RuleValidator ruleValidator;
    private final GameActionExecutor gameActionExecutor;
    private final GameActionResultPublisher gameActionResultPublisher;
    private final GameParticipantStateService gameParticipantStateService;

    /**
     * Validates and executes a game action without embedding game rules in this layer.
     */
    @Override
    @Transactional
    public GameActionResponseDto executeAction(UUID gameId, UUID actorUserId, GameActionRequestDto request) {
        long start = System.currentTimeMillis();
        String actionName = request.actionType() != null ? request.actionType().name() : "UNKNOWN";
        log.info("[{}] Inicio gameId={}, playerId={}", actionName, gameId, actorUserId);

        gameLookupService.assertParticipant(gameId, actorUserId);

        long t1 = System.currentTimeMillis();
        Game game = gameDataService.getRequiredGameForUpdate(gameId);
        Optional<GameStateDto> latestVisibleState = gameSnapshotService.findLatestVisibleState(gameId, actorUserId);
        GameStateDto currentState;
        if (latestVisibleState.isPresent() && latestVisibleState.get().stateVersion() >= game.getStateVersion()) {
            currentState = latestVisibleState.get();
        } else {
            currentState = gameStateQueryService.buildVisibleState(game);
        }
        log.info("[{}] Cargar partida/datos: {} ms", actionName, System.currentTimeMillis() - t1);

        if (!request.expectedStateVersion().equals(currentState.stateVersion())) {
            throw new ConcurrentGameStateException("Expected state version does not match the persisted game state");
        }

        GameActionContext context = new GameActionContext(gameId, actorUserId, request, currentState);

        long t2 = System.currentTimeMillis();
        ruleValidator.validate(context);
        GameActionExecutionResult executionResult = gameActionExecutor.execute(context);
        log.info("[{}] Logica ejecutar accion: {} ms", actionName, System.currentTimeMillis() - t2);

        long t3 = System.currentTimeMillis();
        GameActionResponseDto response = gameActionResultPublisher.publish(
                gameId,
                game,
                currentState,
                executionResult,
                actorUserId,
                request.clientActionId(),
                request.payload(),
                actionName);
        log.info("[{}] Guardar y publicar: {} ms", actionName, System.currentTimeMillis() - t3);

        gameParticipantStateService.resetConsecutiveTimeouts(gameId, actorUserId);

        log.info("[{}] Total: {} ms", actionName, System.currentTimeMillis() - start);
        return response;
    }
}
