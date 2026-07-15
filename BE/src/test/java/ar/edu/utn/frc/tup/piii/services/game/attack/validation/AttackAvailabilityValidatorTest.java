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
class AttackAvailabilityValidatorTest {

    @Mock
    private GameActionContext context;

    private final AttackAvailabilityValidator validator = new AttackAvailabilityValidator();

    @Test
    void shouldRejectAttackWhenFirstPlayerAttacksDuringInitialTurnWindow() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.DECLARE_ATTACK,
                state(actorUserId, 1, actorUserId, GameActionType.DECLARE_ATTACK));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("The player who goes first cannot attack on their first turn");
    }

    @Test
    void shouldAllowAttackWhenFirstPlayerIsPastInitialTurnWindow() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.DECLARE_ATTACK,
                state(actorUserId, 3, actorUserId, GameActionType.DECLARE_ATTACK));

        validator.validate(context);
    }

    @Test
    void shouldAllowAttackWhenActorDidNotGoFirst() {
        UUID actorUserId = UUID.randomUUID();
        UUID playerWhoWentFirstId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.DECLARE_ATTACK,
                state(actorUserId, 2, playerWhoWentFirstId, GameActionType.DECLARE_ATTACK));

        validator.validate(context);
    }

    @Test
    void shouldAllowNonAttackActionDuringInitialTurnWindow() {
        UUID actorUserId = UUID.randomUUID();
        configureContextWithoutActor(
                GameActionType.END_TURN,
                state(actorUserId, 1, actorUserId, GameActionType.END_TURN));

        validator.validate(context);
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

    private GameStateDto state(
            UUID actorUserId,
            Integer currentTurnNumber,
            UUID playerWhoWentFirstId,
            GameActionType availableAction) {
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.ATTACK,
                currentTurnNumber.intValue(),
                1,
                actorUserId,
                List.of(actorUserId),
                false,
                false,
                false,
                Map.<UUID, Integer>of(),
                Map.<UUID, List<SpecialConditionType>>of(),
                currentTurnNumber,
                playerWhoWentFirstId,
                Map.<UUID, Integer>of(),
                List.of(availableAction),
                Instant.now());
    }
}
