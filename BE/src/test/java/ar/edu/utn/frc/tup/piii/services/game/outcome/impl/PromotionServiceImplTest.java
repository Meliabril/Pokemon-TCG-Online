package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private GameActionPayloadReader payloadReader;
    @Mock
    private GameLookupService gameLookupService;
    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private SpecialConditionStateService specialConditionStateService;
    @Mock
    private GameStateQueryService gameStateQueryService;
    @Mock
    private GameEventFactory gameEventFactory;
    @Mock
    private AbilityUsageTracker abilityUsageTracker;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private UUID gameId;
    private UUID actorUserId;
    private UUID pokemonInPlayId;
    private GameActionContext context;
    private Game game;
    private GameStateDto currentState;
    private PokemonInPlay benchPokemon;
    private GameCardInstance benchCard;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        pokemonInPlayId = UUID.randomUUID();

        game = new Game();
        game.setId(gameId);

        benchCard = new GameCardInstance();
        benchCard.setZone(CardZone.BENCH);
        benchCard.setZonePosition(1);

        benchPokemon = new PokemonInPlay();
        benchPokemon.setId(pokemonInPlayId);
        benchPokemon.setSlotPosition(1);
        benchPokemon.setActiveCardInstance(benchCard);

        currentState = GameStateDto.builder()
                .stateVersion(5)
                .resolution(ResolutionStateDto.builder()
                        .resolutionType(ResolutionStateDto.PROMOTION_REQUIRED)
                        .playerToPromoteId(actorUserId)
                        .nextActivePlayerId(UUID.randomUUID())
                        .nextTurnNumber(2)
                        .build())
                .build();

        context = new GameActionContext(
                gameId,
                actorUserId,
                mock(ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto.class),
                currentState
        );
    }

    @Test
    void promoteBenchPokemon_success() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(1);
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(benchPokemon));

        PokemonInPlay otherBenchPokemon = new PokemonInPlay();
        otherBenchPokemon.setId(UUID.randomUUID());
        otherBenchPokemon.setSlotPosition(2);
        GameCardInstance otherBenchCard = new GameCardInstance();
        otherBenchCard.setZone(CardZone.BENCH);
        otherBenchCard.setZonePosition(2);
        otherBenchPokemon.setActiveCardInstance(otherBenchCard);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, actorUserId))
                .thenReturn(List.of(benchPokemon, otherBenchPokemon));

        GameStateDto expectedNewState = GameStateDto.builder().stateVersion(6).build();
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(expectedNewState);

        GameEventDto event1 = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.POKEMON_PROMOTED, 6, false, java.time.Instant.now(), Map.of());
        GameEventDto event2 = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.PHASE_CHANGED, 6, false, java.time.Instant.now(), Map.of());
        GameEventDto event3 = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.TURN_STARTED, 6, false, java.time.Instant.now(), Map.of());

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.POKEMON_PROMOTED), eq(6), any())).thenReturn(event1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.PHASE_CHANGED), eq(6), any())).thenReturn(event2);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TURN_STARTED), eq(6), any())).thenReturn(event3);

        GameActionExecutionResult result = promotionService.promoteBenchPokemon(context);

        assertThat(result.gameState().stateVersion()).isEqualTo(6);
        assertThat(result.emittedEvents()).containsExactly(event1, event2, event3);

        assertThat(benchPokemon.getSlotPosition()).isEqualTo(0);
        assertThat(benchCard.getZone()).isEqualTo(CardZone.ACTIVE);
        assertThat(benchCard.getZonePosition()).isEqualTo(0);
        assertThat(benchCard.getFaceDown()).isFalse();

        verify(gameCardInstanceStateService).save(benchCard);
        verify(pokemonInPlayStateService).save(benchPokemon);
        verify(specialConditionStateService).deleteByPokemonInPlayId(pokemonInPlayId);

        assertThat(otherBenchPokemon.getSlotPosition()).isEqualTo(1);
        assertThat(otherBenchCard.getZone()).isEqualTo(CardZone.BENCH);
        assertThat(otherBenchCard.getZonePosition()).isEqualTo(1);
        verify(gameCardInstanceStateService).save(otherBenchCard);
        verify(pokemonInPlayStateService).save(otherBenchPokemon);

        assertThat(game.getActivePlayerId()).isEqualTo(currentState.resolution().nextActivePlayerId());
        assertThat(game.getTurnNumber()).isEqualTo(currentState.resolution().nextTurnNumber());
        assertThat(game.getCurrentPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(game.getResolutionState()).isEmpty();
    }

    @Test
    void promoteBenchPokemon_failsWhenNoPromotionRequired() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);

        context = new GameActionContext(
                gameId,
                actorUserId,
                context.request(),
                GameStateDto.builder()
                        .resolution(ResolutionStateDto.builder().build())
                        .build()
        );

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("No pending promotion is available");
    }

    @Test
    void promoteBenchPokemon_failsWhenIncorrectPlayer() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);

        context = new GameActionContext(
                gameId,
                UUID.randomUUID(), // Different user
                context.request(),
                currentState
        );

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("No pending promotion is available");
    }

    @Test
    void promoteBenchPokemon_failsWhenAlreadyHasActivePokemon() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.of(new PokemonInPlay()));

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Player already has an active Pokemon");
    }

    @Test
    void promoteBenchPokemon_failsWhenNoBenchPokemon() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(0);

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Player has no Pokemon on the Bench");
    }

    @Test
    void promoteBenchPokemon_failsWhenPokemonNotInBench() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(1);
        
        benchPokemon.setSlotPosition(0); // Not in bench
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(benchPokemon));

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Promotion target must be on the Bench");
    }

    @Test
    void promoteBenchPokemon_failsWhenPokemonNotFound() {
        when(payloadReader.requiredUuid(context.request().payload(), "pokemonInPlayId")).thenReturn(pokemonInPlayId);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.empty());
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(1);
        
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(pokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> promotionService.promoteBenchPokemon(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Promotion target was not found");
    }
}
