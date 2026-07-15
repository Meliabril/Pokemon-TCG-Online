package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameEventRepository extends JpaRepository<GameEvent, UUID>, GameEventAccessRepository {

    @Override
    <S extends GameEvent> S save(S event);

    List<GameEvent> findByGame_IdOrderByCreatedAtAsc(UUID gameId);

    @Query("""
            select e
            from GameEvent e
            where e.game.id = :gameId
              and (e.visibleToUserId is null or e.visibleToUserId = :viewerUserId)
            order by e.version asc, e.createdAt asc
            """)
    List<GameEvent> findVisibleEvents(@Param("gameId") UUID gameId, @Param("viewerUserId") UUID viewerUserId);

    @Query("""
            select e
            from GameEvent e
            where e.game.id = :gameId
              and (e.visibleToUserId is null or e.visibleToUserId = :viewerUserId)
            order by e.createdAt asc
            """)
    List<GameEvent> findVisibleByGameIdAndViewerUserId(
            @Param("gameId") UUID gameId,
            @Param("viewerUserId") UUID viewerUserId);
}
