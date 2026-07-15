package ar.edu.utn.frc.tup.piii.services.matchmaking;

import java.time.Instant;
import java.util.UUID;

public interface MatchFoundEventPublisher {

    void publish(UUID firstUserId, UUID secondUserId, UUID gameId, Instant matchedAt);
}
