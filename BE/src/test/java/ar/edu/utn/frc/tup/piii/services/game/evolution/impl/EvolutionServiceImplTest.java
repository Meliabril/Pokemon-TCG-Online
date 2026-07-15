package ar.edu.utn.frc.tup.piii.services.game.evolution.impl;




import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
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
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.evolution.EvolutionRuleService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvolutionServiceImplTest {

    private static final int CURRENT_TURN_NUMBER = 4;
    private static final int PREVIOUS_TURN_NUMBER = 2;
    private static final int NEXT_STACK_ORDER = 1;
    private static final int EVOLUTION_STACK_ZONE_POSITION = 6;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private EvolutionRuleService evolutionRuleService;

    @Mock
    private PassiveAbilityService passiveAbilityService;

    @Mock
    private TurnService turnService;

    private EvolutionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvolutionServiceImpl(
                new GameActionPayloadReaderImpl(),
                pokemonInPlayStateService,
                pokemonEvolutionStackStateService,
                gameCardInstanceStateService,
                cardService,
                specialConditionStateService,
                gameEventFactory,
                evolutionRuleService,
                passiveAbilityService,
                turnService);
    }

    @Test
    void shouldEvolvePokemonAndPreserveDamageCounters() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID baseCardId = UUID.randomUUID();
        UUID evolutionCardId = UUID.randomUUID();
        UUID baseCardInstanceId = UUID.randomUUID();
        UUID evolutionCardInstanceId = UUID.randomUUID();

        GameCardInstance previousTopInstance = cardInstance(baseCardInstanceId, baseCardId, CardZone.ACTIVE, 0);
        GameCardInstance evolutionCardInstance = cardInstance(evolutionCardInstanceId, evolutionCardId, CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemonInPlay(pokemonInPlayId, actorUserId, previousTopInstance, 0, 3);
        PokemonEvolutionStack topStack = stack(targetPokemon, previousTopInstance, 0, PREVIOUS_TURN_NUMBER);
        Card baseCard = card(baseCardId, CardCategory.BASIC_POKEMON, "Base Pokemon", null);
        Card evolutionCard = card(evolutionCardId, CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon");
        GameStateDto currentState = state(gameId, actorUserId, opponentUserId, pokemonInPlayId, evolutionCardId);
        GameActionContext context = context(gameId, actorUserId, pokemonInPlayId, evolutionCardId, currentState);
        GameEventDto event = event(gameId, currentState.stateVersion() + 1);

        configureSuccessfulLookup(
                gameId,
                actorUserId,
                pokemonInPlayId,
                evolutionCardId,
                targetPokemon,
                evolutionCardInstance,
                topStack,
                baseCard,
                evolutionCard,
                event);

        GameActionExecutionResult result = service.evolve(context);

        ArgumentCaptor<PokemonEvolutionStack> savedStack = ArgumentCaptor.forClass(PokemonEvolutionStack.class);
        verify(evolutionRuleService).validateEvolution(evolutionCard, baseCard, topStack, CURRENT_TURN_NUMBER);
        verify(pokemonEvolutionStackStateService).save(savedStack.capture());
        verify(specialConditionStateService).deleteByPokemonInPlayId(pokemonInPlayId);

        PokemonEvolutionStack newStack = savedStack.getValue();
        assertThat(newStack.getPokemonInPlay()).isEqualTo(targetPokemon);
        assertThat(newStack.getGameCardInstance()).isEqualTo(evolutionCardInstance);
        assertThat(newStack.getStackOrder()).isEqualTo(NEXT_STACK_ORDER);
        assertThat(newStack.getCreatedAtTurn()).isEqualTo(CURRENT_TURN_NUMBER);
        assertThat(previousTopInstance.getZone()).isEqualTo(CardZone.EVOLUTION_STACK);
        assertThat(previousTopInstance.getZonePosition()).isEqualTo(EVOLUTION_STACK_ZONE_POSITION);
        assertThat(evolutionCardInstance.getZone()).isEqualTo(CardZone.ACTIVE);
        assertThat(targetPokemon.getActiveCardInstance()).isEqualTo(evolutionCardInstance);
        assertThat(targetPokemon.getDamageCounters()).isEqualTo(3);
        assertThat(targetPokemon.getEnteredPlayTurn()).isEqualTo(CURRENT_TURN_NUMBER);
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(result.gameState()).get(actorUserId)).isEmpty();
        assertThat(result.gameState().board().enteredPlayTurnByPokemonInPlayId().get(pokemonInPlayId)).isEqualTo(CURRENT_TURN_NUMBER);
        assertThat(result.emittedEvents()).containsExactly(event);
    }

    @Test
    void shouldMoveBenchEvolutionToBenchZone() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID baseCardId = UUID.randomUUID();
        UUID evolutionCardId = UUID.randomUUID();
        UUID baseCardInstanceId = UUID.randomUUID();
        UUID evolutionCardInstanceId = UUID.randomUUID();

        GameCardInstance previousTopInstance = cardInstance(baseCardInstanceId, baseCardId, CardZone.BENCH, 1);
        GameCardInstance evolutionCardInstance = cardInstance(evolutionCardInstanceId, evolutionCardId, CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemonInPlay(pokemonInPlayId, actorUserId, previousTopInstance, 1, 1);
        PokemonEvolutionStack topStack = stack(targetPokemon, previousTopInstance, 0, PREVIOUS_TURN_NUMBER);
        Card baseCard = card(baseCardId, CardCategory.BASIC_POKEMON, "Base Pokemon", null);
        Card evolutionCard = card(evolutionCardId, CardCategory.STAGE_1_POKEMON, "Stage One", "Base Pokemon");
        GameStateDto currentState = state(gameId, actorUserId, opponentUserId, pokemonInPlayId, evolutionCardId);
        GameActionContext context = context(gameId, actorUserId, pokemonInPlayId, evolutionCardId, currentState);
        GameEventDto event = event(gameId, currentState.stateVersion() + 1);

        configureSuccessfulLookup(
                gameId,
                actorUserId,
                pokemonInPlayId,
                evolutionCardId,
                targetPokemon,
                evolutionCardInstance,
                topStack,
                baseCard,
                evolutionCard,
                event);

        GameActionExecutionResult result = service.evolve(context);

        assertThat(evolutionCardInstance.getZone()).isEqualTo(CardZone.BENCH);
        assertThat(evolutionCardInstance.getZonePosition()).isEqualTo(1);
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(result.gameState()).get(actorUserId))
                .containsExactly(SpecialConditionType.BURNED);
    }

    @Test
    void shouldRejectEvolutionWhenBaseStackIsMissing() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID baseCardId = UUID.randomUUID();
        UUID evolutionCardId = UUID.randomUUID();
        UUID baseCardInstanceId = UUID.randomUUID();
        UUID evolutionCardInstanceId = UUID.randomUUID();

        GameCardInstance previousTopInstance = cardInstance(baseCardInstanceId, baseCardId, CardZone.ACTIVE, 0);
        GameCardInstance evolutionCardInstance = cardInstance(evolutionCardInstanceId, evolutionCardId, CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemonInPlay(pokemonInPlayId, actorUserId, previousTopInstance, 0, 0);
        GameStateDto currentState = state(gameId, actorUserId, opponentUserId, pokemonInPlayId, evolutionCardId);
        GameActionContext context = context(gameId, actorUserId, pokemonInPlayId, evolutionCardId, currentState);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                gameId,
                actorUserId,
                evolutionCardId,
                CardZone.HAND))
                .thenReturn(Optional.of(evolutionCardInstance));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(pokemonInPlayId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(evolveCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Evolution stack is missing for the target Pokemon");
        verify(pokemonEvolutionStackStateService, never()).save(any(PokemonEvolutionStack.class));
        verify(specialConditionStateService, never()).deleteByPokemonInPlayId(pokemonInPlayId);
    }

    private void configureSuccessfulLookup(
            UUID gameId,
            UUID actorUserId,
            UUID pokemonInPlayId,
            UUID evolutionCardId,
            PokemonInPlay targetPokemon,
            GameCardInstance evolutionCardInstance,
            PokemonEvolutionStack topStack,
            Card baseCard,
            Card evolutionCard,
            GameEventDto event) {
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                gameId,
                actorUserId,
                evolutionCardId,
                CardZone.HAND))
                .thenReturn(Optional.of(evolutionCardInstance));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(pokemonInPlayId))
                .thenReturn(Optional.of(topStack));
        when(cardService.getCardEntityById(evolutionCardId)).thenReturn(evolutionCard);
        when(cardService.getCardEntityById(baseCard.getId())).thenReturn(baseCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.EVOLUTION_STACK))
                .thenReturn(EVOLUTION_STACK_ZONE_POSITION);
        when(pokemonEvolutionStackStateService.nextStackOrder(pokemonInPlayId)).thenReturn(NEXT_STACK_ORDER);
        when(gameEventFactory.publicEvent(any(UUID.class), any(GameEventType.class), anyInt(), anyMap()))
                .thenReturn(event);
    }

    private ThrowingCallable evolveCall(GameActionContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                service.evolve(context);
            }
        };
    }

    private GameActionContext context(
            UUID gameId,
            UUID actorUserId,
            UUID pokemonInPlayId,
            UUID evolutionCardId,
            GameStateDto currentState) {
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.EVOLVE_POKEMON,
                currentState.stateVersion(),
                Map.<String, Object>of(
                        "pokemonInPlayId", pokemonInPlayId,
                        "cardId", evolutionCardId));
        return new GameActionContext(gameId, actorUserId, request, currentState);
    }

    private GameStateDto state(
            UUID gameId,
            UUID actorUserId,
            UUID opponentUserId,
            UUID pokemonInPlayId,
            UUID evolutionCardId) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                CURRENT_TURN_NUMBER,
                7,
                actorUserId,
                List.of(actorUserId, opponentUserId),
                false,
                false,
                false,
                Map.<UUID, Integer>of(),
                Map.of(actorUserId, List.of(SpecialConditionType.BURNED)),
                CURRENT_TURN_NUMBER,
                actorUserId,
                Map.of(pokemonInPlayId, PREVIOUS_TURN_NUMBER),
                Map.of(actorUserId, List.of(evolutionCardId)),
                Map.<UUID, Set<UUID>>of(),
                List.of(GameActionType.EVOLVE_POKEMON),
                Instant.now());
    }

    private PokemonInPlay pokemonInPlay(
            UUID pokemonInPlayId,
            UUID ownerUserId,
            GameCardInstance activeCardInstance,
            int slotPosition,
            int damageCounters) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(pokemonInPlayId);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCardInstance);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setDamageCounters(damageCounters);
        pokemonInPlay.setEnteredPlayTurn(PREVIOUS_TURN_NUMBER);
        return pokemonInPlay;
    }

    private PokemonEvolutionStack stack(
            PokemonInPlay pokemonInPlay,
            GameCardInstance gameCardInstance,
            int stackOrder,
            int createdAtTurn) {
        PokemonEvolutionStack stack = new PokemonEvolutionStack();
        stack.setPokemonInPlay(pokemonInPlay);
        stack.setGameCardInstance(gameCardInstance);
        stack.setStackOrder(stackOrder);
        stack.setCreatedAtTurn(createdAtTurn);
        return stack;
    }

    private GameCardInstance cardInstance(UUID instanceId, UUID cardId, CardZone zone, int zonePosition) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(instanceId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(zonePosition);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private Card card(UUID cardId, CardCategory category, String name, String evolvesFrom) {
        Card card = new Card();
        card.setId(cardId);
        card.setCategory(category);
        card.setName(name);
        card.setEvolvesFrom(evolvesFrom);
        return card;
    }

    private GameEventDto event(UUID gameId, int stateVersion) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.POKEMON_EVOLVED,
                stateVersion,
                false,
                Instant.now(),
                Map.<String, Object>of());
    }
}
