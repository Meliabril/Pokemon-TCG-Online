package ar.edu.utn.frc.tup.piii.services.websocket;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateSyncDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketGameEventPublisher implements GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishPublic(GameEventDto event) {
        messagingTemplate.convertAndSend(publicDestination(event.gameId()), event);
    }

    @Override
    public void publishPrivate(GameEventDto event, UUID recipientUserId) {
        messagingTemplate.convertAndSendToUser(
                recipientUserId.toString(),
                privateDestination(event.gameId()),
                event);
    }

    @Override
    public void publishPrivateStateSync(GameStateSyncDto event, UUID recipientUserId) {
        messagingTemplate.convertAndSendToUser(
                recipientUserId.toString(),
                privateDestination(event.gameId()),
                event);
    }

    private String publicDestination(UUID gameId) {
        return "/topic/games/" + gameId;
    }

    private String privateDestination(UUID gameId) {
        return "/queue/games/" + gameId;
    }
}
