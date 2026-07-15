package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.mappers.GameActionLogMapper;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogWriteRepository;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogCommandService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class GameActionLogCommandServiceImpl implements GameActionLogCommandService {

    private final GameActionLogWriteRepository gameActionLogWriteRepository;
    private final GameLookupService gameLookupService;
    private final GameActionLogMapper gameActionLogMapper;

    @Transactional
    @Override
    public GameActionLogEntryDto recordAcceptedAction(
            UUID gameId,
            UUID actorUserId,
            GameActionType actionType,
            Map<String, Object> payload,
            Map<String, Object> result,
            Integer version,
            UUID clientActionId) {
        Game game = gameLookupService.getRequiredGame(gameId);

        GameActionLog log = new GameActionLog();
        log.setGame(game);
        log.setActorUserId(actorUserId);
        log.setActionType(actionType);
        if (payload == null) {
            log.setPayload(Map.of());
        } else {
            log.setPayload(Map.copyOf(payload));
        }
        if (result == null) {
            log.setResult(null);
        } else {
            log.setResult(Map.copyOf(result));
        }
        log.setVersion(version);
        log.setClientActionId(clientActionId);

        return gameActionLogMapper.toDto(gameActionLogWriteRepository.save(log));
    }

    @Override
    @Transactional
    public void logAction(
            UUID gameId,
            UUID actorUserId,
            UUID clientActionId,
            Map<String, Object> payload,
            Map<String, Object> result,
            String actionType,
            Integer newStateVersion) {
        recordAcceptedAction(
                gameId,
                actorUserId,
                GameActionType.valueOf(actionType),
                payload,
                result,
                newStateVersion,
                clientActionId);
    }
}
