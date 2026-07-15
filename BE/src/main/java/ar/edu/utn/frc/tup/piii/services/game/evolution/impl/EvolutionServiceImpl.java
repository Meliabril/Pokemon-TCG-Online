package ar.edu.utn.frc.tup.piii.services.game.evolution.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.evolution.EvolutionService;
import ar.edu.utn.frc.tup.piii.services.game.evolution.EvolutionRuleService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvolutionServiceImpl implements EvolutionService {

    private static final String CARD_ID_KEY = "cardId";
    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";

    private final GameActionPayloadReader payloadReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final SpecialConditionStateService specialConditionStateService;
    private final GameEventFactory gameEventFactory;
    private final EvolutionRuleService evolutionRuleService;
    private final PassiveAbilityService passiveAbilityService;
    private final TurnService turnService;

    public EvolutionServiceImpl(
            GameActionPayloadReader payloadReader,
            PokemonInPlayStateService pokemonInPlayStateService,
            PokemonEvolutionStackStateService pokemonEvolutionStackStateService,
            GameCardInstanceStateService gameCardInstanceStateService,
            CardService cardService,
            SpecialConditionStateService specialConditionStateService,
            GameEventFactory gameEventFactory,
            EvolutionRuleService evolutionRuleService,
            PassiveAbilityService passiveAbilityService,
            @Lazy TurnService turnService) {
        this.payloadReader = payloadReader;
        this.pokemonInPlayStateService = pokemonInPlayStateService;
        this.pokemonEvolutionStackStateService = pokemonEvolutionStackStateService;
        this.gameCardInstanceStateService = gameCardInstanceStateService;
        this.cardService = cardService;
        this.specialConditionStateService = specialConditionStateService;
        this.gameEventFactory = gameEventFactory;
        this.evolutionRuleService = evolutionRuleService;
        this.passiveAbilityService = passiveAbilityService;
        this.turnService = turnService;
    }

    @Override
    public GameActionExecutionResult evolve(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID gameId = context.gameId();
        UUID pokemonInPlayId = payloadReader.requiredUuid(context.request().payload(), POKEMON_IN_PLAY_ID_KEY);
        UUID cardId = payloadReader.requiredUuid(context.request().payload(), CARD_ID_KEY);

        Optional<PokemonInPlay> targetPokemonResult =
                pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId);
        if (targetPokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Target Pokemon was not found for the acting player");
        }
        PokemonInPlay targetPokemon = targetPokemonResult.get();

        Optional<GameCardInstance> evolutionCardInstanceResult =
                gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                        gameId,
                        actorUserId,
                        cardId,
                        CardZone.HAND);
        if (evolutionCardInstanceResult.isEmpty()) {
            throw new InvalidGameActionException("Evolution card is not available in the player's hand");
        }
        GameCardInstance evolutionCardInstance = evolutionCardInstanceResult.get();

        PokemonEvolutionStack currentTopStack = findTopStack(targetPokemon);
        Card evolutionCard = cardService.getCardEntityById(cardId);
        Card currentTopCard = cardService.getCardEntityById(currentTopStack.getGameCardInstance().getCardId());
        int currentTurnNumber = currentTurnNumber(context.currentState());

        evolutionRuleService.validateEvolution(evolutionCard, currentTopCard, currentTopStack, currentTurnNumber);

        GameCardInstance previousTop = currentTopStack.getGameCardInstance();
        int previousTopStackPosition = gameCardInstanceStateService.nextZonePosition(
                gameId,
                actorUserId,
                CardZone.EVOLUTION_STACK);
        previousTop.setZone(CardZone.EVOLUTION_STACK);
        previousTop.setZonePosition(previousTopStackPosition);
        gameCardInstanceStateService.save(previousTop);
        gameCardInstanceStateService.flush();

        CardZone targetZone = targetZoneFor(targetPokemon);
        evolutionCardInstance.setZone(targetZone);
        evolutionCardInstance.setZonePosition(targetPokemon.getSlotPosition());
        evolutionCardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(evolutionCardInstance);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        targetPokemon.setActiveCardInstance(evolutionCardInstance);
        targetPokemon.setEnteredPlayTurn(currentTurnNumber);
        pokemonInPlayStateService.save(targetPokemon);

        int nextStackOrder = pokemonEvolutionStackStateService.nextStackOrder(targetPokemon.getId());

        PokemonEvolutionStack evolvedStack = new PokemonEvolutionStack();
        evolvedStack.setPokemonInPlay(targetPokemon);
        evolvedStack.setGameCardInstance(evolutionCardInstance);
        evolvedStack.setStackOrder(nextStackOrder);
        evolvedStack.setCreatedAtTurn(currentTurnNumber);
        pokemonEvolutionStackStateService.save(evolvedStack);

        specialConditionStateService.deleteByPokemonInPlayId(targetPokemon.getId());

        int newStateVersion = context.currentState().stateVersion() + 1;
        GameStateDto newState = context.currentState().toBuilder()
                .stateVersion(newStateVersion)
                .players(playersAfterEvolution(
                        context.currentState(),
                        actorUserId,
                        targetPokemon.getSlotPosition() == 0))
                .board(boardAfterEvolution(
                        context.currentState(),
                        actorUserId,
                        previousTop,
                        evolutionCardInstance,
                        targetPokemon.getId(),
                        targetZoneFor(targetPokemon),
                        currentTurnNumber))
                .updatedAt(Instant.now())
                .build();

        List<GameEventDto> events = new java.util.ArrayList<>(List.of(gameEventFactory.publicEvent(
                gameId,
                GameEventType.POKEMON_EVOLVED,
                newStateVersion,
                Map.of(
                        "playerId", actorUserId.toString(),
                        "pokemonInPlayId", pokemonInPlayId.toString(),
                        "cardId", cardId.toString(),
                        "previousCardId", previousTop.getCardId().toString()))));
        events.addAll(passiveAbilityService.recalculateSweetVeil(targetPokemon.getGame(), actorUserId, newStateVersion));

        if (CardCategory.MEGA_POKEMON.equals(evolutionCard.getCategory())) {
            GameActionContext endTurnContext = new GameActionContext(
                    context.gameId(),
                    context.actorUserId(),
                    context.request(),
                    newState);
            GameActionExecutionResult endTurnResult = turnService.endTurn(endTurnContext);
            List<GameEventDto> allEvents = new java.util.ArrayList<>(events);
            allEvents.addAll(endTurnResult.emittedEvents());
            return new GameActionExecutionResult(endTurnResult.gameState(), List.copyOf(allEvents));
        }

        return new GameActionExecutionResult(newState, List.copyOf(events));
    }

    private PokemonEvolutionStack findTopStack(PokemonInPlay targetPokemon) {
        Optional<PokemonEvolutionStack> currentTopStack =
                pokemonEvolutionStackStateService.findTopByPokemonInPlayId(targetPokemon.getId());
        if (currentTopStack.isEmpty()) {
            throw new InvalidGameActionException("Evolution stack is missing for the target Pokemon");
        }

        return currentTopStack.get();
    }

    private int currentTurnNumber(GameStateDto currentState) {
        return currentState.turn().turnNumber();
    }

    private CardZone targetZoneFor(PokemonInPlay targetPokemon) {
        if (targetPokemon.getSlotPosition() != null && targetPokemon.getSlotPosition() == 0) {
            return CardZone.ACTIVE;
        }

        return CardZone.BENCH;
    }

    private Map<UUID, PlayerStateDto> playersAfterEvolution(
            GameStateDto currentState,
            UUID actorUserId,
            boolean activePokemonWasEvolved) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>(currentState.players());
        if (!activePokemonWasEvolved) {
            return Map.copyOf(players);
        }

        PlayerStateDto playerState = players.get(actorUserId);
        if (playerState == null) {
            playerState = PlayerStateDto.builder().build();
        }

        players.put(actorUserId, playerState.toBuilder()
                .activePokemonConditions(List.of())
                .build());
        return Map.copyOf(players);
    }

    private BoardStateDto boardAfterEvolution(
            GameStateDto currentState,
            UUID actorUserId,
            GameCardInstance previousTop,
            GameCardInstance evolutionCardInstance,
            UUID pokemonInPlayId,
            CardZone targetZone,
            int currentTurnNumber) {
        Map<UUID, Integer> enteredPlayTurns =
                new LinkedHashMap<>(currentState.board().enteredPlayTurnByPokemonInPlayId());
        enteredPlayTurns.put(pokemonInPlayId, currentTurnNumber);

        Map<UUID, CardZone> zones = new LinkedHashMap<>(currentState.board().zoneByCardReferenceId());
        putCardZone(zones, previousTop, CardZone.EVOLUTION_STACK);
        putCardZone(zones, evolutionCardInstance, targetZone);
        zones.put(pokemonInPlayId, targetZone);

        Map<UUID, UUID> owners = new LinkedHashMap<>(currentState.board().ownerByCardReferenceId());
        putCardOwner(owners, previousTop, actorUserId);
        putCardOwner(owners, evolutionCardInstance, actorUserId);
        owners.put(pokemonInPlayId, actorUserId);

        return currentState.board().toBuilder()
                .enteredPlayTurnByPokemonInPlayId(Map.copyOf(enteredPlayTurns))
                .zoneByCardReferenceId(Map.copyOf(zones))
                .ownerByCardReferenceId(Map.copyOf(owners))
                .build();
    }

    private void putCardZone(Map<UUID, CardZone> updatedZones, GameCardInstance cardInstance, CardZone zone) {
        if (cardInstance == null) {
            return;
        }

        updatedZones.put(cardInstance.getId(), zone);
        updatedZones.put(cardInstance.getCardId(), zone);
    }

    private void putCardOwner(Map<UUID, UUID> owners, GameCardInstance cardInstance, UUID actorUserId) {
        if (cardInstance == null) {
            return;
        }

        owners.put(cardInstance.getId(), actorUserId);
        owners.put(cardInstance.getCardId(), actorUserId);
    }

}
