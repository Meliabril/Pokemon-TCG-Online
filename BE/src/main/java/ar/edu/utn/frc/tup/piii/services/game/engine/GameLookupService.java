package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.UUID;

public interface GameLookupService {

    Game getRequiredGame(UUID gameId);

    void assertGameExists(UUID gameId);

    void assertParticipant(UUID gameId, UUID userId);
}
