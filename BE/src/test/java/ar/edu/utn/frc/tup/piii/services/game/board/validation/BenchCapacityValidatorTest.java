package ar.edu.utn.frc.tup.piii.services.game.board.validation;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenchCapacityValidatorTest {

    @Mock
    private GameActionContext context;

    private final BenchCapacityValidator validator = new BenchCapacityValidator();

    @Test
    void shouldAllowBasicPokemonWhenBenchHasSpace() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_BASIC_POKEMON,
                state(actorUserId, Map.<UUID, Integer>of(actorUserId, 4)));

        validator.validate(context);
    }

    @Test
    void shouldAllowNonBasicPokemonActionWhenBenchIsFull() {
        UUID actorUserId = UUID.randomUUID();
        configureContextWithoutActor(
                GameActionType.ATTACH_ENERGY,
                state(actorUserId, Map.<UUID, Integer>of(actorUserId, 5)));

        validator.validate(context);
    }

    @Test
    void shouldAllowBasicPokemonWhenBenchCountIsMissing() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_BASIC_POKEMON,
                state(actorUserId, Map.<UUID, Integer>of()));

        validator.validate(context);
    }

    @Test
    void shouldRejectBasicPokemonWhenBenchIsFull() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_BASIC_POKEMON,
                state(actorUserId, Map.<UUID, Integer>of(actorUserId, 5)));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Bench is full. Maximum capacity is 5");
    }

    private ThrowingCallable validationCall() {
        return new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        };
    }

    private void configureContext(UUID actorUserId, GameActionType actionType, GameStateDto state) {
        when(context.actorUserId()).thenReturn(actorUserId);
        when(context.request()).thenReturn(request(actionType));
        when(context.currentState()).thenReturn(state);
    }

    private void configureContextWithoutActor(GameActionType actionType, GameStateDto state) {
        when(context.request()).thenReturn(request(actionType));
        when(context.currentState()).thenReturn(state);
    }

    private GameActionRequestDto request(GameActionType actionType) {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actionType,
                1,
                Map.<String, Object>of());
    }

    private GameStateDto state(UUID actorUserId, Map<UUID, Integer> benchCountByPlayer) {
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                1,
                1,
                actorUserId,
                List.of(actorUserId),
                false,
                false,
                false,
                benchCountByPlayer,
                Map.<UUID, List<SpecialConditionType>>of(),
                List.of(GameActionType.PLAY_BASIC_POKEMON),
                Instant.now());
    }
}
