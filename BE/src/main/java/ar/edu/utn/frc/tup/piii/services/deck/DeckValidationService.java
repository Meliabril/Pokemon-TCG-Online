package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.entities.Deck;

public interface DeckValidationService {

    DeckValidationResult validate(Deck deck);

    DeckValidationResult validate(DeckValidationSummary summary);
}
