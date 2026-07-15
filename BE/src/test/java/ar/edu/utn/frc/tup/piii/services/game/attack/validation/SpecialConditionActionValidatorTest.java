package ar.edu.utn.frc.tup.piii.services.game.attack.validation;




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
class SpecialConditionActionValidatorTest {

    @Mock
    private GameActionContext context;

    private final SpecialConditionActionValidator validator = new SpecialConditionActionValidator();

    @Test
    void shouldAllowAttackWhenActivePokemonHasNoBlockingCondition() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.DECLARE_ATTACK,
                state(actorUserId, List.of(SpecialConditionType.BURNED, SpecialConditionType.POISONED)));

        validator.validate(context);
    }

    @Test
    void shouldAllowRetreatWhenActivePokemonHasNoConditions() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.RETREAT,
                state(actorUserId, List.<SpecialConditionType>of()));

        validator.validate(context);
    }

    @Test
    void shouldAllowNonAttackOrRetreatActionWhenActivePokemonIsAsleep() {
        UUID actorUserId = UUID.randomUUID();
        configureContextWithoutActor(
                GameActionType.ATTACH_ENERGY,
                state(actorUserId, List.of(SpecialConditionType.ASLEEP)));

        validator.validate(context);
    }

    @Test
    void shouldRejectAttackWhenActivePokemonIsAsleep() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.DECLARE_ATTACK,
                state(actorUserId, List.of(SpecialConditionType.ASLEEP)));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Active Pokemon cannot attack or retreat due to a special condition");
    }

    @Test
    void shouldRejectRetreatWhenActivePokemonIsParalyzed() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.RETREAT,
                state(actorUserId, List.of(SpecialConditionType.PARALYZED)));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Active Pokemon cannot attack or retreat due to a special condition");
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

    private GameStateDto state(UUID actorUserId, List<SpecialConditionType> activePokemonConditions) {
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.ATTACK,
                1,
                1,
                actorUserId,
                List.of(actorUserId),
                false,
                false,
                false,
                Map.<UUID, Integer>of(),
                Map.<UUID, List<SpecialConditionType>>of(actorUserId, activePokemonConditions),
                List.of(GameActionType.DECLARE_ATTACK),
                Instant.now());
    }
}
