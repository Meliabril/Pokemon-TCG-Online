package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import org.springframework.stereotype.Component;

@Component
public class GameActionLogMapper {

    public GameActionLogEntryDto toDto(GameActionLog log) {
        return new GameActionLogEntryDto(
                log.getId(),
                log.getActorUserId(),
                log.getActionType(),
                log.getPayload(),
                log.getResult(),
                log.getVersion(),
                log.getClientActionId(),
                log.getCreatedAt());
    }
}
