package ar.edu.utn.frc.tup.piii.services.game.retreat.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatLockService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetreatLockValidatorTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private RetreatLockService retreatLockService;

    @Mock
    private GameActionContext context;

    private RetreatLockValidator validator;

    private final UUID gameId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();

    @Test
    void shouldRejectRetreatWhenActivePokemonIsLockedForCurrentTurn() {
        validator = new RetreatLockValidator(pokemonInPlayStateService, retreatLockService);
        PokemonInPlay activePokemon = new PokemonInPlay();
        configureContext(GameActionType.RETREAT, 5);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.of(activePokemon));
        when(retreatLockService.isLocked(activePokemon, 5)).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Active Pokemon cannot retreat this turn");
    }

    @Test
    void shouldAllowRetreatWhenActivePokemonIsNotLocked() {
        validator = new RetreatLockValidator(pokemonInPlayStateService, retreatLockService);
        PokemonInPlay activePokemon = new PokemonInPlay();
        configureContext(GameActionType.RETREAT, 5);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.of(activePokemon));
        when(retreatLockService.isLocked(activePokemon, 5)).thenReturn(false);

        validator.validate(context);
    }

    @Test
    void shouldIgnoreNonRetreatActions() {
        validator = new RetreatLockValidator(pokemonInPlayStateService, retreatLockService);
        lenient().when(context.request()).thenReturn(request(GameActionType.ATTACH_ENERGY));
        lenient().when(context.currentState()).thenReturn(state(5));

        validator.validate(context);

        verify(pokemonInPlayStateService, never()).findActivePokemon(eq(gameId), eq(actorUserId));
    }

    @Test
    void shouldDoNothingWhenActivePokemonIsMissing() {
        validator = new RetreatLockValidator(pokemonInPlayStateService, retreatLockService);
        configureContext(GameActionType.RETREAT, 5);
        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.empty());

        validator.validate(context);
    }

    private void configureContext(GameActionType actionType, int turnNumber) {
        when(context.gameId()).thenReturn(gameId);
        when(context.actorUserId()).thenReturn(actorUserId);
        when(context.request()).thenReturn(request(actionType));
        when(context.currentState()).thenReturn(state(turnNumber));
    }

    private GameActionRequestDto request(GameActionType actionType) {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actionType,
                1,
                Map.<String, Object>of());
    }

    private GameStateDto state(int turnNumber) {
        return GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                turnNumber,
                1,
                actorUserId,
                List.of(GameActionType.RETREAT),
                Instant.now());
    }
}
