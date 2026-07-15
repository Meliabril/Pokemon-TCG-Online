package ar.edu.utn.frc.tup.piii.dtos.websocket;

import java.time.Instant;
import java.util.UUID;

public record MatchFoundDto(
        UUID opponentUserId,
        UUID gameId,
        Instant matchedAt) {
}
