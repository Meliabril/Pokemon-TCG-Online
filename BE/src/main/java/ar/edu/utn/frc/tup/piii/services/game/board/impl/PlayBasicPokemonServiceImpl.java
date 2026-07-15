package ar.edu.utn.frc.tup.piii.services.game.board.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.board.PlayBasicPokemonService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlayBasicPokemonServiceImpl implements PlayBasicPokemonService {

    private static final String CARD_ID_KEY = "cardId";

    private final GameActionPayloadReader payloadReader;
    private final AvailableActionsFactory availableActionsFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;

    @Override
    public GameActionExecutionResult playBasicPokemon(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID gameId = context.gameId();
        UUID cardId = payloadReader.requiredUuid(context.request().payload(), CARD_ID_KEY);

        GameCardInstance handCard = findHandCard(
                gameId,
                actorUserId,
                cardId,
                "Basic Pokemon card is not available in the player's hand");
        Card card = cardService.getCardEntityById(cardId);
        if (!card.isBasicStage()) {
            throw new InvalidGameActionException("Only Basic Pokemon cards can be played to the bench");
        }

        int nextBenchSlot = nextBenchSlot(gameId, actorUserId);
        int currentTurnNumber = safeCurrentTurnNumber(context.currentState());

        handCard.setZone(CardZone.BENCH);
        handCard.setZonePosition(nextBenchSlot);
        handCard.setFaceDown(false);
        gameCardInstanceStateService.save(handCard);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        PokemonInPlay savedPokemon = savePokemonInPlay(actorUserId, handCard, nextBenchSlot, currentTurnNumber);
        saveBaseEvolutionStack(handCard, savedPokemon, currentTurnNumber);

        int newStateVersion = context.currentState().stateVersion() + 1;
        BoardStateDto newBoard = updatedBoard(
                context.currentState(),
                actorUserId,
                handCard,
                savedPokemon,
                currentTurnNumber);
        ActionStateDto newActions = context.currentState().actions().toBuilder()
                .availableActions(availableActionsFactory.mainPhaseActions(context.currentState().turn().energyAttachedThisTurn()))
                .build();
        GameStateDto newState = context.currentState().toBuilder()
                .stateVersion(newStateVersion)
                .players(updatedPlayers(context.currentState(), actorUserId))
                .board(newBoard)
                .actions(newActions)
                .updatedAt(Instant.now())
                .build();

        List<GameEventDto> events = List.of(gameEventFactory.publicEvent(
                gameId,
                GameEventType.CARD_PLAYED,
                newStateVersion,
                Map.of(
                        "playerId", actorUserId.toString(),
                        "cardId", cardId.toString(),
                        "pokemonInPlayId", savedPokemon.getId().toString(),
                        "slotPosition", nextBenchSlot)));

        return new GameActionExecutionResult(newState, events);
    }

    private PokemonInPlay savePokemonInPlay(
            UUID actorUserId,
            GameCardInstance handCard,
            int nextBenchSlot,
            int currentTurnNumber) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setGame(handCard.getGame());
        pokemonInPlay.setOwnerUserId(actorUserId);
        pokemonInPlay.setActiveCardInstance(handCard);
        pokemonInPlay.setSlotPosition(nextBenchSlot);
        pokemonInPlay.setEnteredPlayTurn(currentTurnNumber);
        return pokemonInPlayStateService.save(pokemonInPlay);
    }

    private void saveBaseEvolutionStack(
            GameCardInstance handCard,
            PokemonInPlay savedPokemon,
            int currentTurnNumber) {
        PokemonEvolutionStack baseStack = new PokemonEvolutionStack();
        baseStack.setPokemonInPlay(savedPokemon);
        baseStack.setGameCardInstance(handCard);
        baseStack.setStackOrder(0);
        baseStack.setCreatedAtTurn(currentTurnNumber);
        pokemonEvolutionStackStateService.save(baseStack);
    }

    private Map<UUID, PlayerStateDto> updatedPlayers(GameStateDto currentState, UUID actorUserId) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>(currentState.players());
        PlayerStateDto playerState = players.get(actorUserId);
        if (playerState == null) {
            playerState = PlayerStateDto.builder().build();
        }

        players.put(actorUserId, playerState.toBuilder()
                .benchPokemonCount(playerState.benchPokemonCount() + 1)
                .build());
        return Map.copyOf(players);
    }

    private BoardStateDto updatedBoard(
            GameStateDto currentState,
            UUID actorUserId,
            GameCardInstance handCard,
            PokemonInPlay savedPokemon,
            int currentTurnNumber) {
        Map<UUID, Integer> enteredPlayTurns = new LinkedHashMap<>(currentState.board().enteredPlayTurnByPokemonInPlayId());
        enteredPlayTurns.put(savedPokemon.getId(), currentTurnNumber);

        Map<UUID, CardZone> zones = new LinkedHashMap<>(currentState.board().zoneByCardReferenceId());
        zones.put(handCard.getId(), CardZone.BENCH);
        zones.put(handCard.getCardId(), CardZone.BENCH);
        zones.put(savedPokemon.getId(), CardZone.BENCH);

        Map<UUID, UUID> owners = new LinkedHashMap<>(currentState.board().ownerByCardReferenceId());
        owners.put(handCard.getId(), actorUserId);
        owners.put(handCard.getCardId(), actorUserId);
        owners.put(savedPokemon.getId(), actorUserId);

        return currentState.board().toBuilder()
                .enteredPlayTurnByPokemonInPlayId(Map.copyOf(enteredPlayTurns))
                .zoneByCardReferenceId(Map.copyOf(zones))
                .ownerByCardReferenceId(Map.copyOf(owners))
                .build();
    }

    private int nextBenchSlot(UUID gameId, UUID actorUserId) {
        List<PokemonInPlay> benchPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, actorUserId);
        boolean[] usedSlots = new boolean[6];
        for (PokemonInPlay pokemonInPlay : benchPokemon) {
            Integer slot = pokemonInPlay.getSlotPosition();
            if (slot != null && slot >= 1 && slot <= 5) {
                usedSlots[slot] = true;
            }
        }
        for (int slot = 1; slot <= 5; slot++) {
            if (!usedSlots[slot]) {
                return slot;
            }
        }
        throw new InvalidGameActionException("Bench is full. Maximum capacity is 5");
    }

    private int safeCurrentTurnNumber(GameStateDto currentState) {
        return currentState.turn().turnNumber();
    }

    private GameCardInstance findHandCard(UUID gameId, UUID actorUserId, UUID cardId, String errorMessage) {
        Optional<GameCardInstance> handCard = gameCardInstanceStateService
                .findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(gameId, actorUserId, cardId, CardZone.HAND);
        if (handCard.isEmpty()) {
            throw new InvalidGameActionException(errorMessage);
        }

        return handCard.get();
    }
}
