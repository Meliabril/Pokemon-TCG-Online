package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.entities.Deck;

import java.util.UUID;

public interface GameDeckStateService {

    Deck getRequiredDeckWithCards(UUID deckId, UUID ownerUserId);
}
