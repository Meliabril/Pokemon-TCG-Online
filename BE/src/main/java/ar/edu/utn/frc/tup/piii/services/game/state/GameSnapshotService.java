package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;

import java.util.Optional;
import java.util.UUID;

public interface GameSnapshotService {

    void saveSnapshot(UUID gameId, int stateVersion, GameStateDto gameState, UUID triggeredByUserId);

    void updateLatestSnapshot(UUID gameId, GameStateDto gameState);

    Optional<GameStateDto> findLatestVisibleState(UUID gameId, UUID viewerUserId);
}

