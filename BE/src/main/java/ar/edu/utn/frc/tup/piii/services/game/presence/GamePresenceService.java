package ar.edu.utn.frc.tup.piii.services.game.presence;

import java.util.UUID;

public interface GamePresenceService {

    void markConnected(UUID gameId, UUID userId);

    void markDisconnected(UUID gameId, UUID userId);

    void leaveGame(String sessionId, UUID gameId, UUID userId);
}
