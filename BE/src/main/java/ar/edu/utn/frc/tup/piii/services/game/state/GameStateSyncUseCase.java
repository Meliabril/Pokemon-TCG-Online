package ar.edu.utn.frc.tup.piii.services.game.state;

import java.util.UUID;

public interface GameStateSyncUseCase {

    void syncState(UUID gameId, UUID viewerUserId);
}
