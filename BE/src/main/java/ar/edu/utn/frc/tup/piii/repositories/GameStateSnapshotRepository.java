package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameStateSnapshotRepository extends JpaRepository<GameStateSnapshot, UUID>, GameSnapshotStore {

    @Override
    <S extends GameStateSnapshot> S save(S snapshot);

    Optional<GameStateSnapshot> findFirstByGame_IdOrderByVersionDesc(UUID gameId);

    @Override
    default Optional<GameStateSnapshot> findLatestByGameId(UUID gameId) {
        return findFirstByGame_IdOrderByVersionDesc(gameId);
    }
}
