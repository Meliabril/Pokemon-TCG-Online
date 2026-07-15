package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GameEventFactoryImpl implements GameEventFactory {

    @Override
    public GameEventDto publicEvent(UUID gameId, GameEventType type, int version, Map<String, Object> payload) {
        return new GameEventDto(UUID.randomUUID(), gameId, type, version, false, Instant.now(), payload);
    }

    @Override
    public GameEventDto privateEvent(
            UUID gameId,
            GameEventType type,
            int version,
            Map<String, Object> payload,
            UUID viewerUserId) {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        if (payload != null) {
            eventPayload.putAll(payload);
        }
        eventPayload.put("viewerUserId", viewerUserId.toString());
        return new GameEventDto(UUID.randomUUID(), gameId, type, version, true, Instant.now(), Map.copyOf(eventPayload));
    }

    @Override
    public GameEventDto privateStateSync(UUID gameId, int version, GameStateDto state, UUID viewerUserId) {
        return privateEvent(gameId, GameEventType.STATE_SYNC, version, Map.of("state", state), viewerUserId);
    }
}
