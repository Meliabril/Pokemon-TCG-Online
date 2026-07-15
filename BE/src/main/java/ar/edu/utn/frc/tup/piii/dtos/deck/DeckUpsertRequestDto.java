package ar.edu.utn.frc.tup.piii.dtos.deck;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeckUpsertRequestDto(
        @NotBlank(message = "Deck name is required")
        @Size(max = 100, message = "Deck name must have at most 100 characters")
        String name,

        @NotNull(message = "Deck cards are required")
        @Valid
        List<DeckCardRequestDto> cards) {
}
