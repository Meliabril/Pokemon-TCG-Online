package ar.edu.utn.frc.tup.piii.services.game.presence;

import java.time.Instant;
import java.util.UUID;

public record GameParticipantPresenceChangedEvent(
        UUID gameId,
        int stateVersion,
        UUID userId,
        boolean connected,
        Instant lastSeenAt) {
}
