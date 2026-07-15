package ar.edu.utn.frc.tup.piii.services.game.chat.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameChatContext;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameChatMessageDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.websocket.GameChatMessageRequestDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.BadRequestException;
import ar.edu.utn.frc.tup.piii.services.game.chat.GameChatService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameChatServiceImpl implements GameChatService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final GameLookupService gameLookupService;
    private final GameEventService gameEventService;
    private final GameRealtimeEventService gameRealtimeEventService;
    private final UserService userService;

    @Override
    @Transactional
    public void sendMessage(UUID gameId, UUID senderUserId, GameChatMessageRequestDto request) {
        gameLookupService.assertParticipant(gameId, senderUserId);
        validateRequest(request);

        Game game = gameLookupService.getRequiredGame(gameId);
        User sender = userService.getUserEntityById(senderUserId);
        GameChatMessageDto message = new GameChatMessageDto(
                UUID.randomUUID(),
                gameId,
                senderUserId,
                sender.getUsername(),
                request.context(),
                request.content().trim(),
                Instant.now());

        gameRealtimeEventService.dispatchPublic(
                gameId,
                GameEventType.CHAT_MESSAGE,
                game.getStateVersion(),
                toPayload(message));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameChatMessageDto> getVisibleChatHistory(UUID gameId, UUID viewerUserId) {
        gameLookupService.assertParticipant(gameId, viewerUserId);

        List<GameChatMessageDto> messages = new ArrayList<>();
        List<GameEventDto> visibleEvents = gameEventService.getVisibleEvents(gameId, viewerUserId);
        for (GameEventDto event : visibleEvents) {
            if (event.eventType() == GameEventType.CHAT_MESSAGE) {
                messages.add(toChatMessage(event));
            }
        }

        return List.copyOf(messages);
    }

    private void validateRequest(GameChatMessageRequestDto request) {
        if (request == null) {
            throw new BadRequestException("Chat message payload is required");
        }
        if (request.context() == null) {
            throw new BadRequestException("Chat message context is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BadRequestException("Chat message content is required");
        }
        if (request.content().trim().length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException("Chat message content must have at most 500 characters");
        }
    }

    private Map<String, Object> toPayload(GameChatMessageDto message) {
        return Map.of(
                "messageId", message.messageId(),
                "senderUserId", message.senderUserId(),
                "senderUsername", message.senderUsername(),
                "context", message.context().name(),
                "content", message.content(),
                "sentAt", message.sentAt());
    }

    private GameChatMessageDto toChatMessage(GameEventDto event) {
        Map<String, Object> payload = event.payload();
        return new GameChatMessageDto(
                UUID.fromString(String.valueOf(payload.get("messageId"))),
                event.gameId(),
                UUID.fromString(String.valueOf(payload.get("senderUserId"))),
                String.valueOf(payload.get("senderUsername")),
                GameChatContext.valueOf(String.valueOf(payload.get("context"))),
                String.valueOf(payload.get("content")),
                parseInstant(payload.get("sentAt")));
    }

    private Instant parseInstant(Object rawSentAt) {
        if (rawSentAt instanceof Instant instant) {
            return instant;
        }
        if (rawSentAt instanceof Number number) {
            BigDecimal epochValue = BigDecimal.valueOf(number.doubleValue());
            if (epochValue.abs().compareTo(BigDecimal.valueOf(1_000_000_000_000L)) >= 0) {
                return Instant.ofEpochMilli(epochValue.longValue());
            }

            long epochSeconds = epochValue.longValue();
            int nanos = epochValue.subtract(BigDecimal.valueOf(epochSeconds))
                    .movePointRight(9)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            return Instant.ofEpochSecond(epochSeconds, nanos);
        }

        String sentAt = String.valueOf(rawSentAt);
        if (sentAt.matches("^-?\\d+(\\.\\d+)?$")) {
            return parseInstant(Double.parseDouble(sentAt));
        }

        return Instant.parse(sentAt);
    }
}
