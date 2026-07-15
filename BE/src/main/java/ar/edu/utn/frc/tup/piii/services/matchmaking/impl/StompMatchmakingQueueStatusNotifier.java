package ar.edu.utn.frc.tup.piii.services.matchmaking.impl;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingQueueStatusNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompMatchmakingQueueStatusNotifier implements MatchmakingQueueStatusNotifier {

    private static final String MATCHMAKING_DESTINATION = "/queue/matchmaking";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyQueueStatus(UUID userId, MatchmakingQueueStatusDto status) {
        messagingTemplate.convertAndSendToUser(userId.toString(), MATCHMAKING_DESTINATION, status);
    }
}
