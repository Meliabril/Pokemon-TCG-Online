package ar.edu.utn.frc.tup.piii.services.matchmaking.impl;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchFoundDto;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchFoundNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompMatchFoundNotifier implements MatchFoundNotifier {

    private static final String MATCHMAKING_DESTINATION = "/queue/matchmaking";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyMatchFound(UUID firstUserId, UUID secondUserId, UUID gameId, Instant matchedAt) {
        messagingTemplate.convertAndSendToUser(
                firstUserId.toString(),
                MATCHMAKING_DESTINATION,
                new MatchFoundDto(secondUserId, gameId, matchedAt));

        messagingTemplate.convertAndSendToUser(
                secondUserId.toString(),
                MATCHMAKING_DESTINATION,
                new MatchFoundDto(firstUserId, gameId, matchedAt));
    }
}
