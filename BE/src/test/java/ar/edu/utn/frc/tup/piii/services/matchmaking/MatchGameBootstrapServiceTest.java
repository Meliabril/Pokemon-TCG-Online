package ar.edu.utn.frc.tup.piii.services.matchmaking;




import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.services.deck.DeckPersistenceService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameMatchBootstrapService;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameMatchBootstrapServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchGameBootstrapServiceTest {

    private GameDataService gameDataService;
    private GameParticipantStateService gameParticipantStateService;
    private DeckPersistenceService deckPersistenceService;
    private DeckValidationService deckValidationService;
    private GameSnapshotService gameSnapshotService;
    private GameStateQueryService gameStateQueryService;
    private GameMatchBootstrapService gameMatchBootstrapService;

    @BeforeEach
    void setUp() {
        gameDataService = mock(GameDataService.class);
        gameParticipantStateService = mock(GameParticipantStateService.class);
        deckPersistenceService = mock(DeckPersistenceService.class);
        deckValidationService = mock(DeckValidationService.class);
        gameSnapshotService = mock(GameSnapshotService.class);
        gameStateQueryService = mock(GameStateQueryService.class);
        gameMatchBootstrapService = new GameMatchBootstrapServiceImpl(
                gameDataService,
                gameParticipantStateService,
                deckPersistenceService,
                deckValidationService,
                gameSnapshotService,
                gameStateQueryService);
        when(gameDataService.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameParticipantStateService.save(any(GameParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateGameAndTwoParticipantsAtomically() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Deck userDeck = validDeck(userDeckId, userId);
        Deck opponentDeck = validDeck(opponentDeckId, opponentUserId);

        when(deckPersistenceService.findByIdAndOwnerIdWithCards(userDeckId, userId)).thenReturn(Optional.of(userDeck));
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(opponentDeckId, opponentUserId)).thenReturn(Optional.of(opponentDeck));
        when(deckValidationService.validate(userDeck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(deckValidationService.validate(opponentDeck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(gameStateQueryService.buildVisibleState(any(Game.class))).thenReturn(waitingState(UUID.randomUUID()));

        UUID gameId = gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId);

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        ArgumentCaptor<GameParticipant> participantCaptor = ArgumentCaptor.forClass(GameParticipant.class);
        verify(gameDataService).save(gameCaptor.capture());
        verify(gameParticipantStateService, times(2)).save(participantCaptor.capture());
        assertThat(gameId).isEqualTo(gameCaptor.getValue().getId());
        assertThat(participantCaptor.getAllValues())
                .hasSize(2)
                .extracting(GameParticipant::getUserId, GameParticipant::getDeckId, GameParticipant::getPlayerOrder)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(userId, userDeckId, 1),
                        org.assertj.core.groups.Tuple.tuple(opponentUserId, opponentDeckId, 2));
        assertThat(participantCaptor.getAllValues())
                .allSatisfy(participant -> assertThat(participant.getGame().getId()).isEqualTo(gameId));
    }

    @Test
    void shouldPersistInitialWaitingSnapshotDuringBootstrap() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Deck userDeck = validDeck(userDeckId, userId);
        Deck opponentDeck = validDeck(opponentDeckId, opponentUserId);

        when(deckPersistenceService.findByIdAndOwnerIdWithCards(userDeckId, userId)).thenReturn(Optional.of(userDeck));
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(opponentDeckId, opponentUserId)).thenReturn(Optional.of(opponentDeck));
        when(deckValidationService.validate(userDeck)).thenReturn(new DeckValidationResult(true, List.of()));
        when(deckValidationService.validate(opponentDeck)).thenReturn(new DeckValidationResult(true, List.of()));
        GameStateDto visibleState = waitingState(UUID.randomUUID());
        when(gameStateQueryService.buildVisibleState(any(Game.class))).thenReturn(visibleState);

        UUID gameId = gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId);

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(gameStateQueryService).buildVisibleState(gameCaptor.capture());
        verify(gameSnapshotService).saveSnapshot(gameId, 0, visibleState, null);
        assertThat(gameCaptor.getValue().getId()).isEqualTo(gameId);
        assertThat(gameCaptor.getValue().getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(gameCaptor.getValue().getStateVersion()).isEqualTo(0);
    }

    @Test
    void shouldRejectMatchWhenDeckDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();

        when(deckPersistenceService.findByIdAndOwnerIdWithCards(userDeckId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("Deck not found");
    }

    @Test
    void shouldRejectMatchWhenDeckBecomesInvalid() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Deck userDeck = validDeck(userDeckId, userId);
        Deck opponentDeck = validDeck(opponentDeckId, opponentUserId);

        when(deckPersistenceService.findByIdAndOwnerIdWithCards(userDeckId, userId)).thenReturn(Optional.of(userDeck));
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(opponentDeckId, opponentUserId)).thenReturn(Optional.of(opponentDeck));
        when(deckValidationService.validate(userDeck)).thenReturn(new DeckValidationResult(false, List.of("too short")));

        assertThatThrownBy(() -> gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("not valid");

        verify(gameSnapshotService, never()).saveSnapshot(any(), anyInt(), any(), any());
    }

    @Test
    void shouldRejectMatchWhenDeckEntityIsMarkedInvalidEvenIfValidatorPasses() {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID userDeckId = UUID.randomUUID();
        UUID opponentDeckId = UUID.randomUUID();
        Deck userDeck = validDeck(userDeckId, userId);
        userDeck.setValid(false);
        Deck opponentDeck = validDeck(opponentDeckId, opponentUserId);

        when(deckPersistenceService.findByIdAndOwnerIdWithCards(userDeckId, userId)).thenReturn(Optional.of(userDeck));
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(opponentDeckId, opponentUserId)).thenReturn(Optional.of(opponentDeck));
        when(deckValidationService.validate(userDeck)).thenReturn(new DeckValidationResult(true, List.of()));

        assertThatThrownBy(() -> gameMatchBootstrapService.createMatch(userId, userDeckId, opponentUserId, opponentDeckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("not valid");

        verify(gameDataService, times(0)).save(any(Game.class));
        verify(gameParticipantStateService, times(0)).save(any(GameParticipant.class));
    }

    private Deck validDeck(UUID deckId, UUID ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Deck deck = new Deck();
        deck.setId(deckId);
        deck.setOwner(owner);
        deck.setName("Deck");
        deck.setValid(true);
        return deck;
    }

    private GameStateDto waitingState(UUID gameId) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.WAITING,
                null,
                0,
                0,
                null,
                List.of(),
                List.of(GameActionType.START_GAME),
                Instant.parse("2026-05-30T00:00:00Z"));
    }
}
