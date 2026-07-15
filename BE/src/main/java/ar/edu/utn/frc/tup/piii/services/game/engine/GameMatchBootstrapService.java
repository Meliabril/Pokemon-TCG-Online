package ar.edu.utn.frc.tup.piii.services.game.engine;

import java.util.UUID;

public interface GameMatchBootstrapService {

    UUID createMatch(UUID userId, UUID userDeckId, UUID opponentUserId, UUID opponentDeckId);
}
