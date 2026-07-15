package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import org.springframework.stereotype.Component;

@Component
public class GameEventMapper {

    public GameEventDto toDto(GameEvent event) {
        return new GameEventDto(
                event.getId(),
                event.getGame().getId(),
                event.getEventType(),
                event.getVersion(),
                event.getVisibleToUserId() != null,
                event.getCreatedAt(),
                event.getPayload());
    }
}
