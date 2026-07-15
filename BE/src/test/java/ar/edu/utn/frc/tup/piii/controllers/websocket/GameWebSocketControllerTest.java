package ar.edu.utn.frc.tup.piii.controllers.websocket;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameChatContext;
import ar.edu.utn.frc.tup.piii.dtos.websocket.CardHoverChangedVisualEventDto;
import ar.edu.utn.frc.tup.piii.dtos.websocket.GameChatMessageRequestDto;
import ar.edu.utn.frc.tup.piii.security.StompPrincipal;
import ar.edu.utn.frc.tup.piii.services.game.chat.GameChatService;
import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateSyncUseCase;
import ar.edu.utn.frc.tup.piii.services.game.visual.GameVisualEventService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GameWebSocketControllerTest {

    @Test
    void shouldDelegateStateSyncToUseCaseUsingAuthenticatedPrincipal() {
        GameChatService gameChatService = mock(GameChatService.class);
        GameStateSyncUseCase gameStateSyncUseCase = mock(GameStateSyncUseCase.class);
        GamePresenceService gamePresenceService = mock(GamePresenceService.class);
        GameVisualEventService gameVisualEventService = mock(GameVisualEventService.class);
        GameWebSocketController controller = new GameWebSocketController(
                gameChatService,
                gameStateSyncUseCase,
                gamePresenceService,
                gameVisualEventService);
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        controller.syncState(gameId, new StompPrincipal(userId));

        verify(gameStateSyncUseCase).syncState(gameId, userId);
    }

    @Test
    void shouldDelegateChatMessagesToChatServiceUsingAuthenticatedPrincipal() {
        GameChatService gameChatService = mock(GameChatService.class);
        GameStateSyncUseCase gameStateSyncUseCase = mock(GameStateSyncUseCase.class);
        GamePresenceService gamePresenceService = mock(GamePresenceService.class);
        GameVisualEventService gameVisualEventService = mock(GameVisualEventService.class);
        GameWebSocketController controller = new GameWebSocketController(
                gameChatService,
                gameStateSyncUseCase,
                gamePresenceService,
                gameVisualEventService);
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GameChatMessageRequestDto request = new GameChatMessageRequestDto(GameChatContext.ROOM, "hola");

        controller.sendChatMessage(gameId, request, new StompPrincipal(userId));

        verify(gameChatService).sendMessage(gameId, userId, request);
    }

    @Test
    void shouldDelegateExplicitLeaveToPresenceServiceUsingAuthenticatedPrincipalAndSession() {
        GameChatService gameChatService = mock(GameChatService.class);
        GameStateSyncUseCase gameStateSyncUseCase = mock(GameStateSyncUseCase.class);
        GamePresenceService gamePresenceService = mock(GamePresenceService.class);
        GameVisualEventService gameVisualEventService = mock(GameVisualEventService.class);
        GameWebSocketController controller = new GameWebSocketController(
                gameChatService,
                gameStateSyncUseCase,
                gamePresenceService,
                gameVisualEventService);
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId("session-1");

        controller.leaveGame(gameId, new StompPrincipal(userId), headerAccessor);

        verify(gamePresenceService).leaveGame("session-1", gameId, userId);
    }

    @Test
    void shouldDelegateVisualEventsToVisualServiceUsingAuthenticatedPrincipal() {
        GameChatService gameChatService = mock(GameChatService.class);
        GameStateSyncUseCase gameStateSyncUseCase = mock(GameStateSyncUseCase.class);
        GamePresenceService gamePresenceService = mock(GamePresenceService.class);
        GameVisualEventService gameVisualEventService = mock(GameVisualEventService.class);
        GameWebSocketController controller = new GameWebSocketController(
                gameChatService,
                gameStateSyncUseCase,
                gamePresenceService,
                gameVisualEventService);
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CardHoverChangedVisualEventDto event = new CardHoverChangedVisualEventDto(
                "CARD_HOVER_CHANGED",
                gameId,
                userId,
                "HAND",
                "SELF",
                2,
                null,
                true,
                null);

        controller.publishVisualEvent(gameId, event, new StompPrincipal(userId));

        verify(gameVisualEventService).publishVisualEvent(gameId, userId, event);
    }
}
