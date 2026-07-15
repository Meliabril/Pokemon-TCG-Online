package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PokemonInPlayRepository extends JpaRepository<PokemonInPlay, UUID> {

    @EntityGraph(attributePaths = {"activeCardInstance"})
    List<PokemonInPlay> findByGame_IdAndOwnerUserIdOrderBySlotPositionAsc(UUID gameId, UUID ownerUserId);

    @EntityGraph(attributePaths = {"activeCardInstance"})
    Optional<PokemonInPlay> findByGame_IdAndOwnerUserIdAndSlotPosition(UUID gameId, UUID ownerUserId, Integer slotPosition);

    @EntityGraph(attributePaths = {"activeCardInstance"})
    Optional<PokemonInPlay> findByIdAndGame_IdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId);

    @EntityGraph(attributePaths = {"activeCardInstance"})
    List<PokemonInPlay> findByGame_IdOrderByOwnerUserIdAscSlotPositionAsc(UUID gameId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update PokemonInPlay p
               set p.slotPosition = case
                   when p.id = :activePokemonId then :benchSlot
                   when p.id = :benchPokemonId then 0
                   else p.slotPosition
               end
             where p.id in (:activePokemonId, :benchPokemonId)
            """)
    int swapActiveWithBench(
            @Param("activePokemonId") UUID activePokemonId,
            @Param("benchPokemonId") UUID benchPokemonId,
            @Param("benchSlot") Integer benchSlot);
}
