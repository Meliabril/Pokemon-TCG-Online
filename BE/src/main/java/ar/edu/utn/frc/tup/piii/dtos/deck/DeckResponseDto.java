package ar.edu.utn.frc.tup.piii.dtos.deck;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeckResponseDto(
        UUID id,
        UUID ownerUserId,
        String name,
        String format,
        boolean active,
        boolean valid,
        List<String> validationErrors,
        List<DeckCardResponseDto> cards,
        Instant createdAt,
        Instant updatedAt) {
}
