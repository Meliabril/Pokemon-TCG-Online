package ar.edu.utn.frc.tup.piii.services.deck;

import java.util.List;

public record DeckValidationSummary(
        long totalCards,
        boolean containsCardsOutsideSet,
        boolean containsBasicPokemon,
        List<String> duplicatedNames) {
}
