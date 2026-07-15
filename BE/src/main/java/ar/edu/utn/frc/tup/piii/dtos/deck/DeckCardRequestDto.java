package ar.edu.utn.frc.tup.piii.dtos.deck;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeckCardRequestDto(
        @NotNull(message = "Card id is required")
        UUID cardId,

        @Min(value = 1, message = "Quantity must be greater than zero")
        int quantity) {
}
