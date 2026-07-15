package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.Map;
import java.util.UUID;

/**
 * Shared persistence/publication tail for any executed game action result, used both by
 * player-submitted actions and by system-forced ones (e.g. turn timeout).
 */
public interface GameActionResultPublisher {

    GameActionResponseDto publish(
            UUID gameId,
            Game game,
            GameStateDto currentState,
            GameActionExecutionResult executionResult,
            UUID actorUserId,
            UUID clientActionId,
            Map<String, Object> payload,
            String actionTypeName);
}
