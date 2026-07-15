package ar.edu.utn.frc.tup.piii.services.game.engine.validation;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RuleValidatorImplTest {

    @Test
    void shouldDelegateValidationToAllActionValidators() {
        ActionValidator firstValidator = mock(ActionValidator.class);
        ActionValidator secondValidator = mock(ActionValidator.class);
        GameActionContext context = validContext(GameStatus.ACTIVE);
        RuleValidatorImpl ruleValidator = new RuleValidatorImpl(List.of(firstValidator, secondValidator));

        ruleValidator.validate(context);

        InOrder inOrder = inOrder(firstValidator, secondValidator);
        inOrder.verify(firstValidator).validate(context);
        inOrder.verify(secondValidator).validate(context);
    }

    @Test
    void shouldPropagateValidatorExceptionAndStopValidation() {
        ActionValidator failingValidator = mock(ActionValidator.class);
        ActionValidator skippedValidator = mock(ActionValidator.class);
        GameActionContext context = validContext(GameStatus.ACTIVE);
        InvalidGameActionException exception = new InvalidGameActionException("Validation failed.");
        RuleValidatorImpl ruleValidator = new RuleValidatorImpl(List.of(failingValidator, skippedValidator));
        doThrow(exception).when(failingValidator).validate(context);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                ruleValidator.validate(context);
            }
        }).isSameAs(exception);
        verify(skippedValidator, never()).validate(context);
    }

    @Test
    void shouldRejectMissingGameState() {
        GameExistenceValidator validator = new GameExistenceValidator();
        GameActionContext context = new GameActionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                validRequest(),
                null);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Game state is required");
    }

    @Test
    void shouldRejectFinishedGame() {
        GameStatusValidator validator = new GameStatusValidator();
        GameActionContext context = validContext(GameStatus.FINISHED);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("no longer playable");
    }

    @Test
    void shouldRejectCancelledGame() {
        GameStatusValidator validator = new GameStatusValidator();
        GameActionContext context = validContext(GameStatus.CANCELLED);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("no longer playable");
    }

    @Test
    void shouldAllowActiveGame() {
        GameStatusValidator validator = new GameStatusValidator();
        GameActionContext context = validContext(GameStatus.ACTIVE);

        validator.validate(context);
    }

    private GameActionContext validContext(GameStatus status) {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        GameStateDto currentState = GameStateTestFactory.state(
                gameId,
                status,
                TurnPhase.MAIN,
                1,
                1,
                actorUserId,
                List.of(GameActionType.DRAW_CARD),
                Instant.now());

        return new GameActionContext(
                gameId,
                actorUserId,
                validRequest(),
                currentState);
    }

    private GameActionRequestDto validRequest() {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameActionType.DRAW_CARD,
                1,
                Map.<String, Object>of());
    }
}
