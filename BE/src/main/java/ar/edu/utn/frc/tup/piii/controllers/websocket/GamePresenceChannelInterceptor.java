package ar.edu.utn.frc.tup.piii.controllers.websocket;

import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class GamePresenceChannelInterceptor implements ChannelInterceptor {

    private static final Pattern GAME_DESTINATION_PATTERN = Pattern.compile("^/(?:topic/games|user/queue/games)/([0-9a-fA-F-]+)$");

    private final GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry;
    private final GamePresenceService gamePresenceService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        UUID gameId = extractGameId(accessor.getDestination());
        Principal user = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (gameId == null || user == null || sessionId == null) {
            return message;
        }

        UUID userId = UUID.fromString(user.getName());
        if (gameWebSocketPresenceRegistry.register(sessionId, gameId, userId)) {
            gamePresenceService.markConnected(gameId, userId);
        }

        return message;
    }

    private UUID extractGameId(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }

        Matcher matcher = GAME_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }

        return UUID.fromString(matcher.group(1));
    }
}
