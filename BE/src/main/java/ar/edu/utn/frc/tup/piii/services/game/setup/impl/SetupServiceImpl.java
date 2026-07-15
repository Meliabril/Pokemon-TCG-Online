package ar.edu.utn.frc.tup.piii.services.game.setup.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.MulliganRevealedCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
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
import ar.edu.utn.frc.tup.piii.services.game.setup.SetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetupServiceImpl implements SetupService {

    private static final int REQUIRED_DECK_SIZE = 60;
    private static final int INITIAL_HAND_SIZE = 7;
    private static final int PRIZE_COUNT = 6;
    private static final int MAX_BENCH_SIZE = 5;
    private static final int MAX_MULLIGAN_ATTEMPTS = 100;
    private static final String MULLIGAN_COUNT_KEY = "mulliganCountByPlayer";
    private static final String MULLIGAN_ACK_PENDING_KEY = "pendingMulliganAcknowledgementsByPlayer";
    private static final String MULLIGAN_FLOW_KEY = "mulliganFlow";
    private static final String MULLIGAN_FLOW_ACTIVE_KEY = "active";
    private static final String MULLIGAN_FLOW_ROUND_NUMBER_KEY = "roundNumber";
    private static final String MULLIGAN_FLOW_CURRENT_PLAYERS_KEY = "currentMulliganPlayerIds";
    private static final String MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY = "resolvedPlayerIds";
    private static final String MULLIGAN_FLOW_EXTRA_CARDS_PENDING_KEY = "extraCardsPendingByPlayer";
    private static final String MULLIGAN_FLOW_TURN_ORDER_KEY = "turnOrder";
    private static final String MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY = "readyForInitialSelection";
    private static final String MULLIGAN_CHECKPOINT_REVEAL = "REVEAL_INVALID_HAND";
    private static final String SELECTION_SUBMITTED_KEY = "setupSelectionSubmittedByPlayer";
    private static final String ACTIVE_SELECTION_KEY = "setupActiveCardInstanceIdByPlayer";
    private static final String BENCH_SELECTION_KEY = "setupBenchCardInstanceIdsByPlayer";
    private static final String ACTIVE_CARD_INSTANCE_ID_KEY = "activeCardInstanceId";
    private static final String BENCH_CARD_INSTANCE_IDS_KEY = "benchCardInstanceIds";

    private final GameLookupService gameLookupService;
    private final GameParticipantStateService gameParticipantStateService;
    private final GameDeckStateService gameDeckStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final CardService cardService;

    @Override
    public GameActionExecutionResult startGame(GameActionContext context) {
        Game game = gameLookupService.getRequiredGame(context.gameId());
        if (game.getStatus() != GameStatus.WAITING) {
            throw new InvalidGameActionException("Only waiting games can be started");
        }
        if (!gameCardInstanceStateService.findByGameId(game.getId()).isEmpty()) {
            throw new InvalidGameActionException("Game setup has already been initialized");
        }
        if (!pokemonInPlayStateService.findByGameIdOrdered(game.getId()).isEmpty()) {
            throw new InvalidGameActionException("Pokemon board has already been initialized");
        }

        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(game.getId());
        if (participants.size() != 2) {
            throw new InvalidGameActionException("Exactly two participants are required to start a game");
        }

        int newStateVersion = nextStateVersion(context, game);
        Instant now = Instant.now();
        List<UUID> playerIds = playerIds(participants);
        List<GameEventDto> events = new ArrayList<>();
        Map<UUID, PreparedSetup> preparedSetupByPlayer = new LinkedHashMap<>();
        Map<UUID, Integer> mulliganCountByPlayer = new LinkedHashMap<>();
        List<UUID> currentMulliganPlayerIds = new ArrayList<>();

        for (GameParticipant participant : participants) {
            Deck deck = gameDeckStateService.getRequiredDeckWithCards(participant.getDeckId(), participant.getUserId());
            PreparedSetup preparedSetup = prepareInitialSetup(deck);
            preparedSetupByPlayer.put(participant.getUserId(), preparedSetup);
            if (containsBasicPokemon(preparedSetup.handCards())) {
                mulliganCountByPlayer.put(participant.getUserId(), 0);
            } else {
                mulliganCountByPlayer.put(participant.getUserId(), 1);
                currentMulliganPlayerIds.add(participant.getUserId());
            }
        }

        boolean hasMulliganFlow = !currentMulliganPlayerIds.isEmpty();
        for (GameParticipant participant : participants) {
            PreparedSetup preparedSetup = preparedSetupByPlayer.get(participant.getUserId());
            if (!hasMulliganFlow) {
                placePrizeCards(preparedSetup);
            }
            saveInitialCardInstances(game, participant.getUserId(), preparedSetup);
        }

        game.setStatus(GameStatus.SETUP);
        game.setCurrentPhase(null);
        game.setTurnNumber(0);
        game.setActivePlayerId(null);
        game.setPlayerWhoWentFirstId(null);
        game.setStartedAt(now);
        game.setSetupState(initialSetupState(playerIds, mulliganCountByPlayer, currentMulliganPlayerIds));

        // Opening hands are dealt and persisted; announce it once (public, no sensitive data) so the
        // client can animate the initial shuffle/deal before any Mulligan or state-sync events.
        events.add(gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.OPENING_HANDS_DEALT,
                newStateVersion,
                openingHandsDealtPayload(playerIds)));

        if (hasMulliganFlow) {
            addMulliganCheckpointEvents(
                    game.getId(),
                    currentMulliganPlayerIds,
                    mulliganCountByPlayer,
                    newStateVersion,
                    events);
        }

        GameStateDto state = buildState(game, participants, newStateVersion, Map.of());
        addPrivateStateSyncEvents(events, game.getId(), newStateVersion, state, playerIds);

        return new GameActionExecutionResult(state, events);
    }

    @Override
    public GameActionExecutionResult ackMulliganNotice(GameActionContext context) {
        Game game = gameLookupService.getRequiredGame(context.gameId());
        if (game.getStatus() != GameStatus.SETUP) {
            throw new InvalidGameActionException("Mulligan notices can only be acknowledged during setup");
        }

        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(game.getId());
        if (participants.size() != 2) {
            throw new InvalidGameActionException("Exactly two participants are required during setup");
        }

        UUID actorUserId = context.actorUserId();
        int newStateVersion = nextStateVersion(context, game);
        Map<String, Object> setupState = mutableSetupState(game.getSetupState());
        boolean pendingAcknowledgement = hasPendingMulliganAcknowledgement(setupState, actorUserId);
        List<GameEventDto> events = new ArrayList<>();

        if (pendingAcknowledgement) {
            acknowledgeMulliganNotice(setupState, actorUserId);
            events.add(gameEventFactory.publicEvent(
                    game.getId(),
                    GameEventType.MULLIGAN_NOTICE_ACKNOWLEDGED,
                    newStateVersion,
                    mulliganNoticeAcknowledgedPayload(actorUserId, mulliganRoundNumber(setupState))));

            // Single SYNCHRONIZED initial barrier: BOTH players hold a pending acknowledgement while any
            // Mulligan is required. The automatic sequence only starts once EVERY player has confirmed,
            // so both clients begin animating at the same logical moment. There are no per-round ACKs:
            // after the barrier, all required Mulligans resolve automatically here, in a single
            // backend-decided, interleaved order.
            if (isMulliganFlowActive(setupState) && !hasAnyPendingMulliganAcknowledgement(setupState)) {
                runInterleavedMulligan(game, participants, setupState, newStateVersion, events);
            }
            game.setSetupState(Map.copyOf(setupState));
        }

        List<UUID> playerIds = playerIds(participants);
        GameStateDto state = buildState(game, participants, newStateVersion, Map.of());
        addPrivateStateSyncEvents(events, game.getId(), newStateVersion, state, playerIds);

        return new GameActionExecutionResult(state, events);
    }

    @Override
    public GameActionExecutionResult chooseInitialPokemon(GameActionContext context) {
        Game game = gameLookupService.getRequiredGame(context.gameId());
        if (game.getStatus() != GameStatus.SETUP) {
            throw new InvalidGameActionException("Initial Pokemon can only be selected during setup");
        }

        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(game.getId());
        if (participants.size() != 2) {
            throw new InvalidGameActionException("Exactly two participants are required during setup");
        }

        int newStateVersion = nextStateVersion(context, game);
        UUID actorUserId = context.actorUserId();
        Map<String, Object> setupState = mutableSetupState(game.getSetupState());
        if (isMulliganFlowActive(setupState) || !isReadyForInitialSelection(setupState)) {
            throw new InvalidGameActionException("Mulligan flow must be completed before choosing initial Pokemon");
        }
        if (hasPendingMulliganAcknowledgement(setupState, actorUserId)) {
            throw new InvalidGameActionException("Mulligan notice must be acknowledged before choosing initial Pokemon");
        }

        Map<String, Object> submittedByPlayer = mutableNestedMap(setupState, SELECTION_SUBMITTED_KEY);
        if (Boolean.TRUE.equals(booleanValue(submittedByPlayer.get(actorUserId.toString())))) {
            throw new InvalidGameActionException("Initial Pokemon selection has already been submitted");
        }

        Map<String, Object> payload = context.request().payload();
        UUID activeCardInstanceId = requiredUuid(payload.get(ACTIVE_CARD_INSTANCE_ID_KEY), ACTIVE_CARD_INSTANCE_ID_KEY);
        List<UUID> benchCardInstanceIds = optionalUuidList(payload.get(BENCH_CARD_INSTANCE_IDS_KEY), BENCH_CARD_INSTANCE_IDS_KEY);
        validateSelectionUniqueness(activeCardInstanceId, benchCardInstanceIds);
        if (benchCardInstanceIds.size() > MAX_BENCH_SIZE) {
            throw new InvalidGameActionException("Initial bench cannot contain more than 5 Pokemon");
        }

        validateSelectedBasicPokemon(game.getId(), actorUserId, activeCardInstanceId);
        for (UUID benchCardInstanceId : benchCardInstanceIds) {
            validateSelectedBasicPokemon(game.getId(), actorUserId, benchCardInstanceId);
        }

        registerSelection(setupState, actorUserId, activeCardInstanceId, benchCardInstanceIds);
        game.setSetupState(Map.copyOf(setupState));

        List<UUID> playerIds = playerIds(participants);
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.INITIAL_POKEMON_SELECTED,
                newStateVersion,
                Map.of("playerId", actorUserId.toString())));

        GameStateDto state;
        if (allPlayersSubmitted(setupState, playerIds)) {
            state = completeInitialPokemonSelection(game, participants, newStateVersion);
            events.add(gameEventFactory.publicEvent(
                    game.getId(),
                    GameEventType.INITIAL_BOARD_REVEALED,
                    newStateVersion,
                    Map.of("turnNumber", 1)));
            events.add(gameEventFactory.publicEvent(
                    game.getId(),
                    GameEventType.GAME_STARTED,
                    newStateVersion,
                    Map.of("activePlayerId", state.turn().activePlayerId().toString(), "turnNumber", 1)));
        } else {
            state = buildState(game, participants, newStateVersion, Map.of());
        }

        addPrivateStateSyncEvents(events, game.getId(), newStateVersion, state, playerIds);

        return new GameActionExecutionResult(state, events);
    }

    private PreparedSetup prepareInitialSetup(Deck deck) {
        List<Card> expandedCards = expandDeck(deck);
        validateDeckForSetup(expandedCards);

        List<Card> shuffledCards = gameRandomService.shuffledCopy(expandedCards);
        List<Card> openingHand = firstCards(shuffledCards, INITIAL_HAND_SIZE);
        List<Card> deckCards = remainingCards(shuffledCards, INITIAL_HAND_SIZE);
        return new PreparedSetup(openingHand, new ArrayList<>(), deckCards);
    }

    private void validateDeckForSetup(List<Card> expandedCards) {
        if (expandedCards.size() != REQUIRED_DECK_SIZE) {
            throw new InvalidGameActionException("Deck must contain exactly 60 cards to initialize the game");
        }
        if (!containsBasicPokemon(expandedCards)) {
            throw new InvalidGameActionException("Deck must contain at least one Basic Pokemon");
        }
    }

    private void drawExtraCards(PreparedSetup preparedSetup, int extraCards) {
        for (int drawnCards = 0; drawnCards < extraCards; drawnCards++) {
            if (preparedSetup.deckCards().isEmpty()) {
                throw new InvalidGameActionException("Deck does not contain enough cards for mulligan rewards");
            }
            Card card = preparedSetup.deckCards().remove(0);
            preparedSetup.handCards().add(card);
        }
    }

    private void placePrizeCards(PreparedSetup preparedSetup) {
        if (preparedSetup.deckCards().size() < PRIZE_COUNT) {
            throw new InvalidGameActionException("Deck does not contain enough cards to place Prize cards");
        }

        for (int prizeIndex = 0; prizeIndex < PRIZE_COUNT; prizeIndex++) {
            Card card = preparedSetup.deckCards().remove(0);
            preparedSetup.prizeCards().add(card);
        }
    }

    private void saveInitialCardInstances(Game game, UUID ownerUserId, PreparedSetup preparedSetup) {
        List<GameCardInstance> gameCardInstances = new ArrayList<>();
        int handPosition = 1;
        for (Card card : preparedSetup.handCards()) {
            gameCardInstances.add(cardInstance(game, ownerUserId, card.getId(), CardZone.HAND, handPosition, false));
            handPosition++;
        }

        int prizePosition = 1;
        for (Card card : preparedSetup.prizeCards()) {
            gameCardInstances.add(cardInstance(game, ownerUserId, card.getId(), CardZone.PRIZE, prizePosition, true));
            prizePosition++;
        }

        int deckPosition = 1;
        for (Card card : preparedSetup.deckCards()) {
            gameCardInstances.add(cardInstance(game, ownerUserId, card.getId(), CardZone.DECK, deckPosition, true));
            deckPosition++;
        }

        gameCardInstanceStateService.saveAll(gameCardInstances);
    }

    /**
     * Resolves the whole Mulligan flow once the barrier is satisfied, as a single backend-decided,
     * INTERLEAVED sequence so both clients animate the exact same order. When more than one player must
     * Mulligan, the turn order is chosen randomly ONCE (and persisted) so it cannot depend on which
     * client acked first or which event arrived first. Then attempts alternate per round
     * (player1 attempt, player2 attempt, player1 attempt, ...) until each has a Basic Pokemon; a player
     * that already validated leaves the cycle and only the rest keeps going. Every emitted event carries
     * a monotonic {@code sequenceIndex} so the frontend can order animations deterministically. Extra
     * cards are granted ONLY at the very end (see {@code completeMulliganFlow}).
     */
    private void runInterleavedMulligan(
            Game game,
            List<GameParticipant> participants,
            Map<String, Object> setupState,
            int stateVersion,
            List<GameEventDto> events) {
        List<UUID> playerIds = playerIds(participants);
        // Decide and persist the order ONCE (random when both must Mulligan; a single player → list of 1).
        List<UUID> turnOrder = gameRandomService.shuffledCopy(currentMulliganPlayerIds(setupState));
        persistMulliganTurnOrder(setupState, turnOrder);

        Map<UUID, Integer> mulliganCountByPlayer = mulliganCountByPlayer(setupState, playerIds);
        List<UUID> activePlayers = new ArrayList<>(turnOrder);
        int sequenceIndex = 0;
        int roundNumber = mulliganRoundNumber(setupState);

        while (!activePlayers.isEmpty()) {
            List<UUID> stillActive = new ArrayList<>();
            for (UUID playerUserId : activePlayers) {
                UUID opponentUserId = opponentUserId(playerIds, playerUserId);
                int mulliganCount = mulliganCountByPlayer.getOrDefault(playerUserId, 0);
                sequenceIndex++;
                boolean hasBasicPokemon = performOneMulliganAttempt(
                        game, playerUserId, mulliganCount, roundNumber, sequenceIndex, stateVersion, events);

                if (hasBasicPokemon) {
                    mulliganCountByPlayer.put(playerUserId, mulliganCount);
                    markMulliganPlayerResolved(setupState, playerUserId);
                    removeFromCurrentMulliganPlayers(setupState, playerUserId);
                    addOwnMulliganSummaryEvent(
                            game.getId(), playerUserId, opponentUserId, mulliganCount, sequenceIndex, stateVersion, events);
                } else {
                    int nextMulliganCount = mulliganCount + 1;
                    if (nextMulliganCount > MAX_MULLIGAN_ATTEMPTS) {
                        throw new InvalidGameActionException("Could not draw an opening hand with a Basic Pokemon");
                    }
                    mulliganCountByPlayer.put(playerUserId, nextMulliganCount);
                    stillActive.add(playerUserId);
                }
                putMulliganCounts(setupState, mulliganCountByPlayer);
            }
            activePlayers = stillActive;
            roundNumber++;
        }

        if (currentMulliganPlayerIds(setupState).isEmpty()) {
            Map<UUID, Integer> finalCounts = mulliganCountByPlayer(setupState, playerIds);
            completeMulliganFlow(
                    game, playerIds, setupState, finalCounts, roundNumber, sequenceIndex, stateVersion, events);
        }
    }

    /**
     * Performs a SINGLE Mulligan attempt for one player: reveal the current invalid hand to the rival,
     * return it to the deck, shuffle, draw a new hand, and validate. Returns whether the new hand has a
     * Basic Pokemon. The validated event carries the post-attempt count (matching the legacy behaviour).
     */
    private boolean performOneMulliganAttempt(
            Game game,
            UUID playerUserId,
            int mulliganCount,
            int roundNumber,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        revealMulliganHandToBothPlayers(game.getId(), playerUserId, mulliganCount, sequenceIndex, stateVersion, events);
        returnHandToDeckAndShuffle(game.getId(), playerUserId, mulliganCount, roundNumber, sequenceIndex, stateVersion, events);
        List<Card> newHand = drawNewMulliganHand(game.getId(), playerUserId, mulliganCount, roundNumber, sequenceIndex, stateVersion, events);
        boolean hasBasicPokemon = containsBasicPokemon(newHand);
        int validatedCount = hasBasicPokemon ? mulliganCount : mulliganCount + 1;

        events.add(gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.MULLIGAN_HAND_VALIDATED,
                stateVersion,
                mulliganValidationPayload(playerUserId, validatedCount, roundNumber, hasBasicPokemon, sequenceIndex)));

        return hasBasicPokemon;
    }

    private void persistMulliganTurnOrder(Map<String, Object> setupState, List<UUID> turnOrder) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        flowState.put(MULLIGAN_FLOW_TURN_ORDER_KEY, uuidStrings(turnOrder));
        setupState.put(MULLIGAN_FLOW_KEY, Map.copyOf(flowState));
    }

    private void removeFromCurrentMulliganPlayers(Map<String, Object> setupState, UUID playerUserId) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        List<UUID> currentPlayers = optionalUuidList(
                flowState.get(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY), MULLIGAN_FLOW_CURRENT_PLAYERS_KEY);
        List<UUID> updated = new ArrayList<>(currentPlayers);
        updated.remove(playerUserId);
        flowState.put(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY, uuidStrings(updated));
        setupState.put(MULLIGAN_FLOW_KEY, Map.copyOf(flowState));
    }

    private void returnHandToDeckAndShuffle(
            UUID gameId,
            UUID playerUserId,
            int mulliganNumber,
            int roundNumber,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        List<GameCardInstance> handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.HAND);
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.DECK);
        if (handCards.size() < INITIAL_HAND_SIZE) {
            throw new InvalidGameActionException("Opening hand does not contain enough cards for Mulligan");
        }

        List<GameCardInstance> cardsToShuffle = new ArrayList<>();
        cardsToShuffle.addAll(handCards);
        cardsToShuffle.addAll(deckCards);
        List<GameCardInstance> shuffledCards = gameRandomService.shuffledCopy(cardsToShuffle);
        int deckPosition = 1;
        for (GameCardInstance cardInstance : shuffledCards) {
            cardInstance.setZone(CardZone.DECK);
            cardInstance.setZonePosition(deckPosition++);
            cardInstance.setFaceDown(true);
        }
        gameCardInstanceStateService.saveAll(shuffledCards);

        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.MULLIGAN_HAND_RETURNED,
                stateVersion,
                mulliganStepPayload(playerUserId, mulliganNumber, roundNumber, sequenceIndex)));
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.MULLIGAN_DECK_SHUFFLED,
                stateVersion,
                mulliganStepPayload(playerUserId, mulliganNumber, roundNumber, sequenceIndex)));
    }

    private List<Card> drawNewMulliganHand(
            UUID gameId,
            UUID playerUserId,
            int mulliganNumber,
            int roundNumber,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.DECK);
        if (deckCards.size() < INITIAL_HAND_SIZE) {
            throw new InvalidGameActionException("Deck does not contain enough cards for Mulligan");
        }

        List<GameCardInstance> changedCards = new ArrayList<>();
        List<Card> newHand = new ArrayList<>();
        for (int index = 0; index < deckCards.size(); index++) {
            GameCardInstance cardInstance = deckCards.get(index);
            if (index < INITIAL_HAND_SIZE) {
                cardInstance.setZone(CardZone.HAND);
                cardInstance.setZonePosition(index + 1);
                cardInstance.setFaceDown(false);
                newHand.add(cardService.getCardEntityById(cardInstance.getCardId()));
            } else {
                cardInstance.setZone(CardZone.DECK);
                cardInstance.setZonePosition(index - INITIAL_HAND_SIZE + 1);
                cardInstance.setFaceDown(true);
            }
            changedCards.add(cardInstance);
        }
        gameCardInstanceStateService.saveAll(changedCards);

        // PRIVATE to the owner: includes the freshly drawn hand so the client can show the real new hand
        // after each draw (even on intermediate, still-invalid attempts). These are the owner's OWN cards
        // in a private event — no leak of the rival's hand, deck order or prizes.
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.MULLIGAN_NEW_HAND_DRAWN,
                stateVersion,
                mulliganNewHandDrawnPayload(playerUserId, mulliganNumber, roundNumber, sequenceIndex, newHand),
                playerUserId));
        return List.copyOf(newHand);
    }

    private void completeMulliganFlow(
            Game game,
            List<UUID> playerIds,
            Map<String, Object> setupState,
            Map<UUID, Integer> mulliganCountByPlayer,
            int roundNumber,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        // Extra cards are granted ONLY here, at the very end of the whole interleaved flow (never between
        // attempts). Their sequenceIndex continues after the last attempt so the frontend animates them last.
        int extraCardsSequenceIndex = sequenceIndex;
        for (UUID playerUserId : playerIds) {
            UUID opponentUserId = opponentUserId(playerIds, playerUserId);
            int extraCards = mulliganCountByPlayer.getOrDefault(opponentUserId, 0);
            extraCardsSequenceIndex++;
            drawExtraCardsFromDeck(
                    game.getId(), playerUserId, opponentUserId, extraCards, extraCardsSequenceIndex, stateVersion, events);
        }
        for (UUID playerUserId : playerIds) {
            placePrizeCards(game.getId(), playerUserId);
        }

        Map<String, Object> pendingAcknowledgements = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            pendingAcknowledgements.put(playerId.toString(), Boolean.FALSE);
        }
        setupState.put(MULLIGAN_ACK_PENDING_KEY, Map.copyOf(pendingAcknowledgements));
        setupState.put(MULLIGAN_FLOW_KEY, Map.copyOf(completedMulliganFlowState(playerIds, mulliganCountByPlayer, roundNumber)));

        events.add(gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.MULLIGAN_FLOW_COMPLETED,
                stateVersion,
                Map.of(
                        "roundNumber", roundNumber,
                        "sequenceIndex", extraCardsSequenceIndex + 1,
                        "automatic", true)));
    }

    private void drawExtraCardsFromDeck(
            UUID gameId,
            UUID playerUserId,
            UUID sourceMulliganPlayerId,
            int extraCards,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        if (extraCards <= 0) {
            return;
        }

        List<GameCardInstance> handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.HAND);
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.DECK);
        if (deckCards.size() < extraCards) {
            throw new InvalidGameActionException("Deck does not contain enough cards for mulligan rewards");
        }

        List<GameCardInstance> changedCards = new ArrayList<>();
        int nextHandPosition = handCards.size() + 1;
        for (int index = 0; index < deckCards.size(); index++) {
            GameCardInstance cardInstance = deckCards.get(index);
            if (index < extraCards) {
                cardInstance.setZone(CardZone.HAND);
                cardInstance.setZonePosition(nextHandPosition++);
                cardInstance.setFaceDown(false);
            } else {
                cardInstance.setZone(CardZone.DECK);
                cardInstance.setZonePosition(index - extraCards + 1);
                cardInstance.setFaceDown(true);
            }
            changedCards.add(cardInstance);
        }
        gameCardInstanceStateService.saveAll(changedCards);
        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.MULLIGAN_EXTRA_CARDS_GRANTED,
                stateVersion,
                mulliganExtraCardsPayload(playerUserId, sourceMulliganPlayerId, extraCards, sequenceIndex),
                playerUserId));
    }

    private void placePrizeCards(UUID gameId, UUID playerUserId) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.DECK);
        if (deckCards.size() < PRIZE_COUNT) {
            throw new InvalidGameActionException("Deck does not contain enough cards to place Prize cards");
        }

        List<GameCardInstance> changedCards = new ArrayList<>();
        for (int index = 0; index < deckCards.size(); index++) {
            GameCardInstance cardInstance = deckCards.get(index);
            if (index < PRIZE_COUNT) {
                cardInstance.setZone(CardZone.PRIZE);
                cardInstance.setZonePosition(index + 1);
                cardInstance.setFaceDown(true);
            } else {
                cardInstance.setZone(CardZone.DECK);
                cardInstance.setZonePosition(index - PRIZE_COUNT + 1);
                cardInstance.setFaceDown(true);
            }
            changedCards.add(cardInstance);
        }
        gameCardInstanceStateService.saveAll(changedCards);
    }

    private GameStateDto completeInitialPokemonSelection(Game game, List<GameParticipant> participants, int stateVersion) {
        Map<String, Object> setupState = mutableSetupState(game.getSetupState());
        Map<String, Object> activeSelections = mutableNestedMap(setupState, ACTIVE_SELECTION_KEY);
        Map<String, Object> benchSelections = mutableNestedMap(setupState, BENCH_SELECTION_KEY);
        Map<UUID, Integer> pokemonEnteredPlayTurnMap = new LinkedHashMap<>();

        for (GameParticipant participant : participants) {
            UUID ownerUserId = participant.getUserId();
            UUID activeCardInstanceId = requiredUuid(activeSelections.get(ownerUserId.toString()), ACTIVE_CARD_INSTANCE_ID_KEY);
            GameCardInstance activeCardInstance = validateSelectedBasicPokemon(game.getId(), ownerUserId, activeCardInstanceId);
            moveCardToZone(activeCardInstance, CardZone.ACTIVE, 0, false);
            PokemonInPlay activePokemon = savePokemonInPlay(game, ownerUserId, activeCardInstance, 0);
            pokemonEnteredPlayTurnMap.put(activePokemon.getId(), 1);

            List<UUID> benchCardInstanceIds = optionalUuidList(benchSelections.get(ownerUserId.toString()), BENCH_CARD_INSTANCE_IDS_KEY);
            int benchSlot = 1;
            for (UUID benchCardInstanceId : benchCardInstanceIds) {
                GameCardInstance benchCardInstance = validateSelectedBasicPokemon(game.getId(), ownerUserId, benchCardInstanceId);
                moveCardToZone(benchCardInstance, CardZone.BENCH, benchSlot, false);
                PokemonInPlay benchPokemon = savePokemonInPlay(game, ownerUserId, benchCardInstance, benchSlot);
                pokemonEnteredPlayTurnMap.put(benchPokemon.getId(), 1);
                benchSlot++;
            }

            gameCardInstanceStateService.resequenceZone(game.getId(), ownerUserId, CardZone.HAND);
        }

        List<UUID> playerIds = playerIds(participants);
        UUID firstPlayerId = gameRandomService.chooseOne(playerIds);
        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPhase(TurnPhase.DRAW);
        game.setTurnNumber(1);
        game.setActivePlayerId(firstPlayerId);
        game.setPlayerWhoWentFirstId(firstPlayerId);
        // Mulligan counters are setup-only in this first observable phase.
        game.setTurnStartedAt(Instant.now());
        game.setSetupState(Map.of());

        return buildState(game, participants, stateVersion, Map.copyOf(pokemonEnteredPlayTurnMap));
    }

    private PokemonInPlay savePokemonInPlay(Game game, UUID ownerUserId, GameCardInstance cardInstance, int slotPosition) {
        PokemonInPlay pokemonInPlay = pokemonInPlay(game, ownerUserId, cardInstance, slotPosition, 1);
        PokemonInPlay savedPokemon = pokemonInPlayStateService.save(pokemonInPlay);
        pokemonEvolutionStackStateService.save(evolutionBase(savedPokemon, cardInstance));
        return savedPokemon;
    }

    private void moveCardToZone(GameCardInstance cardInstance, CardZone zone, Integer zonePosition, boolean faceDown) {
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(zonePosition);
        cardInstance.setFaceDown(faceDown);
        gameCardInstanceStateService.save(cardInstance);
    }

    private GameCardInstance validateSelectedBasicPokemon(UUID gameId, UUID ownerUserId, UUID cardInstanceId) {
        Optional<GameCardInstance> foundCardInstance = gameCardInstanceStateService
                .findByIdAndGameIdAndOwnerUserId(cardInstanceId, gameId, ownerUserId);
        if (foundCardInstance.isEmpty()) {
            throw new InvalidGameActionException("Selected card is not in player's hand");
        }
        GameCardInstance cardInstance = foundCardInstance.get();

        if (!CardZone.HAND.equals(cardInstance.getZone())) {
            throw new InvalidGameActionException("Selected card is not in player's hand");
        }

        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (!isBasicPokemon(card)) {
            throw new InvalidGameActionException("Only Basic Pokemon can be selected during setup");
        }

        return cardInstance;
    }

    private void validateSelectionUniqueness(UUID activeCardInstanceId, List<UUID> benchCardInstanceIds) {
        Set<UUID> selectedCardInstanceIds = new HashSet<>();
        selectedCardInstanceIds.add(activeCardInstanceId);
        for (UUID benchCardInstanceId : benchCardInstanceIds) {
            if (!selectedCardInstanceIds.add(benchCardInstanceId)) {
                throw new InvalidGameActionException("Initial Pokemon selection cannot contain duplicate cards");
            }
        }
    }

    private void registerSelection(
            Map<String, Object> setupState,
            UUID actorUserId,
            UUID activeCardInstanceId,
            List<UUID> benchCardInstanceIds) {
        Map<String, Object> submittedByPlayer = mutableNestedMap(setupState, SELECTION_SUBMITTED_KEY);
        Map<String, Object> activeSelections = mutableNestedMap(setupState, ACTIVE_SELECTION_KEY);
        Map<String, Object> benchSelections = mutableNestedMap(setupState, BENCH_SELECTION_KEY);
        String playerKey = actorUserId.toString();

        submittedByPlayer.put(playerKey, Boolean.TRUE);
        activeSelections.put(playerKey, activeCardInstanceId.toString());
        benchSelections.put(playerKey, uuidStrings(benchCardInstanceIds));

        setupState.put(SELECTION_SUBMITTED_KEY, Map.copyOf(submittedByPlayer));
        setupState.put(ACTIVE_SELECTION_KEY, Map.copyOf(activeSelections));
        setupState.put(BENCH_SELECTION_KEY, Map.copyOf(benchSelections));
    }

    private boolean allPlayersSubmitted(Map<String, Object> setupState, List<UUID> playerIds) {
        Map<String, Object> submittedByPlayer = mutableNestedMap(setupState, SELECTION_SUBMITTED_KEY);
        for (UUID playerId : playerIds) {
            if (!Boolean.TRUE.equals(booleanValue(submittedByPlayer.get(playerId.toString())))) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Object> initialSetupState(
            List<UUID> playerIds,
            Map<UUID, Integer> mulliganCountByPlayer,
            List<UUID> currentMulliganPlayerIds) {
        Map<String, Object> setupState = new LinkedHashMap<>();
        Map<String, Object> mulliganCounts = new LinkedHashMap<>();
        Map<String, Object> pendingMulliganAcknowledgements = new LinkedHashMap<>();
        Map<String, Object> submittedValues = new LinkedHashMap<>();
        Map<String, Object> activeSelections = new LinkedHashMap<>();
        Map<String, Object> benchSelections = new LinkedHashMap<>();
        boolean mulliganFlowActive = !currentMulliganPlayerIds.isEmpty();

        for (UUID playerId : playerIds) {
            Integer mulliganCount = mulliganCountByPlayer.get(playerId);
            if (mulliganCount == null) {
                mulliganCounts.put(playerId.toString(), 0);
            } else {
                mulliganCounts.put(playerId.toString(), mulliganCount);
            }
            // Synchronized initial barrier: if ANY player must Mulligan, BOTH players hold a pending
            // acknowledgement so the automatic sequence only begins once both are ready (the observer
            // confirms "ready to continue", the Mulligan player confirms "start"). With no Mulligan,
            // nobody is pending and initial selection is enabled immediately.
            pendingMulliganAcknowledgements.put(playerId.toString(), mulliganFlowActive);
            submittedValues.put(playerId.toString(), Boolean.FALSE);
            benchSelections.put(playerId.toString(), List.of());
        }

        setupState.put(MULLIGAN_COUNT_KEY, Map.copyOf(mulliganCounts));
        setupState.put(MULLIGAN_ACK_PENDING_KEY, Map.copyOf(pendingMulliganAcknowledgements));
        setupState.put(
                MULLIGAN_FLOW_KEY,
                Map.copyOf(initialMulliganFlowState(
                        playerIds,
                        mulliganCountByPlayer,
                        currentMulliganPlayerIds,
                        mulliganFlowActive)));
        setupState.put(SELECTION_SUBMITTED_KEY, Map.copyOf(submittedValues));
        setupState.put(ACTIVE_SELECTION_KEY, Map.copyOf(activeSelections));
        setupState.put(BENCH_SELECTION_KEY, Map.copyOf(benchSelections));
        return Map.copyOf(setupState);
    }

    private Map<String, Object> initialMulliganFlowState(
            List<UUID> playerIds,
            Map<UUID, Integer> mulliganCountByPlayer,
            List<UUID> currentMulliganPlayerIds,
            boolean active) {
        Map<String, Object> flowState = new LinkedHashMap<>();
        flowState.put(MULLIGAN_FLOW_ACTIVE_KEY, active);
        flowState.put(MULLIGAN_FLOW_ROUND_NUMBER_KEY, active ? 1 : 0);
        flowState.put(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY, uuidStrings(currentMulliganPlayerIds));
        flowState.put(MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY, resolvedPlayerIds(playerIds, currentMulliganPlayerIds));
        flowState.put(MULLIGAN_FLOW_EXTRA_CARDS_PENDING_KEY, extraCardsPendingByPlayer(playerIds, mulliganCountByPlayer));
        flowState.put(MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY, !active);
        return flowState;
    }

    private Map<String, Object> completedMulliganFlowState(
            List<UUID> playerIds,
            Map<UUID, Integer> mulliganCountByPlayer,
            int roundNumber) {
        Map<String, Object> flowState = new LinkedHashMap<>();
        flowState.put(MULLIGAN_FLOW_ACTIVE_KEY, Boolean.FALSE);
        flowState.put(MULLIGAN_FLOW_ROUND_NUMBER_KEY, roundNumber);
        flowState.put(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY, List.of());
        flowState.put(MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY, uuidStrings(playerIds));
        flowState.put(MULLIGAN_FLOW_EXTRA_CARDS_PENDING_KEY, extraCardsPendingByPlayer(playerIds, mulliganCountByPlayer));
        flowState.put(MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY, Boolean.TRUE);
        return flowState;
    }

    private List<String> resolvedPlayerIds(List<UUID> playerIds, List<UUID> currentMulliganPlayerIds) {
        List<String> resolvedPlayerIds = new ArrayList<>();
        for (UUID playerId : playerIds) {
            if (!currentMulliganPlayerIds.contains(playerId)) {
                resolvedPlayerIds.add(playerId.toString());
            }
        }
        return List.copyOf(resolvedPlayerIds);
    }

    private Map<String, Object> extraCardsPendingByPlayer(List<UUID> playerIds, Map<UUID, Integer> mulliganCountByPlayer) {
        Map<String, Object> extraCardsPending = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            UUID opponentUserId = opponentUserId(playerIds, playerId);
            extraCardsPending.put(playerId.toString(), mulliganCountByPlayer.getOrDefault(opponentUserId, 0));
        }
        return Map.copyOf(extraCardsPending);
    }

    private GameStateDto buildState(
            Game game,
            List<GameParticipant> participants,
            int stateVersion,
            Map<UUID, Integer> pokemonEnteredPlayTurnMap) {
        List<UUID> playerIds = playerIds(participants);
        Map<UUID, Integer> benchCountByPlayer = new LinkedHashMap<>();
        Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer = new LinkedHashMap<>();
        Map<UUID, List<UUID>> cardsInHandByPlayer = new LinkedHashMap<>();
        Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> affordableAttacksByPlayer = new LinkedHashMap<>();
        Map<UUID, CardZone> cardZones = new LinkedHashMap<>();
        Map<UUID, UUID> cardOwners = new LinkedHashMap<>();

        for (UUID playerId : playerIds) {
            benchCountByPlayer.put(playerId, 0);
            activePokemonConditionsByPlayer.put(playerId, List.of());
            cardsInHandByPlayer.put(playerId, new ArrayList<>());
            cardsInHandInstanceIdsByPlayer.put(playerId, new ArrayList<>());
            affordableAttacksByPlayer.put(playerId, Set.of());
        }

        List<GameCardInstance> cardInstances = gameCardInstanceStateService.findByGameId(game.getId());
        for (GameCardInstance cardInstance : cardInstances) {
            putCardReference(cardZones, cardOwners, cardInstance);
            if (CardZone.HAND.equals(cardInstance.getZone())) {
                addCardToHandMap(cardsInHandByPlayer, cardInstance.getOwnerUserId(), cardInstance.getCardId());
                addCardToHandMap(cardsInHandInstanceIdsByPlayer, cardInstance.getOwnerUserId(), cardInstance.getId());
            }
            if (CardZone.BENCH.equals(cardInstance.getZone())) {
                incrementBenchCount(benchCountByPlayer, cardInstance.getOwnerUserId());
            }
        }

        SetupStateView setupStateView = setupStateView(game.getSetupState(), playerIds);

        return GameStateDto.builder()
                .gameId(game.getId())
                .status(game.getStatus())
                .stateVersion(stateVersion)
                .playerIds(playerIds)
                .players(players(
                        playerIds,
                        benchCountByPlayer,
                        activePokemonConditionsByPlayer,
                        cardsInHandByPlayer,
                        cardsInHandInstanceIdsByPlayer,
                        affordableAttacksByPlayer,
                        setupStateView))
                .turn(TurnContextDto.builder()
                        .currentPhase(game.getCurrentPhase())
                        .turnNumber(game.getTurnNumber())
                        .activePlayerId(game.getActivePlayerId())
                        .playerWhoWentFirstId(game.getPlayerWhoWentFirstId())
                        .build())
                .board(BoardStateDto.builder()
                        .enteredPlayTurnByPokemonInPlayId(Map.copyOf(pokemonEnteredPlayTurnMap))
                        .zoneByCardReferenceId(Map.copyOf(cardZones))
                        .ownerByCardReferenceId(Map.copyOf(cardOwners))
                        .build())
                .actions(ActionStateDto.builder()
                        .availableActions(availableActionsFor(game))
                        .processedClientActionIds(Set.of())
                        .build())
                .updatedAt(Instant.now())
                .build();
    }

    private Map<UUID, PlayerStateDto> players(
            List<UUID> playerIds,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            SetupStateView setupStateView) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>();
        Map<UUID, List<UUID>> copiedHandCards = copyUuidListMap(cardsInHandByPlayer);
        Map<UUID, List<UUID>> copiedHandInstances = copyUuidListMap(cardsInHandInstanceIdsByPlayer);

        for (UUID playerId : playerIds) {
            Integer benchCount = benchCountByPlayer.get(playerId);
            int safeBenchCount = 0;
            if (benchCount != null) {
                safeBenchCount = benchCount;
            }

            Integer mulliganCount = setupStateView.mulliganCountByPlayer().get(playerId);
            int safeMulliganCount = 0;
            if (mulliganCount != null) {
                safeMulliganCount = mulliganCount;
            }

            Boolean selectionSubmitted = setupStateView.setupSelectionSubmittedByPlayer().get(playerId);
            boolean safeSelectionSubmitted = false;
            if (selectionSubmitted != null) {
                safeSelectionSubmitted = selectionSubmitted;
            }

            players.put(playerId, PlayerStateDto.builder()
                    .benchPokemonCount(safeBenchCount)
                    .activePokemonConditions(activePokemonConditionsByPlayer.get(playerId))
                    .cardIdsInHand(copiedHandCards.get(playerId))
                    .cardInstanceIdsInHand(copiedHandInstances.get(playerId))
                    .affordableAttackIds(affordableAttacksByPlayer.get(playerId))
                    .mulliganCount(safeMulliganCount)
                    .mulliganNoticePending(setupStateView.pendingMulliganAcknowledgementsByPlayer().getOrDefault(playerId, Boolean.FALSE))
                    .mulliganCurrentPlayer(setupStateView.mulliganCurrentPlayerByPlayer().getOrDefault(playerId, Boolean.FALSE))
                    .mulliganFlowActive(setupStateView.mulliganFlowActive())
                    .mulliganReadyForInitialSelection(setupStateView.mulliganReadyForInitialSelection())
                    .mulliganRoundNumber(setupStateView.mulliganRoundNumber())
                    .initialPokemonSelectionSubmitted(safeSelectionSubmitted)
                    .initialActiveCardInstanceId(setupStateView.setupActiveCardInstanceIdByPlayer().get(playerId))
                    .initialBenchCardInstanceIds(setupStateView.setupBenchCardInstanceIdsByPlayer().get(playerId))
                    .build());
        }

        return Map.copyOf(players);
    }

    private SetupStateView setupStateView(Map<String, Object> setupState, List<UUID> playerIds) {
        Map<UUID, Integer> mulliganCountByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> pendingMulliganAcknowledgementsByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> mulliganCurrentPlayerByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> setupSelectionSubmittedByPlayer = new LinkedHashMap<>();
        Map<UUID, UUID> setupActiveCardInstanceIdByPlayer = new LinkedHashMap<>();
        Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer = new LinkedHashMap<>();
        Map<String, Object> mulliganValues = mutableNestedMap(setupState, MULLIGAN_COUNT_KEY);
        Map<String, Object> pendingMulliganAcknowledgementValues = mutableNestedMap(setupState, MULLIGAN_ACK_PENDING_KEY);
        Map<String, Object> submittedValues = mutableNestedMap(setupState, SELECTION_SUBMITTED_KEY);
        Map<String, Object> activeValues = mutableNestedMap(setupState, ACTIVE_SELECTION_KEY);
        Map<String, Object> benchValues = mutableNestedMap(setupState, BENCH_SELECTION_KEY);
        Map<String, Object> mulliganFlow = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        boolean mulliganFlowActive = booleanValue(mulliganFlow.get(MULLIGAN_FLOW_ACTIVE_KEY));
        boolean mulliganReadyForInitialSelection = booleanValue(mulliganFlow.get(MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY));
        if (!mulliganFlowActive && mulliganFlow.isEmpty()) {
            mulliganReadyForInitialSelection = true;
        }
        int mulliganRoundNumber = integerValue(mulliganFlow.get(MULLIGAN_FLOW_ROUND_NUMBER_KEY), 0);
        // Players still owing their single initial Mulligan approval; mirrors GameStateQueryServiceImpl
        // so the STATE_SYNC emitted by setup actions exposes mulliganCurrentPlayer consistently.
        List<UUID> currentMulliganPlayerIds = currentMulliganPlayerIds(setupState);

        for (UUID playerId : playerIds) {
            String playerKey = playerId.toString();
            mulliganCountByPlayer.put(playerId, integerValue(mulliganValues.get(playerKey), 0));
            pendingMulliganAcknowledgementsByPlayer.put(playerId, booleanValue(pendingMulliganAcknowledgementValues.get(playerKey)));
            mulliganCurrentPlayerByPlayer.put(playerId, currentMulliganPlayerIds.contains(playerId));
            setupSelectionSubmittedByPlayer.put(playerId, booleanValue(submittedValues.get(playerKey)));
            UUID activeCardInstanceId = optionalUuid(activeValues.get(playerKey));
            if (activeCardInstanceId != null) {
                setupActiveCardInstanceIdByPlayer.put(playerId, activeCardInstanceId);
            }
            setupBenchCardInstanceIdsByPlayer.put(playerId, optionalUuidList(benchValues.get(playerKey), BENCH_CARD_INSTANCE_IDS_KEY));
        }

        return new SetupStateView(
                Map.copyOf(mulliganCountByPlayer),
                Map.copyOf(pendingMulliganAcknowledgementsByPlayer),
                Map.copyOf(mulliganCurrentPlayerByPlayer),
                mulliganFlowActive,
                mulliganReadyForInitialSelection,
                mulliganRoundNumber,
                Map.copyOf(setupSelectionSubmittedByPlayer),
                Map.copyOf(setupActiveCardInstanceIdByPlayer),
                copyUuidListMap(setupBenchCardInstanceIdsByPlayer));
    }

    private List<GameActionType> availableActionsFor(Game game) {
        if (GameStatus.SETUP.equals(game.getStatus())) {
            if (isMulliganFlowActive(game.getSetupState()) || hasAnyPendingMulliganAcknowledgement(game.getSetupState())) {
                return List.of(GameActionType.ACK_MULLIGAN_NOTICE, GameActionType.CHOOSE_INITIAL_POKEMON);
            }
            return List.of(GameActionType.CHOOSE_INITIAL_POKEMON);
        }
        if (GameStatus.ACTIVE.equals(game.getStatus()) && TurnPhase.DRAW.equals(game.getCurrentPhase())) {
            return List.of(GameActionType.DRAW_CARD);
        }

        return List.of();
    }

    private void addPrivateStateSyncEvents(
            List<GameEventDto> events,
            UUID gameId,
            int stateVersion,
            GameStateDto state,
            List<UUID> playerIds) {
        for (UUID playerId : playerIds) {
            events.add(gameEventFactory.privateStateSync(gameId, stateVersion, state, playerId));
        }
    }

    private List<Card> expandDeck(Deck deck) {
        List<Card> expandedCards = new ArrayList<>();
        for (DeckCard deckCard : deck.getCards()) {
            for (int copies = 0; copies < deckCard.getQuantity(); copies++) {
                expandedCards.add(deckCard.getCard());
            }
        }
        return expandedCards;
    }

    private boolean containsBasicPokemon(List<Card> cards) {
        for (Card card : cards) {
            if (isBasicPokemon(card)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBasicPokemon(Card card) {
        return card != null && card.isBasicStage();
    }

    private List<Card> firstCards(List<Card> cards, int count) {
        List<Card> selectedCards = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            selectedCards.add(cards.get(index));
        }

        return selectedCards;
    }

    private List<Card> remainingCards(List<Card> cards, int startIndex) {
        List<Card> selectedCards = new ArrayList<>();
        for (int index = startIndex; index < cards.size(); index++) {
            selectedCards.add(cards.get(index));
        }

        return selectedCards;
    }

    private List<String> cardIds(List<Card> cards) {
        List<String> cardIds = new ArrayList<>();
        for (Card card : cards) {
            cardIds.add(card.getId().toString());
        }

        return List.copyOf(cardIds);
    }

    private Map<String, Object> mulliganPayload(
            UUID revealingPlayerId, int mulliganCount, int sequenceIndex, List<Card> revealedCards) {
        return Map.of(
                "revealingPlayerId", revealingPlayerId.toString(),
                "mulliganNumber", mulliganCount,
                "mulliganCount", mulliganCount,
                "sequenceIndex", sequenceIndex,
                "revealedCardIds", cardIds(revealedCards),
                "revealedCards", revealedCards(revealedCards),
                "extraCardsGranted", 1,
                "pendingExtraCardsForViewer", mulliganCount);
    }

    private void addMulliganCheckpointEvents(
            UUID gameId,
            List<UUID> currentMulliganPlayerIds,
            Map<UUID, Integer> mulliganCountByPlayer,
            int stateVersion,
            List<GameEventDto> events) {
        for (UUID playerUserId : currentMulliganPlayerIds) {
            int mulliganCount = mulliganCountByPlayer.getOrDefault(playerUserId, 1);
            // Only the owner is privately notified that a Mulligan is required. The hand is not
            // revealed to the opponent until the owner acknowledges the notice (see resolveMulliganForPlayer).
            events.add(gameEventFactory.privateEvent(
                    gameId,
                    GameEventType.MULLIGAN_REQUIRED,
                    stateVersion,
                    mulliganRequiredPayload(playerUserId, mulliganCount, mulliganRoundNumberFromCount(mulliganCount)),
                    playerUserId));
        }
    }

    private void revealMulliganHandToBothPlayers(
            UUID gameId,
            UUID playerUserId,
            int mulliganCount,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        List<GameCardInstance> handCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                playerUserId,
                CardZone.HAND);
        List<Card> revealedCards = new ArrayList<>();
        for (GameCardInstance cardInstance : handCards) {
            revealedCards.add(cardService.getCardEntityById(cardInstance.getCardId()));
        }
        // PUBLIC: both players receive the reveal of the INVALID hand. The opponent sees it by the Mulligan
        // rule; the owner receives their own cards (no leak), which lets them animate their own "show invalid
        // hand + ¡MULLIGAN!" beat. Only the invalid hand is exposed — never the new hand, deck or prizes.
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.MULLIGAN_HAND_REVEALED,
                stateVersion,
                mulliganPayload(playerUserId, mulliganCount, sequenceIndex, List.copyOf(revealedCards))));
    }

    private Map<String, Object> openingHandsDealtPayload(List<UUID> playerIds) {
        return Map.of(
                "handSize", INITIAL_HAND_SIZE,
                "playerIds", playerIds.stream().map(UUID::toString).toList(),
                "deckShuffled", true,
                "automatic", true);
    }

    private Map<String, Object> mulliganRequiredPayload(UUID playerUserId, int mulliganCount, int roundNumber) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "mulliganNumber", mulliganCount,
                "mulliganCount", mulliganCount,
                "roundNumber", roundNumber,
                "automatic", true);
    }

    private Map<String, Object> mulliganStepPayload(
            UUID playerUserId, int mulliganNumber, int roundNumber, int sequenceIndex) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "mulliganNumber", mulliganNumber,
                "roundNumber", roundNumber,
                "sequenceIndex", sequenceIndex,
                "automatic", true);
    }

    private Map<String, Object> mulliganNewHandDrawnPayload(
            UUID playerUserId, int mulliganNumber, int roundNumber, int sequenceIndex, List<Card> drawnCards) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "mulliganNumber", mulliganNumber,
                "roundNumber", roundNumber,
                "sequenceIndex", sequenceIndex,
                "cardsDrawn", INITIAL_HAND_SIZE,
                "drawnCards", revealedCards(drawnCards),
                "automatic", true);
    }

    private Map<String, Object> mulliganValidationPayload(
            UUID playerUserId,
            int mulliganNumber,
            int roundNumber,
            boolean hasBasicPokemon,
            int sequenceIndex) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "mulliganNumber", mulliganNumber,
                "roundNumber", roundNumber,
                "sequenceIndex", sequenceIndex,
                "hasBasic", hasBasicPokemon,
                "willRepeat", !hasBasicPokemon,
                "automatic", true);
    }

    private Map<String, Object> mulliganExtraCardsPayload(
            UUID playerUserId,
            UUID sourceMulliganPlayerId,
            int extraCards,
            int sequenceIndex) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "sourceMulliganPlayerId", sourceMulliganPlayerId.toString(),
                "cardsGranted", extraCards,
                "sequenceIndex", sequenceIndex,
                "automatic", true);
    }

    private int mulliganRoundNumberFromCount(int mulliganCount) {
        return Math.max(mulliganCount, 1);
    }

    private void addOwnMulliganSummaryEvent(
            UUID gameId,
            UUID playerUserId,
            UUID opponentUserId,
            int mulliganCount,
            int sequenceIndex,
            int stateVersion,
            List<GameEventDto> events) {
        if (mulliganCount <= 0) {
            return;
        }

        events.add(gameEventFactory.privateEvent(
                gameId,
                GameEventType.MULLIGAN_SEQUENCE_COMPLETED,
                stateVersion,
                ownMulliganSummaryPayload(playerUserId, mulliganCount, opponentUserId, sequenceIndex),
                playerUserId));
    }

    private Map<String, Object> ownMulliganSummaryPayload(
            UUID playerUserId, int mulliganCount, UUID opponentUserId, int sequenceIndex) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "mulliganCount", mulliganCount,
                "extraCardsGrantedToOpponent", mulliganCount,
                "opponentPlayerId", opponentUserId.toString(),
                "sequenceIndex", sequenceIndex,
                "automatic", true);
    }

    private Map<String, Object> mulliganNoticeAcknowledgedPayload(UUID playerUserId, int roundNumber) {
        return Map.of(
                "playerId", playerUserId.toString(),
                "acknowledged", true,
                "automatic", true,
                "checkpoint", MULLIGAN_CHECKPOINT_REVEAL,
                "roundNumber", roundNumber);
    }

    private boolean hasAnyMulligan(Map<UUID, Integer> mulliganCountByPlayer) {
        for (Integer mulliganCount : mulliganCountByPlayer.values()) {
            if (mulliganCount != null && mulliganCount > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPendingMulliganAcknowledgement(Map<String, Object> setupState, UUID playerUserId) {
        if (playerUserId == null) {
            return false;
        }

        Map<String, Object> pendingValues = mutableNestedMap(setupState, MULLIGAN_ACK_PENDING_KEY);
        return Boolean.TRUE.equals(booleanValue(pendingValues.get(playerUserId.toString())));
    }

    private boolean hasAnyPendingMulliganAcknowledgement(Map<String, Object> setupState) {
        Map<String, Object> pendingValues = mutableNestedMap(setupState, MULLIGAN_ACK_PENDING_KEY);
        for (Object pendingValue : pendingValues.values()) {
            if (Boolean.TRUE.equals(booleanValue(pendingValue))) {
                return true;
            }
        }

        return false;
    }

    private void acknowledgeMulliganNotice(Map<String, Object> setupState, UUID playerUserId) {
        Map<String, Object> pendingValues = mutableNestedMap(setupState, MULLIGAN_ACK_PENDING_KEY);
        pendingValues.put(playerUserId.toString(), Boolean.FALSE);
        setupState.put(MULLIGAN_ACK_PENDING_KEY, Map.copyOf(pendingValues));
    }

    private boolean isMulliganFlowActive(Map<String, Object> setupState) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        return booleanValue(flowState.get(MULLIGAN_FLOW_ACTIVE_KEY));
    }

    private boolean isReadyForInitialSelection(Map<String, Object> setupState) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        if (flowState.isEmpty()) {
            return true;
        }
        return booleanValue(flowState.get(MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY));
    }

    private int mulliganRoundNumber(Map<String, Object> setupState) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        return integerValue(flowState.get(MULLIGAN_FLOW_ROUND_NUMBER_KEY), 0);
    }

    private List<UUID> currentMulliganPlayerIds(Map<String, Object> setupState) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        return optionalUuidList(flowState.get(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY), MULLIGAN_FLOW_CURRENT_PLAYERS_KEY);
    }

    private Map<UUID, Integer> mulliganCountByPlayer(Map<String, Object> setupState, List<UUID> playerIds) {
        Map<String, Object> mulliganValues = mutableNestedMap(setupState, MULLIGAN_COUNT_KEY);
        Map<UUID, Integer> mulliganCountByPlayer = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            mulliganCountByPlayer.put(playerId, integerValue(mulliganValues.get(playerId.toString()), 0));
        }
        return mulliganCountByPlayer;
    }

    private void putMulliganCounts(Map<String, Object> setupState, Map<UUID, Integer> mulliganCountByPlayer) {
        Map<String, Object> mulliganValues = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : mulliganCountByPlayer.entrySet()) {
            mulliganValues.put(entry.getKey().toString(), entry.getValue());
        }
        setupState.put(MULLIGAN_COUNT_KEY, Map.copyOf(mulliganValues));
    }

    private void markMulliganPlayerResolved(Map<String, Object> setupState, UUID playerUserId) {
        Map<String, Object> flowState = mutableNestedMap(setupState, MULLIGAN_FLOW_KEY);
        List<UUID> resolvedPlayers = optionalUuidList(flowState.get(MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY), MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY);
        List<UUID> updatedResolvedPlayers = new ArrayList<>(resolvedPlayers);
        if (!updatedResolvedPlayers.contains(playerUserId)) {
            updatedResolvedPlayers.add(playerUserId);
        }
        flowState.put(MULLIGAN_FLOW_RESOLVED_PLAYERS_KEY, uuidStrings(updatedResolvedPlayers));
        setupState.put(MULLIGAN_FLOW_KEY, Map.copyOf(flowState));
    }

    private List<MulliganRevealedCardDto> revealedCards(List<Card> cards) {
        List<MulliganRevealedCardDto> revealedCards = new ArrayList<>();
        for (Card card : cards) {
            revealedCards.add(new MulliganRevealedCardDto(
                    card.getId(),
                    card.getName(),
                    card.getExternalId(),
                    card.getSetCode(),
                    card.getNumber(),
                    card.getSupertype(),
                    card.getCategory(),
                    card.getImageSmallUrl(),
                    card.getImageLargeUrl(),
                    card.getHp()));
        }

        return List.copyOf(revealedCards);
    }

    private List<String> uuidStrings(List<UUID> values) {
        List<String> strings = new ArrayList<>();
        for (UUID value : values) {
            strings.add(value.toString());
        }

        return List.copyOf(strings);
    }

    private List<UUID> playerIds(List<GameParticipant> participants) {
        List<UUID> playerIds = new ArrayList<>();
        for (GameParticipant participant : participants) {
            playerIds.add(participant.getUserId());
        }

        return List.copyOf(playerIds);
    }

    private UUID opponentUserId(List<UUID> playerIds, UUID playerUserId) {
        for (UUID candidateUserId : playerIds) {
            if (!candidateUserId.equals(playerUserId)) {
                return candidateUserId;
            }
        }

        throw new InvalidGameActionException("Opponent player could not be resolved");
    }

    private int nextStateVersion(GameActionContext context, Game game) {
        if (context.currentState() != null) {
            return context.currentState().stateVersion() + 1;
        }

        return game.getStateVersion() + 1;
    }

    private UUID requiredUuid(Object value, String payloadKey) {
        UUID uuid = optionalUuid(value);
        if (uuid == null) {
            throw new InvalidGameActionException("Payload field '" + payloadKey + "' is required");
        }

        return uuid;
    }

    private UUID optionalUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuidValue) {
            return uuidValue;
        }

        return UUID.fromString(String.valueOf(value));
    }

    private List<UUID> optionalUuidList(Object value, String payloadKey) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawValues)) {
            throw new InvalidGameActionException("Payload field '" + payloadKey + "' must be a list");
        }

        List<UUID> values = new ArrayList<>();
        for (Object rawValue : rawValues) {
            values.add(requiredUuid(rawValue, payloadKey));
        }

        return List.copyOf(values);
    }

    private Map<String, Object> mutableSetupState(Map<String, Object> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copiedValues.put(entry.getKey(), entry.getValue());
        }

        return copiedValues;
    }

    private Map<String, Object> mutableNestedMap(Map<String, Object> source, String key) {
        if (source == null) {
            return new LinkedHashMap<>();
        }

        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                copiedValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return copiedValues;
    }

    private Integer integerValue(Object value, Integer defaultValue) {
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value == null) {
            return defaultValue;
        }

        return Integer.valueOf(String.valueOf(value));
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return Boolean.FALSE;
        }

        return Boolean.valueOf(String.valueOf(value));
    }

    private void addCardToHandMap(Map<UUID, List<UUID>> cardsInHandByPlayer, UUID ownerUserId, UUID cardId) {
        List<UUID> handCards = cardsInHandByPlayer.get(ownerUserId);
        if (handCards == null) {
            handCards = new ArrayList<>();
            cardsInHandByPlayer.put(ownerUserId, handCards);
        }
        handCards.add(cardId);
    }

    private void putCardReference(
            Map<UUID, CardZone> cardZones,
            Map<UUID, UUID> cardOwners,
            GameCardInstance cardInstance) {
        if (cardInstance == null || cardInstance.getZone() == null) {
            return;
        }

        putReference(cardZones, cardOwners, cardInstance.getId(), cardInstance.getZone(), cardInstance.getOwnerUserId());
        putReference(cardZones, cardOwners, cardInstance.getCardId(), cardInstance.getZone(), cardInstance.getOwnerUserId());
    }

    private void putReference(
            Map<UUID, CardZone> cardZones,
            Map<UUID, UUID> cardOwners,
            UUID referenceId,
            CardZone zone,
            UUID ownerUserId) {
        if (referenceId == null) {
            return;
        }

        cardZones.put(referenceId, zone);
        cardOwners.put(referenceId, ownerUserId);
    }

    private void incrementBenchCount(Map<UUID, Integer> benchCountByPlayer, UUID ownerUserId) {
        Integer currentCount = benchCountByPlayer.get(ownerUserId);
        if (currentCount == null) {
            benchCountByPlayer.put(ownerUserId, 1);
            return;
        }

        benchCountByPlayer.put(ownerUserId, currentCount + 1);
    }

    private Map<UUID, List<UUID>> copyUuidListMap(Map<UUID, List<UUID>> source) {
        Map<UUID, List<UUID>> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<UUID>> entry : source.entrySet()) {
            List<UUID> values = entry.getValue();
            if (values == null) {
                copiedValues.put(entry.getKey(), List.of());
            } else {
                copiedValues.put(entry.getKey(), List.copyOf(values));
            }
        }

        return Map.copyOf(copiedValues);
    }

    private GameCardInstance cardInstance(Game game, UUID ownerUserId, UUID cardId, CardZone zone, Integer zonePosition, boolean faceDown) {
        GameCardInstance instance = new GameCardInstance();
        instance.setGame(game);
        instance.setOwnerUserId(ownerUserId);
        instance.setCardId(cardId);
        instance.setZone(zone);
        instance.setZonePosition(zonePosition);
        instance.setFaceDown(faceDown);
        return instance;
    }

    private PokemonInPlay pokemonInPlay(Game game, UUID ownerUserId, GameCardInstance activeCardInstance, int slotPosition, int enteredPlayTurn) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(game);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCardInstance);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setEnteredPlayTurn(enteredPlayTurn);
        return pokemonInPlay;
    }

    private PokemonEvolutionStack evolutionBase(PokemonInPlay pokemonInPlay, GameCardInstance activeCardInstance) {
        PokemonEvolutionStack evolutionStack = new PokemonEvolutionStack();
        evolutionStack.setPokemonInPlay(pokemonInPlay);
        evolutionStack.setGameCardInstance(activeCardInstance);
        evolutionStack.setStackOrder(0);
        Integer enteredPlayTurn = pokemonInPlay.getEnteredPlayTurn();
        if (enteredPlayTurn == null) {
            evolutionStack.setCreatedAtTurn(0);
        } else {
            evolutionStack.setCreatedAtTurn(enteredPlayTurn);
        }
        return evolutionStack;
    }

    private record PreparedSetup(
            List<Card> handCards,
            List<Card> prizeCards,
            List<Card> deckCards) {
    }

    private record SetupStateView(
            Map<UUID, Integer> mulliganCountByPlayer,
            Map<UUID, Boolean> pendingMulliganAcknowledgementsByPlayer,
            Map<UUID, Boolean> mulliganCurrentPlayerByPlayer,
            boolean mulliganFlowActive,
            boolean mulliganReadyForInitialSelection,
            int mulliganRoundNumber,
            Map<UUID, Boolean> setupSelectionSubmittedByPlayer,
            Map<UUID, UUID> setupActiveCardInstanceIdByPlayer,
            Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer) {
    }
}
