package ar.edu.utn.frc.tup.piii.services.game.setup.impl;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.MulliganRevealedCardDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameDeckStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupServiceImplTest {

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @Mock
    private GameDeckStateService gameDeckStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private CardService cardService;

    private SetupServiceImpl setupService;

    @BeforeEach
    void setUp() {
        setupService = new SetupServiceImpl(
                gameLookupService,
                gameParticipantStateService,
                gameDeckStateService,
                gameCardInstanceStateService,
                pokemonInPlayStateService,
                pokemonEvolutionStackStateService,
                gameRandomService,
                gameEventFactory,
                cardService);
    }

    @Test
    void shouldInitializeSetupWithoutAutomaticBoard() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Game game = waitingGame(gameId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, deckOneId, deckTwoId);
        Deck deckOne = deck(deckOneId, basicCard, 60);
        Deck deckTwo = deck(deckTwoId, basicCard, 60);

        configureStartGameBase(game, participants, deckOne, deckTwo);
        when(gameRandomService.<Card>shuffledCopy(Mockito.anyList()))
                .thenReturn(expandedCards(basicCard, 60), expandedCards(basicCard, 60));

        GameActionExecutionResult result = setupService.startGame(context(gameId, playerOneId, GameActionType.START_GAME, 0, Map.of()));

        assertThat(result.gameState().status()).isEqualTo(GameStatus.SETUP);
        assertThat(result.gameState().actions().availableActions()).containsExactly(GameActionType.CHOOSE_INITIAL_POKEMON);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerOneId, 0);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerTwoId, 0);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerOneId, false);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerTwoId, false);
        // With no Mulligan, nobody is a current Mulligan player, so no approval button is ever shown.
        assertThat(GameStateTestFactory.mulliganCurrentPlayerByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, false);
        assertThat(result.gameState().actions().availableActions())
                .contains(GameActionType.CHOOSE_INITIAL_POKEMON);
        assertThat(game.getStatus()).isEqualTo(GameStatus.SETUP);
        verify(pokemonInPlayStateService, never()).save(any());

        assertThat(eventTypes(result.emittedEvents())).contains(GameEventType.OPENING_HANDS_DEALT);
        ArgumentCaptor<Map<String, Object>> openingPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory, times(1)).publicEvent(
                eq(gameId),
                eq(GameEventType.OPENING_HANDS_DEALT),
                anyInt(),
                openingPayloadCaptor.capture());
        assertOpeningHandsDealtPayload(openingPayloadCaptor.getValue(), playerOneId, playerTwoId);
    }

    @Test
    void shouldRevealMulliganAndWaitForInteractiveAcknowledgements() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Card energyCard = card(UUID.randomUUID(), CardCategory.BASIC_ENERGY);
        Game game = waitingGame(gameId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, deckOneId, deckTwoId);
        Deck deckOne = mixedDeck(deckOneId, basicCard, energyCard);
        Deck deckTwo = mixedDeck(deckTwoId, basicCard, energyCard);

        configureStartGameBase(game, participants, deckOne, deckTwo);
        when(gameRandomService.<Card>shuffledCopy(Mockito.anyList()))
                .thenReturn(
                        shuffledWithOpeningCards(energyCard, basicCard),
                        shuffledWithOpeningCards(basicCard, energyCard));

        GameActionExecutionResult result = setupService.startGame(context(gameId, playerOneId, GameActionType.START_GAME, 0, Map.of()));

        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerOneId, 1);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerTwoId, 0);
        // Synchronized barrier: with any Mulligan required, BOTH players hold a pending acknowledgement.
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerOneId, true);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerTwoId, true);
        // mulliganCurrentPlayer distinguishes the button variant: only the Mulligan player is "current"
        // (shows "Iniciar Mulligan"); the observer is not (shows "Entendido. Continuar").
        assertThat(GameStateTestFactory.mulliganCurrentPlayerByPlayer(result.gameState()))
                .containsEntry(playerOneId, true)
                .containsEntry(playerTwoId, false);
        assertThat(result.gameState().actions().availableActions())
                .containsExactly(GameActionType.ACK_MULLIGAN_NOTICE, GameActionType.CHOOSE_INITIAL_POKEMON);
        assertThat(eventTypes(result.emittedEvents()))
                .contains(GameEventType.MULLIGAN_REQUIRED)
                .doesNotContain(
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                        GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED);

        // The invalid hand must NOT be revealed to the opponent at START_GAME; it is only revealed
        // after the owner acknowledges the Mulligan notice (see advanceMulliganFlow).
        verify(gameEventFactory, never()).privateEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_HAND_REVEALED),
                anyInt(),
                anyMap(),
                any(UUID.class));

        List<Iterable<GameCardInstance>> savedBatches = savedCardInstanceBatches();
        assertThat(countZone(savedBatches, playerOneId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerOneId, CardZone.DECK)).isEqualTo(53);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.DECK)).isEqualTo(53);
        assertThat(countZone(savedBatches, playerOneId, CardZone.PRIZE)).isZero();
        assertThat(countZone(savedBatches, playerTwoId, CardZone.PRIZE)).isZero();
    }

    @Test
    void shouldAccumulateMulligansIndependentlyWhenBothPlayersRevealInvalidHands() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Card energyCard = card(UUID.randomUUID(), CardCategory.BASIC_ENERGY);
        Game game = waitingGame(gameId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, deckOneId, deckTwoId);
        Deck deckOne = mixedDeck(deckOneId, basicCard, energyCard);
        Deck deckTwo = mixedDeck(deckTwoId, basicCard, energyCard);

        configureStartGameBase(game, participants, deckOne, deckTwo);
        when(gameRandomService.<Card>shuffledCopy(Mockito.anyList()))
                .thenReturn(
                        shuffledWithOpeningCards(energyCard, basicCard),
                        shuffledWithOpeningCards(energyCard, basicCard));

        GameActionExecutionResult result = setupService.startGame(context(gameId, playerOneId, GameActionType.START_GAME, 0, Map.of()));

        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerOneId, 1);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerTwoId, 1);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerOneId, true);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerTwoId, true);
        // Both players must Mulligan, so both are current players (each owns one initial approval).
        assertThat(GameStateTestFactory.mulliganCurrentPlayerByPlayer(result.gameState()))
                .containsEntry(playerOneId, true)
                .containsEntry(playerTwoId, true);
        assertThat(eventTypes(result.emittedEvents()))
                .contains(GameEventType.MULLIGAN_REQUIRED)
                .doesNotContain(
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                        GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED);

        // Neither hand is revealed at START_GAME; each reveal is gated behind its owner's acknowledgement.
        verify(gameEventFactory, never()).privateEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_HAND_REVEALED),
                anyInt(),
                anyMap(),
                any(UUID.class));
        List<Iterable<GameCardInstance>> savedBatches = savedCardInstanceBatches();
        assertThat(countZone(savedBatches, playerOneId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerOneId, CardZone.DECK)).isEqualTo(53);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.DECK)).isEqualTo(53);
    }

    @Test
    void shouldNotAdvanceMultipleMulligansUntilPlayersAcknowledgeCurrentAttempt() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Card energyCard = card(UUID.randomUUID(), CardCategory.BASIC_ENERGY);
        Game game = waitingGame(gameId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, deckOneId, deckTwoId);
        Deck deckOne = mixedDeck(deckOneId, basicCard, energyCard);
        Deck deckTwo = mixedDeck(deckTwoId, basicCard, energyCard);

        configureStartGameBase(game, participants, deckOne, deckTwo);
        when(gameRandomService.<Card>shuffledCopy(Mockito.anyList()))
                .thenReturn(
                        shuffledWithOpeningCards(energyCard, basicCard),
                        shuffledWithOpeningCards(basicCard, energyCard));

        GameActionExecutionResult result = setupService.startGame(context(gameId, playerOneId, GameActionType.START_GAME, 0, Map.of()));

        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerOneId, 1);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState())).containsEntry(playerTwoId, 0);
        // Synchronized barrier: both players hold a pending acknowledgement until both confirm.
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerOneId, true);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState())).containsEntry(playerTwoId, true);
        // Only the Mulligan player is the current one (button variant), the observer only confirms readiness.
        assertThat(GameStateTestFactory.mulliganCurrentPlayerByPlayer(result.gameState()))
                .containsEntry(playerOneId, true)
                .containsEntry(playerTwoId, false);
        List<GameEventType> emittedTypes = eventTypes(result.emittedEvents());
        assertThat(emittedTypes)
                .contains(GameEventType.OPENING_HANDS_DEALT, GameEventType.MULLIGAN_REQUIRED)
                .doesNotContain(
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_SEQUENCE_COMPLETED);
        // Opening deal is announced before any Mulligan event.
        assertThat(emittedTypes.indexOf(GameEventType.OPENING_HANDS_DEALT))
                .isGreaterThanOrEqualTo(0)
                .isLessThan(emittedTypes.indexOf(GameEventType.MULLIGAN_REQUIRED));

        // The hand is not revealed to the opponent until the owner acknowledges the Mulligan notice.
        verify(gameEventFactory, never()).privateEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_HAND_REVEALED),
                anyInt(),
                anyMap(),
                any(UUID.class));

        List<Iterable<GameCardInstance>> savedBatches = savedCardInstanceBatches();
        assertThat(countZone(savedBatches, playerOneId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.HAND)).isEqualTo(7);
        assertThat(countZone(savedBatches, playerOneId, CardZone.DECK)).isEqualTo(53);
        assertThat(countZone(savedBatches, playerTwoId, CardZone.DECK)).isEqualTo(53);
    }

    @Test
    void shouldAcknowledgePendingMulliganNoticeForActorOnly() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId, true);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());
        GameEventDto acknowledgedEvent = event(gameId, GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED);

        configureAckGame(game, participants);
        when(gameEventFactory.publicEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED),
                eq(2),
                anyMap())).thenReturn(acknowledgedEvent);

        GameActionExecutionResult result = setupService.ackMulliganNotice(
                context(gameId, playerOneId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));

        assertThat(result.emittedEvents()).contains(acknowledgedEvent);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, true);
        assertThat(result.gameState().actions().availableActions())
                .containsExactly(GameActionType.ACK_MULLIGAN_NOTICE, GameActionType.CHOOSE_INITIAL_POKEMON);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED),
                eq(2),
                payloadCaptor.capture());
        assertMulliganNoticeAcknowledgedPayload(payloadCaptor.getValue(), playerOneId);
    }

    @Test
    void shouldRejectDuplicateMulliganNoticeAcknowledgementWithoutDuplicatingEvent() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId, false);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                setupService.ackMulliganNotice(
                        context(gameId, playerOneId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Mulligan notice has already been acknowledged by this player");

        verify(gameEventFactory, never()).publicEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED),
                anyInt(),
                anyMap());
    }

    @Test
    void shouldAdvanceMulliganFlowAfterAllAcknowledgementsAndGrantExtrasAtTheEnd() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Card energyCard = card(UUID.randomUUID(), CardCategory.BASIC_ENERGY);
        Game game = setupGameWithActiveMulliganFlow(gameId, playerOneId, playerTwoId, playerOneId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());
        List<GameCardInstance> playerOneHand = cardInstances(game, playerOneId, energyCard, CardZone.HAND, 7);
        List<GameCardInstance> playerOneDeck = cardInstances(game, playerOneId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerOneShuffledDeck = combinedDeck(playerOneDeck, playerOneHand);
        List<GameCardInstance> playerOneDeckAfterDraw = cardInstances(game, playerOneId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerTwoHand = cardInstances(game, playerTwoId, basicCard, CardZone.HAND, 7);
        List<GameCardInstance> playerTwoDeck = cardInstances(game, playerTwoId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerTwoDeckAfterExtra = cardInstances(game, playerTwoId, basicCard, CardZone.DECK, 52);

        configureAckGame(game, participants);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerOneId, CardZone.HAND))
                .thenReturn(playerOneHand);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerOneId, CardZone.DECK))
                .thenReturn(playerOneDeck, playerOneShuffledDeck, playerOneDeckAfterDraw);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerTwoId, CardZone.HAND))
                .thenReturn(playerTwoHand);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerTwoId, CardZone.DECK))
                .thenReturn(playerTwoDeck, playerTwoDeckAfterExtra);
        // shuffledCopy is used both for the deck shuffle (GameCardInstance list) and for the random
        // Mulligan turn order (UUID list). Answer by element type: keep the turn order deterministic
        // (= input order) and return the prepared shuffled deck for card lists.
        when(gameRandomService.shuffledCopy(Mockito.anyList())).thenAnswer(invocation -> {
            List<?> source = invocation.getArgument(0);
            if (!source.isEmpty() && source.get(0) instanceof UUID) {
                return new ArrayList<>(source);
            }
            return playerOneShuffledDeck;
        });
        when(cardService.getCardEntityById(basicCard.getId())).thenReturn(basicCard);
        // The invalid hand (7 energy cards) is read and revealed to the opponent during the advance.
        when(cardService.getCardEntityById(energyCard.getId())).thenReturn(energyCard);

        // Barrier: the observer confirms first. The automatic sequence must NOT start yet (the Mulligan
        // player is still pending), so no reveal/return happens on this first acknowledgement.
        GameActionExecutionResult afterFirstAck = setupService.ackMulliganNotice(
                context(gameId, playerTwoId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(afterFirstAck.gameState()))
                .containsEntry(playerOneId, true)
                .containsEntry(playerTwoId, false);
        assertThat(eventTypes(afterFirstAck.emittedEvents()))
                .contains(GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED)
                .doesNotContain(GameEventType.MULLIGAN_HAND_REVEALED, GameEventType.MULLIGAN_FLOW_COMPLETED);

        // The Mulligan player confirms last → barrier passed → the whole sequence resolves automatically
        // and, being the only Mulligan player, completes the flow.
        GameActionExecutionResult result = setupService.ackMulliganNotice(
                context(gameId, playerOneId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));

        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, false);
        assertThat(GameStateTestFactory.mulliganCountByPlayer(result.gameState()))
                .containsEntry(playerOneId, 1)
                .containsEntry(playerTwoId, 0);
        // Once the flow completes, nobody remains a current Mulligan player.
        assertThat(GameStateTestFactory.mulliganCurrentPlayerByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, false);
        assertThat(eventTypes(result.emittedEvents()))
                .contains(
                        GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED,
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_HAND_RETURNED,
                        GameEventType.MULLIGAN_DECK_SHUFFLED,
                        GameEventType.MULLIGAN_NEW_HAND_DRAWN,
                        GameEventType.MULLIGAN_HAND_VALIDATED,
                        GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                        GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED,
                        GameEventType.MULLIGAN_FLOW_COMPLETED);
        assertThat(game.getSetupState().get("mulliganFlow").toString()).contains("readyForInitialSelection=true");

        // The reveal is gated behind the barrier and now PUBLIC: both players receive the invalid hand so
        // the Mulligan player also sees their own "show invalid hand + ¡MULLIGAN!" beat. It still exposes
        // only the invalid hand (never the new hand, deck or prizes).
        ArgumentCaptor<Map<String, Object>> revealPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_HAND_REVEALED),
                anyInt(),
                revealPayloadCaptor.capture());
        assertMulliganPayload(revealPayloadCaptor.getValue(), playerOneId, 1, energyCard);

        // The new hand is delivered PRIVATELY to its owner with the drawn cards (so the client can show the
        // real hand after each draw). It carries the owner's own cards only — never deck order or prizes.
        ArgumentCaptor<Map<String, Object>> newHandPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).privateEvent(
                eq(gameId),
                eq(GameEventType.MULLIGAN_NEW_HAND_DRAWN),
                anyInt(),
                newHandPayloadCaptor.capture(),
                eq(playerOneId));
        Map<String, Object> newHandPayload = newHandPayloadCaptor.getValue();
        assertThat(newHandPayload).containsEntry("playerId", playerOneId.toString());
        assertThat(newHandPayload.get("drawnCards")).isInstanceOf(List.class);
        assertThat((List<?>) newHandPayload.get("drawnCards")).isNotEmpty();
        assertThat(newHandPayload).doesNotContainKeys("deck", "deckOrder", "prizeCards", "prizes", "opponentHand");
    }

    @Test
    void shouldNotStartMulliganSequenceWhenOnlyOnePlayerConfirmsTheBarrier() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Game game = setupGameWithActiveMulliganFlow(gameId, playerOneId, playerTwoId, playerOneId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        configureAckGame(game, participants);

        // Only the Mulligan player confirms; the observer is still pending. The barrier is NOT satisfied,
        // so the automatic sequence must not start (no reveal, no shuffle, no flow completion) and the
        // flow stays active waiting for the other player.
        GameActionExecutionResult result = setupService.ackMulliganNotice(
                context(gameId, playerOneId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));

        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, true);
        assertThat(eventTypes(result.emittedEvents()))
                .contains(GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED)
                .doesNotContain(
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_HAND_RETURNED,
                        GameEventType.MULLIGAN_DECK_SHUFFLED,
                        GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                        GameEventType.MULLIGAN_FLOW_COMPLETED);
        assertThat(game.getSetupState().get("mulliganFlow").toString()).contains("active=true");
        assertThat(game.getSetupState().get("mulliganFlow").toString()).contains("readyForInitialSelection=false");
        verify(gameCardInstanceStateService, never()).saveAll(Mockito.anyList());
    }

    @Test
    void shouldResolveBothMulligansInterleavedInPersistedRandomOrder() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Card basicCard = card(UUID.randomUUID(), CardCategory.BASIC_POKEMON);
        Card energyCard = card(UUID.randomUUID(), CardCategory.BASIC_ENERGY);
        // Both players must Mulligan; current order is [p1, p2] but the random pick will reorder to [p2, p1].
        Game game = setupGameWithBothMulliganBarrier(gameId, playerOneId, playerTwoId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        List<GameCardInstance> playerOneHand = cardInstances(game, playerOneId, energyCard, CardZone.HAND, 7);
        List<GameCardInstance> playerOneDeck = cardInstances(game, playerOneId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerOneShuffledDeck = combinedDeck(playerOneDeck, playerOneHand);
        List<GameCardInstance> playerOneDeckAfterDraw = cardInstances(game, playerOneId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerOneDeckAfterExtra = cardInstances(game, playerOneId, basicCard, CardZone.DECK, 52);
        List<GameCardInstance> playerTwoHand = cardInstances(game, playerTwoId, energyCard, CardZone.HAND, 7);
        List<GameCardInstance> playerTwoDeck = cardInstances(game, playerTwoId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerTwoShuffledDeck = combinedDeck(playerTwoDeck, playerTwoHand);
        List<GameCardInstance> playerTwoDeckAfterDraw = cardInstances(game, playerTwoId, basicCard, CardZone.DECK, 53);
        List<GameCardInstance> playerTwoDeckAfterExtra = cardInstances(game, playerTwoId, basicCard, CardZone.DECK, 52);

        configureAckGame(game, participants);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerOneId, CardZone.HAND))
                .thenReturn(playerOneHand);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerOneId, CardZone.DECK))
                .thenReturn(playerOneDeck, playerOneShuffledDeck, playerOneDeckAfterDraw, playerOneDeckAfterExtra);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerTwoId, CardZone.HAND))
                .thenReturn(playerTwoHand);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerTwoId, CardZone.DECK))
                .thenReturn(playerTwoDeck, playerTwoShuffledDeck, playerTwoDeckAfterDraw, playerTwoDeckAfterExtra);
        when(cardService.getCardEntityById(basicCard.getId())).thenReturn(basicCard);
        when(cardService.getCardEntityById(energyCard.getId())).thenReturn(energyCard);
        // The random Mulligan turn order is forced to [p2, p1] (backend-decided, NOT insertion order); the
        // deck shuffle returns the prepared shuffled deck of the owner being processed.
        when(gameRandomService.shuffledCopy(Mockito.anyList())).thenAnswer(invocation -> {
            List<?> source = invocation.getArgument(0);
            if (!source.isEmpty() && source.get(0) instanceof UUID) {
                return List.of(playerTwoId, playerOneId);
            }
            GameCardInstance first = (GameCardInstance) source.get(0);
            return first.getOwnerUserId().equals(playerOneId) ? playerOneShuffledDeck : playerTwoShuffledDeck;
        });

        // Both confirm the barrier; the second acknowledgement starts the whole interleaved sequence.
        setupService.ackMulliganNotice(context(gameId, playerOneId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));
        GameActionExecutionResult result = setupService.ackMulliganNotice(
                context(gameId, playerTwoId, GameActionType.ACK_MULLIGAN_NOTICE, 1, Map.of()));

        // Reveal order follows the backend-decided random order [p2, p1]: player 2 first (seq 1), then
        // player 1 (seq 2). This proves the order comes from the persisted random pick, not insertion order.
        // The reveal is PUBLIC (both players see the invalid hand); only the invalid hand is exposed.
        ArgumentCaptor<Map<String, Object>> revealCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory, times(2)).publicEvent(
                eq(gameId), eq(GameEventType.MULLIGAN_HAND_REVEALED), anyInt(), revealCaptor.capture());
        assertThat(revealCaptor.getAllValues().get(0))
                .containsEntry("revealingPlayerId", playerTwoId.toString())
                .containsEntry("sequenceIndex", 1);
        assertThat(revealCaptor.getAllValues().get(1))
                .containsEntry("revealingPlayerId", playerOneId.toString())
                .containsEntry("sequenceIndex", 2);

        // Both resolved → flow completed, extra cards granted at the very end, selection enabled.
        assertThat(eventTypes(result.emittedEvents()))
                .contains(
                        GameEventType.MULLIGAN_HAND_REVEALED,
                        GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED,
                        GameEventType.MULLIGAN_FLOW_COMPLETED);
        assertThat(GameStateTestFactory.mulliganNoticePendingByPlayer(result.gameState()))
                .containsEntry(playerOneId, false)
                .containsEntry(playerTwoId, false);
        assertThat(game.getSetupState().get("mulliganFlow").toString()).contains("readyForInitialSelection=true");
    }

    @Test
    void shouldRejectInitialPokemonSelectionWhenMulliganNoticeIsPending() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID selectedCardInstanceId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId, true);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);

        GameActionContext context = context(
                gameId,
                playerOneId,
                GameActionType.CHOOSE_INITIAL_POKEMON,
                1,
                Map.<String, Object>of(
                        "activeCardInstanceId", selectedCardInstanceId.toString(),
                        "benchCardInstanceIds", List.of()));

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                setupService.chooseInitialPokemon(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Mulligan notice must be acknowledged before choosing initial Pokemon");
    }

    @Test
    void shouldRejectDuplicateInitialPokemonSelection() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID selectedCardInstanceId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);

        GameActionContext context = context(
                gameId,
                playerOneId,
                GameActionType.CHOOSE_INITIAL_POKEMON,
                1,
                Map.<String, Object>of(
                        "activeCardInstanceId", selectedCardInstanceId.toString(),
                        "benchCardInstanceIds", List.of(selectedCardInstanceId.toString())));

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                setupService.chooseInitialPokemon(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Initial Pokemon selection cannot contain duplicate cards");
    }

    @Test
    void shouldRejectInitialBenchOverCapacity() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());

        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);

        GameActionContext context = context(
                gameId,
                playerOneId,
                GameActionType.CHOOSE_INITIAL_POKEMON,
                1,
                Map.<String, Object>of(
                        "activeCardInstanceId", UUID.randomUUID().toString(),
                        "benchCardInstanceIds", List.of(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString())));

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                setupService.chooseInitialPokemon(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Initial bench cannot contain more than 5 Pokemon");
    }

    @Test
    void shouldRejectNonBasicInitialPokemonSelection() {
        UUID gameId = UUID.randomUUID();
        UUID playerOneId = UUID.randomUUID();
        UUID playerTwoId = UUID.randomUUID();
        UUID selectedCardInstanceId = UUID.randomUUID();
        UUID stageOneCardId = UUID.randomUUID();
        Game game = setupGame(gameId, playerOneId, playerTwoId);
        List<GameParticipant> participants = participants(game, playerOneId, playerTwoId, UUID.randomUUID(), UUID.randomUUID());
        GameCardInstance selectedCardInstance = cardInstance(game, playerOneId, selectedCardInstanceId, stageOneCardId, CardZone.HAND);

        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);
        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(selectedCardInstanceId, gameId, playerOneId))
                .thenReturn(Optional.of(selectedCardInstance));
        when(cardService.getCardEntityById(stageOneCardId)).thenReturn(card(stageOneCardId, CardCategory.STAGE_1_POKEMON));

        GameActionContext context = context(
                gameId,
                playerOneId,
                GameActionType.CHOOSE_INITIAL_POKEMON,
                1,
                Map.<String, Object>of(
                        "activeCardInstanceId", selectedCardInstanceId.toString(),
                        "benchCardInstanceIds", List.of()));

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                setupService.chooseInitialPokemon(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Only Basic Pokemon can be selected during setup");
    }

    private void configureStartGameBase(
            Game game,
            List<GameParticipant> participants,
            Deck deckOne,
            Deck deckTwo) {
        UUID gameId = game.getId();
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of());
        when(pokemonInPlayStateService.findByGameIdOrdered(gameId)).thenReturn(List.of());
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);
        when(gameDeckStateService.getRequiredDeckWithCards(participants.get(0).getDeckId(), participants.get(0).getUserId())).thenReturn(deckOne);
        when(gameDeckStateService.getRequiredDeckWithCards(participants.get(1).getDeckId(), participants.get(1).getUserId())).thenReturn(deckTwo);
        when(gameEventFactory.privateStateSync(eq(gameId), anyInt(), any(GameStateDto.class), any(UUID.class)))
                .thenReturn(event(gameId, GameEventType.STATE_SYNC));
        Mockito.lenient().when(gameEventFactory.publicEvent(
                        eq(gameId),
                        any(GameEventType.class),
                        anyInt(),
                        anyMap()))
                .thenAnswer(invocation -> event(gameId, invocation.getArgument(1)));
        Mockito.lenient().when(gameEventFactory.privateEvent(
                        eq(gameId),
                        any(GameEventType.class),
                        anyInt(),
                        anyMap(),
                        any(UUID.class)))
                .thenAnswer(invocation -> event(gameId, invocation.getArgument(1)));
    }

    private void configureAckGame(Game game, List<GameParticipant> participants) {
        UUID gameId = game.getId();
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameParticipantStateService.findOrderedByGameId(gameId)).thenReturn(participants);
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of());
        when(gameEventFactory.privateStateSync(eq(gameId), anyInt(), any(GameStateDto.class), any(UUID.class)))
                .thenReturn(event(gameId, GameEventType.STATE_SYNC));
        Mockito.lenient().when(gameEventFactory.publicEvent(
                        eq(gameId),
                        any(GameEventType.class),
                        anyInt(),
                        anyMap()))
                .thenAnswer(invocation -> event(gameId, invocation.getArgument(1)));
        Mockito.lenient().when(gameEventFactory.privateEvent(
                        eq(gameId),
                        any(GameEventType.class),
                        anyInt(),
                        anyMap(),
                        any(UUID.class)))
                .thenAnswer(invocation -> event(gameId, invocation.getArgument(1)));
    }

    @SuppressWarnings("unchecked")
    private List<Iterable<GameCardInstance>> savedCardInstanceBatches() {
        ArgumentCaptor<Iterable<GameCardInstance>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(gameCardInstanceStateService, times(2)).saveAll(captor.capture());
        return captor.getAllValues();
    }

    private int countZone(List<Iterable<GameCardInstance>> savedBatches, UUID ownerUserId, CardZone zone) {
        int count = 0;
        for (Iterable<GameCardInstance> savedBatch : savedBatches) {
            for (GameCardInstance cardInstance : savedBatch) {
                if (ownerUserId.equals(cardInstance.getOwnerUserId()) && zone.equals(cardInstance.getZone())) {
                    count++;
                }
            }
        }

        return count;
    }

    private List<GameEventType> eventTypes(List<GameEventDto> events) {
        List<GameEventType> eventTypes = new ArrayList<>();
        for (GameEventDto event : events) {
            eventTypes.add(event.eventType());
        }
        return List.copyOf(eventTypes);
    }

    private void assertOpeningHandsDealtPayload(
            Map<String, Object> payload,
            UUID playerOneId,
            UUID playerTwoId) {
        assertThat(payload).containsEntry("handSize", 7);
        assertThat(payload).containsEntry("deckShuffled", true);
        assertThat(payload).containsEntry("automatic", true);
        assertThat(payload.get("playerIds"))
                .isInstanceOf(List.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(String.class))
                .containsExactlyInAnyOrder(playerOneId.toString(), playerTwoId.toString());
        assertThat(payload).doesNotContainKeys(
                "cards", "cardIds", "revealedCards", "revealedCardIds", "deck", "hand", "prizeCards", "zone");
    }

    private void assertMulliganPayload(
            Map<String, Object> payload,
            UUID revealingPlayerId,
            int mulliganNumber,
            Card revealedCard) {
        assertThat(payload).containsEntry("revealingPlayerId", revealingPlayerId.toString());
        assertThat(payload).containsEntry("mulliganNumber", mulliganNumber);
        assertThat(payload).containsEntry("mulliganCount", mulliganNumber);
        assertThat(payload).containsEntry("extraCardsGranted", 1);
        assertThat(payload).containsEntry("pendingExtraCardsForViewer", mulliganNumber);
        assertThat(payload).doesNotContainKeys("viewerUserId", "cardInstanceId", "zone", "deck", "hand", "prizeCards");
        assertThat(payload.get("revealedCardIds")).isEqualTo(List.of(
                revealedCard.getId().toString(),
                revealedCard.getId().toString(),
                revealedCard.getId().toString(),
                revealedCard.getId().toString(),
                revealedCard.getId().toString(),
                revealedCard.getId().toString(),
                revealedCard.getId().toString()));

        Object revealedCardsValue = payload.get("revealedCards");
        assertThat(revealedCardsValue).isInstanceOf(List.class);
        List<?> revealedCards = (List<?>) revealedCardsValue;
        assertThat(revealedCards).hasSize(7);
        assertThat(revealedCards.get(0)).isInstanceOf(MulliganRevealedCardDto.class);
        MulliganRevealedCardDto firstCard = (MulliganRevealedCardDto) revealedCards.get(0);
        assertThat(firstCard.cardId()).isEqualTo(revealedCard.getId());
        assertThat(firstCard.name()).isEqualTo(revealedCard.getName());
        assertThat(firstCard.category()).isEqualTo(revealedCard.getCategory());
    }

    private void assertOwnMulliganSummaryPayload(
            Map<String, Object> payload,
            UUID playerId,
            int mulliganCount,
            UUID opponentPlayerId) {
        assertThat(payload).containsEntry("playerId", playerId.toString());
        assertThat(payload).containsEntry("mulliganCount", mulliganCount);
        assertThat(payload).containsEntry("extraCardsGrantedToOpponent", mulliganCount);
        assertThat(payload).containsEntry("opponentPlayerId", opponentPlayerId.toString());
        assertThat(payload).containsEntry("automatic", true);
        assertThat(payload).doesNotContainKeys(
                "viewerUserId",
                "revealedCardIds",
                "revealedCards",
                "cardInstanceId",
                "zone",
                "deck",
                "hand",
                "prizeCards");
    }

    private void assertMulliganNoticeAcknowledgedPayload(Map<String, Object> payload, UUID playerId) {
        assertThat(payload).containsEntry("playerId", playerId.toString());
        assertThat(payload).containsEntry("acknowledged", true);
        assertThat(payload).containsEntry("automatic", true);
        assertThat(payload).doesNotContainKeys(
                "viewerUserId",
                "revealedCardIds",
                "revealedCards",
                "cardInstanceId",
                "zone",
                "deck",
                "hand",
                "prizeCards");
    }

    private GameActionContext context(
            UUID gameId,
            UUID actorUserId,
            GameActionType actionType,
            int expectedStateVersion,
            Map<String, Object> payload) {
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.WAITING,
                null,
                0,
                expectedStateVersion,
                null,
                List.of(actorUserId),
                List.of(actionType),
                Instant.parse("2026-05-24T20:00:00Z"));
        return new GameActionContext(
                gameId,
                actorUserId,
                new GameActionRequestDto(gameId, UUID.randomUUID(), actionType, expectedStateVersion, payload),
                state);
    }

    private Game waitingGame(UUID gameId) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        return game;
    }

    private Game setupGame(UUID gameId, UUID playerOneId, UUID playerTwoId) {
        return setupGame(gameId, playerOneId, playerTwoId, false);
    }

    private Game setupGame(UUID gameId, UUID playerOneId, UUID playerTwoId, boolean pendingMulliganAcknowledgement) {
        Game game = waitingGame(gameId);
        game.setStatus(GameStatus.SETUP);
        game.setSetupState(Map.of(
                "mulliganCountByPlayer", Map.of(playerOneId.toString(), 0, playerTwoId.toString(), 0),
                "pendingMulliganAcknowledgementsByPlayer", Map.of(
                        playerOneId.toString(), pendingMulliganAcknowledgement,
                        playerTwoId.toString(), pendingMulliganAcknowledgement),
                "setupSelectionSubmittedByPlayer", Map.of(playerOneId.toString(), Boolean.FALSE, playerTwoId.toString(), Boolean.FALSE),
                "setupActiveCardInstanceIdByPlayer", Map.of(),
                "setupBenchCardInstanceIdsByPlayer", Map.of(playerOneId.toString(), List.of(), playerTwoId.toString(), List.of())));
        return game;
    }

    private Game setupGameWithActiveMulliganFlow(
            UUID gameId,
            UUID playerOneId,
            UUID playerTwoId,
            UUID currentMulliganPlayerId) {
        Game game = waitingGame(gameId);
        game.setStatus(GameStatus.SETUP);
        game.setSetupState(Map.of(
                "mulliganCountByPlayer", Map.of(playerOneId.toString(), 1, playerTwoId.toString(), 0),
                // Synchronized barrier: both players hold a pending acknowledgement until both confirm.
                "pendingMulliganAcknowledgementsByPlayer", Map.of(
                        playerOneId.toString(), Boolean.TRUE,
                        playerTwoId.toString(), Boolean.TRUE),
                "mulliganFlow", Map.of(
                        "active", Boolean.TRUE,
                        "roundNumber", 1,
                        "currentMulliganPlayerIds", List.of(currentMulliganPlayerId.toString()),
                        "resolvedPlayerIds", List.of(),
                        "extraCardsPendingByPlayer", Map.of(playerOneId.toString(), 0, playerTwoId.toString(), 1),
                        "readyForInitialSelection", Boolean.FALSE),
                "setupSelectionSubmittedByPlayer", Map.of(playerOneId.toString(), Boolean.FALSE, playerTwoId.toString(), Boolean.FALSE),
                "setupActiveCardInstanceIdByPlayer", Map.of(),
                "setupBenchCardInstanceIdsByPlayer", Map.of(playerOneId.toString(), List.of(), playerTwoId.toString(), List.of())));
        return game;
    }

    private Game setupGameWithBothMulliganBarrier(UUID gameId, UUID playerOneId, UUID playerTwoId) {
        Game game = waitingGame(gameId);
        game.setStatus(GameStatus.SETUP);
        game.setSetupState(Map.of(
                "mulliganCountByPlayer", Map.of(playerOneId.toString(), 1, playerTwoId.toString(), 1),
                // Barrier: both players must Mulligan and both hold a pending acknowledgement.
                "pendingMulliganAcknowledgementsByPlayer", Map.of(
                        playerOneId.toString(), Boolean.TRUE,
                        playerTwoId.toString(), Boolean.TRUE),
                "mulliganFlow", Map.of(
                        "active", Boolean.TRUE,
                        "roundNumber", 1,
                        // Insertion order [p1, p2]; the random pick will reorder it to [p2, p1].
                        "currentMulliganPlayerIds", List.of(playerOneId.toString(), playerTwoId.toString()),
                        "resolvedPlayerIds", List.of(),
                        "extraCardsPendingByPlayer", Map.of(playerOneId.toString(), 1, playerTwoId.toString(), 1),
                        "readyForInitialSelection", Boolean.FALSE),
                "setupSelectionSubmittedByPlayer", Map.of(playerOneId.toString(), Boolean.FALSE, playerTwoId.toString(), Boolean.FALSE),
                "setupActiveCardInstanceIdByPlayer", Map.of(),
                "setupBenchCardInstanceIdsByPlayer", Map.of(playerOneId.toString(), List.of(), playerTwoId.toString(), List.of())));
        return game;
    }

    private List<GameParticipant> participants(
            Game game,
            UUID playerOneId,
            UUID playerTwoId,
            UUID deckOneId,
            UUID deckTwoId) {
        List<GameParticipant> participants = new ArrayList<>();
        participants.add(participant(game, playerOneId, deckOneId, 1));
        participants.add(participant(game, playerTwoId, deckTwoId, 2));
        return participants;
    }

    private GameParticipant participant(Game game, UUID userId, UUID deckId, int playerOrder) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setDeckId(deckId);
        participant.setPlayerOrder(playerOrder);
        return participant;
    }

    private Deck deck(UUID deckId, Card card, int quantity) {
        Deck deck = new Deck();
        deck.setId(deckId);
        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        deck.setCards(List.of(deckCard));
        return deck;
    }

    private Deck mixedDeck(UUID deckId, Card basicCard, Card energyCard) {
        Deck deck = new Deck();
        deck.setId(deckId);
        DeckCard basicDeckCard = new DeckCard();
        basicDeckCard.setDeck(deck);
        basicDeckCard.setCard(basicCard);
        basicDeckCard.setQuantity(53);
        DeckCard energyDeckCard = new DeckCard();
        energyDeckCard.setDeck(deck);
        energyDeckCard.setCard(energyCard);
        energyDeckCard.setQuantity(7);
        deck.setCards(List.of(basicDeckCard, energyDeckCard));
        return deck;
    }

    private Card card(UUID cardId, CardCategory category) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("test-" + cardId);
        card.setSetCode("xy1");
        card.setNumber("1");
        card.setName(category.name() + " " + cardId);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(category);
        card.setImageSmallUrl("https://example.test/small/" + cardId);
        card.setImageLargeUrl("https://example.test/large/" + cardId);
        card.setHp(60);
        if (category == CardCategory.BASIC_POKEMON) {
            card.setSubtype("Basic");
        }
        return card;
    }

    private GameCardInstance cardInstance(Game game, UUID ownerUserId, UUID instanceId, UUID cardId, CardZone zone) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(instanceId);
        cardInstance.setGame(game);
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(1);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private List<GameCardInstance> cardInstances(
            Game game,
            UUID ownerUserId,
            Card card,
            CardZone zone,
            int count) {
        List<GameCardInstance> cardInstances = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            GameCardInstance cardInstance = cardInstance(game, ownerUserId, UUID.randomUUID(), card.getId(), zone);
            cardInstance.setZonePosition(index + 1);
            cardInstance.setFaceDown(CardZone.DECK.equals(zone) || CardZone.PRIZE.equals(zone));
            cardInstances.add(cardInstance);
        }
        return cardInstances;
    }

    private List<GameCardInstance> combinedDeck(
            List<GameCardInstance> firstCards,
            List<GameCardInstance> remainingCards) {
        List<GameCardInstance> combinedCards = new ArrayList<>();
        combinedCards.addAll(firstCards);
        combinedCards.addAll(remainingCards);
        return combinedCards;
    }

    private List<Card> expandedCards(Card card, int quantity) {
        List<Card> cards = new ArrayList<>();
        for (int index = 0; index < quantity; index++) {
            cards.add(card);
        }
        return cards;
    }

    private List<Card> shuffledWithOpeningCards(Card openingCard, Card remainingCard) {
        List<Card> cards = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            cards.add(openingCard);
        }
        for (int index = 7; index < 60; index++) {
            cards.add(remainingCard);
        }
        return cards;
    }

    private GameEventDto event(UUID gameId, GameEventType eventType) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                eventType,
                1,
                eventType == GameEventType.STATE_SYNC
                        || eventType == GameEventType.MULLIGAN_REQUIRED
                        || eventType == GameEventType.MULLIGAN_HAND_REVEALED
                        || eventType == GameEventType.MULLIGAN_NEW_HAND_DRAWN
                        || eventType == GameEventType.MULLIGAN_SEQUENCE_COMPLETED
                        || eventType == GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED,
                Instant.parse("2026-05-24T20:00:01Z"),
                Map.of());
    }
}
