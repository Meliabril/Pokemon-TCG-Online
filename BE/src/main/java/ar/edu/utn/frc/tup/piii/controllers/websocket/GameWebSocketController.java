package ar.edu.utn.frc.tup.piii.controllers.websocket;

import ar.edu.utn.frc.tup.piii.dtos.websocket.GameChatMessageRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.websocket.CardHoverChangedVisualEventDto;
import ar.edu.utn.frc.tup.piii.services.game.chat.GameChatService;
import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateSyncUseCase;
import ar.edu.utn.frc.tup.piii.services.game.visual.GameVisualEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameChatService gameChatService;
    private final GameStateSyncUseCase gameStateSyncUseCase;
    private final GamePresenceService gamePresenceService;
    private final GameVisualEventService gameVisualEventService;

    @MessageMapping("/games/{gameId}/chat")
    public void sendChatMessage(
            @DestinationVariable UUID gameId,
            GameChatMessageRequestDto request,
            Principal principal) {
        gameChatService.sendMessage(gameId, UUID.fromString(principal.getName()), request);
    }

    @MessageMapping("/games/{gameId}/state-sync")
    public void syncState(@DestinationVariable UUID gameId, Principal principal) {
        gameStateSyncUseCase.syncState(gameId, UUID.fromString(principal.getName()));
    }

    @MessageMapping("/games/{gameId}/visual-events")
    public void publishVisualEvent(
            @DestinationVariable UUID gameId,
            CardHoverChangedVisualEventDto event,
            Principal principal) {
        gameVisualEventService.publishVisualEvent(gameId, UUID.fromString(principal.getName()), event);
    }

    @MessageMapping("/games/{gameId}/leave")
    public void leaveGame(
            @DestinationVariable UUID gameId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        gamePresenceService.leaveGame(sessionId, gameId, UUID.fromString(principal.getName()));
    }
}
