package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;

import java.util.Map;
import java.util.UUID;

/**
 * Command port for persisted game action logs.
 */
public interface GameActionLogCommandService extends GameActionLogger {

    GameActionLogEntryDto recordAcceptedAction(
            UUID gameId,
            UUID actorUserId,
            GameActionType actionType,
            Map<String, Object> payload,
            Map<String, Object> result,
            Integer version,
            UUID clientActionId);
}
