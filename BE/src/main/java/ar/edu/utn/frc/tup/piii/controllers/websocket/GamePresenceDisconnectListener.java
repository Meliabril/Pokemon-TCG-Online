package ar.edu.utn.frc.tup.piii.controllers.websocket;

import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class GamePresenceDisconnectListener {

    private final GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry;
    private final GamePresenceService gamePresenceService;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        for (GameWebSocketPresenceRegistry.GamePresenceKey key : gameWebSocketPresenceRegistry.unregisterSession(event.getSessionId())) {
            gamePresenceService.markDisconnected(key.gameId(), key.userId());
        }
    }
}
