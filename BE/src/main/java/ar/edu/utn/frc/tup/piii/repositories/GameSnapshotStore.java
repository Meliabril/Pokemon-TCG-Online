package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;

import java.util.Optional;
import java.util.UUID;

public interface GameSnapshotStore {

    <S extends GameStateSnapshot> S save(S snapshot);

    Optional<GameStateSnapshot> findLatestByGameId(UUID gameId);
}
