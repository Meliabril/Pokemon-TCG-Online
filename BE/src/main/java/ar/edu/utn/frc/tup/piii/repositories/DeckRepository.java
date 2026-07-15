package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.repositories.projections.DeckValidationSummaryProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID>, MatchmakingDeckAccessRepository {

    List<Deck> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    @Query("select distinct d from Deck d where d.id in :deckIds")
    List<Deck> findAllWithCardsByIdIn(@Param("deckIds") List<UUID> deckIds);

    @Override
    @EntityGraph(attributePaths = {"cards", "cards.card"})
    @Query("select d from Deck d where d.id = :deckId and d.owner.id = :ownerId")
    Optional<Deck> findByIdAndOwnerIdWithCards(@Param("deckId") UUID deckId, @Param("ownerId") UUID ownerId);

    @Query("select d from Deck d where d.id = :deckId and d.owner.id = :ownerId")
    Optional<Deck> findMetadataByIdAndOwnerId(@Param("deckId") UUID deckId, @Param("ownerId") UUID ownerId);

    @Override
    @EntityGraph(attributePaths = {"cards", "cards.card"})
    @Query("select d from Deck d where d.owner.id = :ownerId and d.active = true")
    Optional<Deck> findByOwnerIdAndActiveTrue(@Param("ownerId") UUID ownerId);

    @Query("select d from Deck d where d.owner.id = :ownerId and d.active = true")
    Optional<Deck> findActiveMetadataByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("""
            select coalesce(sum(dc.quantity), 0) as totalCards,
                   coalesce(sum(case when card.setCode not in :setCodes then 1 else 0 end), 0) as cardsOutsideSet,
                   coalesce(sum(case when card.supertype = ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype.POKEMON and lower(card.subtype) = 'basic' then 1 else 0 end), 0) as basicPokemonCards
            from DeckCard dc
            join dc.card card
            where dc.deck.id = :deckId
            """)
    DeckValidationSummaryProjection findValidationSummary(
            @Param("deckId") UUID deckId,
            @Param("setCodes") Collection<String> setCodes);

    @Query("""
            select card.name
            from DeckCard dc
            join dc.card card
            where dc.deck.id = :deckId
              and card.category <> :basicEnergyCategory
            group by card.name
            having sum(dc.quantity) > :maxCopies
            order by card.name
            """)
    List<String> findDuplicatedCardNames(
            @Param("deckId") UUID deckId,
            @Param("basicEnergyCategory") CardCategory basicEnergyCategory,
            @Param("maxCopies") int maxCopies);

    long countByOwnerId(UUID ownerId);
}
