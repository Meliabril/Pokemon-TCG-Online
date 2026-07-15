package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.MatchmakingQueueEntry;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingQueueAccessRepository {

    Optional<MatchmakingQueueEntry> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);

    void delete(MatchmakingQueueEntry entry);

    long count();

    MatchmakingQueueEntry save(MatchmakingQueueEntry entry);

    Optional<MatchmakingQueueEntry> findFirstByUser_IdNotAndDeckIdIsNotNullAndCustomMatchCodeIsNullOrderByCreatedAtAsc(UUID userId);

    Optional<MatchmakingQueueEntry> findByCustomMatchCode(String customMatchCode);
}
