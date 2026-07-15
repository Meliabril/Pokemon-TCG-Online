package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.MatchmakingQueueEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingQueueEntryRepository extends JpaRepository<MatchmakingQueueEntry, UUID>, MatchmakingQueueAccessRepository {

    @Override
    Optional<MatchmakingQueueEntry> findByUser_Id(UUID userId);

    @Override
    boolean existsByUser_Id(UUID userId);

    @Override
    void deleteByUser_Id(UUID userId);

    @Override
    void delete(MatchmakingQueueEntry entry);

    @Override
    long count();

    @Override
    MatchmakingQueueEntry save(MatchmakingQueueEntry entry);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Override
    Optional<MatchmakingQueueEntry> findFirstByUser_IdNotAndDeckIdIsNotNullAndCustomMatchCodeIsNullOrderByCreatedAtAsc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Override
    Optional<MatchmakingQueueEntry> findByCustomMatchCode(String customMatchCode);
}
