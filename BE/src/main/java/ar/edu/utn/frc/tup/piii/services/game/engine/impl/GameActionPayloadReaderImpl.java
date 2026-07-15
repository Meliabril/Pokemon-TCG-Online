package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.exceptions.BadRequestException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameActionPayloadReaderImpl implements GameActionPayloadReader {

    @Override
    public UUID requiredUuid(Map<String, Object> payload, String key) {
        return optionalUuid(payload, key)
                .orElseThrow(() -> new BadRequestException("Payload field '" + key + "' must be a UUID"));
    }

    @Override
    public Optional<UUID> optionalUuid(Map<String, Object> payload, String key) {
        if (payload == null) {
            return Optional.empty();
        }

        Object value = payload.get(key);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Optional.of(UUID.fromString(string));
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Payload field '" + key + "' must be a UUID");
            }
        }
        return Optional.empty();
    }
}
