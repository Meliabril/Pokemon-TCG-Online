package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Deck;

import java.util.Optional;
import java.util.UUID;

public interface MatchmakingDeckAccessRepository {

    Optional<Deck> findByIdAndOwnerIdWithCards(UUID deckId, UUID ownerId);

    Optional<Deck> findByOwnerIdAndActiveTrue(UUID ownerId);
}
