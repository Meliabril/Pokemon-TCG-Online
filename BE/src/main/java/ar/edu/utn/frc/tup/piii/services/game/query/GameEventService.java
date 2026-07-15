package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GameEventService {

    GameEventDto recordPublicEvent(UUID gameId, GameEventType eventType, Integer version, Map<String, Object> payload);

    GameEventDto recordPrivateEvent(
            UUID gameId,
            GameEventType eventType,
            Integer version,
            Map<String, Object> payload,
            UUID visibleToUserId);

    List<GameEventDto> getVisibleEvents(UUID gameId, UUID viewerUserId);
}
