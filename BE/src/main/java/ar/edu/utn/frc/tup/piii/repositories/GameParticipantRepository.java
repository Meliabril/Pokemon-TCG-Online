package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, UUID>, GameParticipantAccessRepository {

    boolean existsByGame_IdAndUserId(UUID gameId, UUID userId);

    Optional<GameParticipant> findByGame_IdAndUserId(UUID gameId, UUID userId);

    List<GameParticipant> findByGame_IdOrderByPlayerOrderAsc(UUID gameId);

    @Override
    default boolean existsByGameIdAndUserId(UUID gameId, UUID userId) {
        return existsByGame_IdAndUserId(gameId, userId);
    }

    @Override
    default List<GameParticipant> findOrderedByGameId(UUID gameId) {
        return findByGame_IdOrderByPlayerOrderAsc(gameId);
    }
}
