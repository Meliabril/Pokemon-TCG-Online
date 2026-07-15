package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameCardInstanceRepository extends JpaRepository<GameCardInstance, UUID>, GameCardInstanceAccessRepository {

    List<GameCardInstance> findByGame_Id(UUID gameId);

    List<GameCardInstance> findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(
            UUID gameId,
            UUID ownerUserId,
            CardZone zone);

    Optional<GameCardInstance> findByIdAndGame_IdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId);

    Optional<GameCardInstance> findFirstByGame_IdAndOwnerUserIdAndCardIdAndZoneOrderByZonePositionAsc(
            UUID gameId,
            UUID ownerUserId,
            UUID cardId,
            CardZone zone);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update GameCardInstance g
               set g.zone = case
                       when g.id = :activeInstanceId then ar.edu.utn.frc.tup.piii.dtos.enums.CardZone.BENCH
                       when g.id = :benchInstanceId then ar.edu.utn.frc.tup.piii.dtos.enums.CardZone.ACTIVE
                       else g.zone
                   end,
                   g.zonePosition = case
                       when g.id = :activeInstanceId then :benchSlot
                       when g.id = :benchInstanceId then 0
                       else g.zonePosition
                   end
             where g.id in (:activeInstanceId, :benchInstanceId)
            """)
    int swapActiveWithBench(
            @Param("activeInstanceId") UUID activeInstanceId,
            @Param("benchInstanceId") UUID benchInstanceId,
            @Param("benchSlot") Integer benchSlot);

    @Override
    default List<GameCardInstance> findByGameId(UUID gameId) {
        return findByGame_Id(gameId);
    }
}
