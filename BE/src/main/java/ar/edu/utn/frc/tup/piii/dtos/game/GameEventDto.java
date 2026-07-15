package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GameEventDto(
        UUID eventId,
        UUID gameId,
        GameEventType eventType,
        int stateVersion,
        boolean privateEvent,
        Instant occurredAt,
        Map<String, Object> payload) {

    public GameEventDto {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

