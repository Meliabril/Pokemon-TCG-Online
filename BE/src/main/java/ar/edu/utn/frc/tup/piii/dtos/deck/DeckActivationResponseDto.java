package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.util.List;
import java.util.UUID;

public record DeckActivationResponseDto(
        UUID id,
        boolean active,
        boolean valid,
        List<String> validationErrors) {
}
