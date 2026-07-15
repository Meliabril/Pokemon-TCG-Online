package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.util.List;
import java.util.UUID;

public record DeckValidationResponseDto(
        UUID deckId,
        boolean valid,
        List<String> errors) {
}
