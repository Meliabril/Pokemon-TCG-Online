package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GameActionLogEntryDto(
        UUID id,
        UUID actorUserId,
        GameActionType actionType,
        Map<String, Object> payload,
        Map<String, Object> result,
        Integer version,
        UUID clientActionId,
        Instant createdAt) {

    public GameActionLogEntryDto {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        result = result == null ? Map.of() : Map.copyOf(result);
    }
}
