package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogCommandService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameActionLogServiceImpl implements GameActionLogService {

    private final GameActionLogCommandService gameActionLogCommandService;
    private final GameHistoryQueryService gameHistoryQueryService;

    @Override
    public GameActionLogEntryDto recordAcceptedAction(
            UUID gameId,
            UUID actorUserId,
            GameActionType actionType,
            Map<String, Object> payload,
            Map<String, Object> result,
            Integer version,
            UUID clientActionId) {
        return gameActionLogCommandService.recordAcceptedAction(
                gameId,
                actorUserId,
                actionType,
                payload,
                result,
                version,
                clientActionId);
    }

    @Override
    public List<GameActionLogEntryDto> getHistory(UUID gameId) {
        return gameHistoryQueryService.getHistory(gameId);
    }
}
