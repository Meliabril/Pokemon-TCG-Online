package ar.edu.utn.frc.tup.piii.services.game.chat.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameChatContext;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameChatServiceImplTest {

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameEventService gameEventService;

    @Mock
    private GameRealtimeEventService gameRealtimeEventService;

    @Mock
    private UserService userService;

    @Test
    void shouldDispatchPublicChatMessageUsingCurrentGameVersion() {
        GameChatService service = new GameChatServiceImpl(
                gameLookupService,
                gameEventService,
                gameRealtimeEventService,
                userService);
        UUID gameId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setStateVersion(9);
        User user = new User();
        user.setId(senderUserId);
        user.setUsername("misty");
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(userService.getUserEntityById(senderUserId)).thenReturn(user);

        service.sendMessage(gameId, senderUserId, new GameChatMessageRequestDto(GameChatContext.ROOM, "  Hola Brock  "));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameRealtimeEventService).dispatchPublic(
                eq(gameId),
                eq(GameEventType.CHAT_MESSAGE),
                eq(9),
                payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("senderUserId", senderUserId)
                .containsEntry("senderUsername", "misty")
                .containsEntry("context", "ROOM")
                .containsEntry("content", "Hola Brock")
                .containsKeys("messageId", "sentAt");
    }

    @Test
    void shouldReturnOnlyPersistedChatMessagesVisibleToViewer() {
        GameChatService service = new GameChatServiceImpl(
                gameLookupService,
                gameEventService,
                gameRealtimeEventService,
                userService);
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();
        Instant sentAt = Instant.parse("2026-05-30T04:30:00Z");
        when(gameEventService.getVisibleEvents(gameId, viewerUserId)).thenReturn(List.of(
                new GameEventDto(
                        UUID.randomUUID(),
                        gameId,
                        GameEventType.GAME_STARTED,
                        3,
                        false,
                        sentAt.minusSeconds(10),
                        Map.of("phase", "DRAW")),
                new GameEventDto(
                        UUID.randomUUID(),
                        gameId,
                        GameEventType.CHAT_MESSAGE,
                        3,
                        false,
                        sentAt,
                        Map.of(
                                "messageId", UUID.randomUUID().toString(),
                                "senderUserId", senderUserId.toString(),
                                "senderUsername", "gary",
                                "context", "GAME",
                                "content", "te toca",
                                "sentAt", (double) sentAt.getEpochSecond()))));

        List<GameChatMessageDto> history = service.getVisibleChatHistory(gameId, viewerUserId);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().senderUsername()).isEqualTo("gary");
        assertThat(history.getFirst().context()).isEqualTo(GameChatContext.GAME);
        assertThat(history.getFirst().content()).isEqualTo("te toca");
    }

    @Test
    void shouldRejectBlankOrOversizedChatMessages() {
        GameChatService service = new GameChatServiceImpl(
                gameLookupService,
                gameEventService,
                gameRealtimeEventService,
                userService);
        UUID gameId = UUID.randomUUID();
        UUID senderUserId = UUID.randomUUID();

        assertThatThrownBy(() -> service.sendMessage(
                gameId,
                senderUserId,
                new GameChatMessageRequestDto(GameChatContext.GAME, "   ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("content is required");

        assertThatThrownBy(() -> service.sendMessage(
                gameId,
                senderUserId,
                new GameChatMessageRequestDto(GameChatContext.GAME, "a".repeat(501))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at most 500 characters");
    }
}
