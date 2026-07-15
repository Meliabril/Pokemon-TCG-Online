package ar.edu.utn.frc.tup.piii.services.matchmaking.impl;

import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchFoundApplicationEvent;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchFoundEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApplicationEventMatchFoundEventPublisher implements MatchFoundEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(UUID firstUserId, UUID secondUserId, UUID gameId, Instant matchedAt) {
        applicationEventPublisher.publishEvent(new MatchFoundApplicationEvent(
                firstUserId,
                secondUserId,
                gameId,
                matchedAt));
    }
}
