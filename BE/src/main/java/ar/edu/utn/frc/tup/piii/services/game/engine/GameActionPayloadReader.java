package ar.edu.utn.frc.tup.piii.services.game.engine;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface GameActionPayloadReader {

    UUID requiredUuid(Map<String, Object> payload, String key);

    Optional<UUID> optionalUuid(Map<String, Object> payload, String key);
}
