package ar.edu.utn.frc.tup.piii.dtos.websocket;

import java.time.Instant;
import java.util.UUID;

public record MatchmakingQueueStatusDto(
        boolean queued,
        Instant queuedAt,
        Integer queueSize,
        UUID matchedUserId,
        UUID gameId) {
}
