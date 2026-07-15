package ar.edu.utn.frc.tup.piii.services.deck;

import java.util.List;

public record DeckValidationResult(
        boolean valid,
        List<String> errors) {
}
