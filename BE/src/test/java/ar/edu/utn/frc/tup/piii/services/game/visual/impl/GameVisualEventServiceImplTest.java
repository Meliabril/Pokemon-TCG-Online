package ar.edu.utn.frc.tup.piii.services.game.visual.impl;

import ar.edu.utn.frc.tup.piii.dtos.websocket.CardHoverChangedVisualEventDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GameVisualEventServiceImplTest {

    @Test
    void shouldValidateParticipantAndPublishVisualEventWithoutPersistence() {
        GameLookupService gameLookupService = mock(GameLookupService.class);
        GameDataService gameDataService = mock(GameDataService.class);
        GameStateQueryService gameStateQueryService = mock(GameStateQueryService.class);
        GameSnapshotService gameSnapshotService = mock(GameSnapshotService.class);
        GameCardInstanceStateService cardInstanceStateService = mock(GameCardInstanceStateService.class);
        CardService cardService = mock(CardService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GameVisualEventServiceImpl service = new GameVisualEventServiceImpl(
                gameLookupService,
                gameDataService,
                gameStateQueryService,
                gameSnapshotService,
                cardInstanceStateService,
                cardService,
                messagingTemplate);
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        CardHoverChangedVisualEventDto request = new CardHoverChangedVisualEventDto(
                "CARD_HOVER_CHANGED",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "HAND",
                "SELF",
                2,
                null,
                true,
                null);

        service.publishVisualEvent(gameId, playerId, request);

        verify(gameLookupService).assertParticipant(gameId, playerId);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/games/" + gameId + "/visual-events"),
                eq(new CardHoverChangedVisualEventDto(
                        "CARD_HOVER_CHANGED",
                        gameId,
                        playerId,
                        "HAND",
                        "SELF",
                        2,
                        null,
                        true,
                        null)));
    }

    @Test
    void shouldAllowDiscardVisualEvents() {
        GameLookupService gameLookupService = mock(GameLookupService.class);
        GameDataService gameDataService = mock(GameDataService.class);
        GameStateQueryService gameStateQueryService = mock(GameStateQueryService.class);
        GameSnapshotService gameSnapshotService = mock(GameSnapshotService.class);
        GameCardInstanceStateService cardInstanceStateService = mock(GameCardInstanceStateService.class);
        CardService cardService = mock(CardService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GameVisualEventServiceImpl service = new GameVisualEventServiceImpl(
                gameLookupService,
                gameDataService,
                gameStateQueryService,
                gameSnapshotService,
                cardInstanceStateService,
                cardService,
                messagingTemplate);
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        CardHoverChangedVisualEventDto request = new CardHoverChangedVisualEventDto(
                "CARD_HOVER_CHANGED",
                gameId,
                playerId,
                "DISCARD",
                "OPPONENT",
                0,
                null,
                true,
                null);

        service.publishVisualEvent(gameId, playerId, request);

        verify(gameLookupService).assertParticipant(gameId, playerId);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/games/" + gameId + "/visual-events"),
                eq(new CardHoverChangedVisualEventDto(
                        "CARD_HOVER_CHANGED",
                        gameId,
                        playerId,
                        "DISCARD",
                        "OPPONENT",
                        0,
                        null,
                        true,
                        null)));
    }

    @Test
    void shouldPersistAndPublishSetupOccupancyWithoutCardIdentity() {
        GameLookupService gameLookupService = mock(GameLookupService.class);
        GameDataService gameDataService = mock(GameDataService.class);
        GameStateQueryService gameStateQueryService = mock(GameStateQueryService.class);
        GameSnapshotService gameSnapshotService = mock(GameSnapshotService.class);
        GameCardInstanceStateService cardInstanceStateService = mock(GameCardInstanceStateService.class);
        CardService cardService = mock(CardService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        GameVisualEventServiceImpl service = new GameVisualEventServiceImpl(
                gameLookupService,
                gameDataService,
                gameStateQueryService,
                gameSnapshotService,
                cardInstanceStateService,
                cardService,
                messagingTemplate);
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.SETUP);
        game.setSetupState(Map.of());
        when(gameDataService.getRequiredGameForUpdate(gameId)).thenReturn(game);
        when(gameDataService.save(game)).thenReturn(game);
        ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto updatedState = mock(
                ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto.class);
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(updatedState);
        UUID cardInstanceId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(cardInstanceId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(CardZone.HAND);
        Card card = new Card();
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic");
        when(cardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardInstanceId, gameId, playerId))
                .thenReturn(java.util.Optional.of(cardInstance));
        when(cardService.getCardEntityById(cardId)).thenReturn(card);
        CardHoverChangedVisualEventDto request = new CardHoverChangedVisualEventDto(
                "SETUP_SLOT_CHANGED",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BENCH",
                "SELF",
                0,
                cardInstanceId,
                null,
                true);

        service.publishVisualEvent(gameId, playerId, request);

        verify(gameLookupService).assertParticipant(gameId, playerId);
        verify(gameDataService).save(game);
        verify(gameSnapshotService).updateLatestSnapshot(gameId, updatedState);
        assertEquals(
                Map.of(playerId.toString(), java.util.List.of(0)),
                game.getSetupState().get("setupBenchOccupiedIndexesByPlayer"));
        assertEquals(
                Map.of(playerId.toString(), java.util.List.of(cardInstanceId.toString())),
                game.getSetupState().get("setupBenchCardInstanceIdsByPlayer"));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/games/" + gameId + "/visual-events"),
                eq(new CardHoverChangedVisualEventDto(
                        "SETUP_SLOT_CHANGED",
                        gameId,
                        playerId,
                        "BENCH",
                        "SELF",
                        0,
                        null,
                        null,
                        true)));
    }
}
