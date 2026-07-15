package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryProjection;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryResultProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID>, GameAccessRepository, MatchmakingGameAccessRepository {

    @Override
    Optional<Game> findById(UUID gameId);

    @Override
    boolean existsById(UUID gameId);

    @Override
    <S extends Game> S save(S game);

    @EntityGraph(attributePaths = "participants")
    @Query("select distinct g from Game g where g.id = :gameId")
    Optional<Game> findDetailById(@Param("gameId") UUID gameId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "participants")
    @Query("select distinct g from Game g where g.id = :gameId")
    Optional<Game> findDetailByIdForUpdate(@Param("gameId") UUID gameId);

    @Query("""
            select g.id
            from Game g
            where g.status = :status
              and g.turnStartedAt is not null
              and g.turnStartedAt <= :cutoff
            """)
    List<UUID> findActiveGameIdsWithExpiredTurn(
            @Param("status") GameStatus status,
            @Param("cutoff") Instant cutoff);

    @Override
    default Optional<Game> findLatestByParticipantUserIdAndStatusIn(UUID userId, Collection<GameStatus> statuses) {
        return findRecentIdsByParticipantUserIdAndStatusIn(userId, statuses, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .flatMap(this::findDetailById);
    }

    @Query("""
            select g.id
            from Game g
            join g.participants participant
            where participant.userId = :userId
              and g.status in :statuses
            order by g.createdAt desc
            """)
    List<UUID> findRecentIdsByParticipantUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<GameStatus> statuses,
            Pageable pageable);

    @Query("""
            select g
            from Game g
            join g.participants participant
            where participant.userId = :userId
              and g.status in :statuses
            order by g.createdAt desc
            """)
    List<Game> findRecentByParticipantUserIdAndStatusIn(
            @Param("userId") UUID userId,
            @Param("statuses") Collection<GameStatus> statuses,
            Pageable pageable);

    @Query(value = """
            select g.id as matchId,
                   g.winnerPlayerId as winnerPlayerId,
                   opponent.username as opponentName,
                   g.finishedAt as date,
                   g.turnNumber as turnsPlayed
            from Game g
            join g.participants participant
            join g.participants opponentParticipant
            join User opponent on opponent.id = opponentParticipant.userId
            where participant.userId = :userId
              and opponentParticipant.userId <> :userId
              and g.status = :status
              and g.winnerPlayerId is not null
              and g.finishedAt is not null
              and (:includeWins = true or g.winnerPlayerId <> :userId)
              and (:includeLosses = true or g.winnerPlayerId = :userId)
              and (:finishedFrom is null or g.finishedAt >= :finishedFrom)
            order by g.finishedAt desc
            """,
            countQuery = """
            select count(g.id)
            from Game g
            join g.participants participant
            join g.participants opponentParticipant
            where participant.userId = :userId
              and opponentParticipant.userId <> :userId
              and g.status = :status
              and g.winnerPlayerId is not null
              and g.finishedAt is not null
              and (:includeWins = true or g.winnerPlayerId <> :userId)
              and (:includeLosses = true or g.winnerPlayerId = :userId)
              and (:finishedFrom is null or g.finishedAt >= :finishedFrom)
            """)
    Page<MatchHistoryProjection> findMatchHistoryByParticipantUserId(
            @Param("userId") UUID userId,
            @Param("status") GameStatus status,
            @Param("includeWins") boolean includeWins,
            @Param("includeLosses") boolean includeLosses,
            @Param("finishedFrom") Instant finishedFrom,
            Pageable pageable);

    @Query("""
            select g.id as matchId,
                   g.winnerPlayerId as winnerPlayerId,
                   g.finishedAt as date
            from Game g
            join g.participants participant
            where participant.userId = :userId
              and g.status = :status
              and g.winnerPlayerId is not null
              and g.finishedAt is not null
            order by g.finishedAt desc
            """)
    List<MatchHistoryResultProjection> findFinishedMatchResultsByParticipantUserId(
            @Param("userId") UUID userId,
            @Param("status") GameStatus status);
}
