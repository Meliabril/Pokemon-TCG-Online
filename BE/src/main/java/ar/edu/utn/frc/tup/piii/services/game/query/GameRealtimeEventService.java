package ar.edu.utn.frc.tup.piii.services.game.query;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.Map;
import java.util.UUID;

public interface GameRealtimeEventService {

    void dispatchPublic(UUID gameId, GameEventType type, int version, Map<String, Object> payload);

    void dispatchPrivate(UUID gameId, GameEventType type, int version, Map<String, Object> payload, UUID viewerUserId);

    void dispatchStateSync(GameStateDto state, UUID viewerUserId);

    void dispatch(GameEventDto event);
}
