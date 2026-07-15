package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.entities.Deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckPersistenceService {

    List<Deck> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerUserId);

    List<Deck> findAllMetadataByOwnerIdOrderByCreatedAtAsc(UUID ownerUserId);

    Optional<Deck> findByIdAndOwnerIdWithCards(UUID deckId, UUID ownerUserId);

    Optional<Deck> findMetadataByIdAndOwnerId(UUID deckId, UUID ownerUserId);

    Optional<Deck> findActiveDeckByOwnerId(UUID ownerUserId);

    Optional<Deck> findActiveDeckMetadataByOwnerId(UUID ownerUserId);

    DeckValidationSummary getValidationSummary(UUID deckId);

    long countByOwnerId(UUID ownerUserId);

    Deck save(Deck deck);

    void delete(Deck deck);

    void flush();
}
