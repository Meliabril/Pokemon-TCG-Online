package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.Map;
import java.util.UUID;

public interface GameEventFactory {

    GameEventDto publicEvent(UUID gameId, GameEventType type, int version, Map<String, Object> payload);

    GameEventDto privateEvent(
            UUID gameId,
            GameEventType type,
            int version,
            Map<String, Object> payload,
            UUID viewerUserId);

    GameEventDto privateStateSync(UUID gameId, int version, GameStateDto state, UUID viewerUserId);
}
