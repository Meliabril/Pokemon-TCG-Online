package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeckCardRepository extends JpaRepository<DeckCard, UUID> {

    Optional<DeckCard> findByDeckIdAndCardId(UUID deckId, UUID cardId);
}
