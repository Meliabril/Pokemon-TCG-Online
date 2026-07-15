package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GameActionLogService {

    GameActionLogEntryDto recordAcceptedAction(
            UUID gameId,
            UUID actorUserId,
            GameActionType actionType,
            Map<String, Object> payload,
            Map<String, Object> result,
            Integer version,
            UUID clientActionId);

    List<GameActionLogEntryDto> getHistory(UUID gameId);
}
