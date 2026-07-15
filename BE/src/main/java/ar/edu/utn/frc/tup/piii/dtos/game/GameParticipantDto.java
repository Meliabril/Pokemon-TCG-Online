package ar.edu.utn.frc.tup.piii.dtos.game;

import java.time.Instant;
import java.util.UUID;

public record GameParticipantDto(
        UUID id,
        UUID userId,
        String username,
        String avatar,
        UUID deckId,
        Integer playerOrder,
        Boolean connected,
        Instant lastSeenAt,
        Instant createdAt) {
}
