package ar.edu.utn.frc.tup.piii.services.game.engine.impl;




import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityService;
import ar.edu.utn.frc.tup.piii.services.game.ability.impl.UseAbilityActionHandler;
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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameActionExecutorImpl;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameActionExecutorImplTest {

    @Test
    void shouldDelegateToHandlerMatchingActionType() {
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(sampleState(), List.of(sampleEvent()));
        GameActionHandler drawHandler = handler(GameActionType.DRAW_CARD, expectedResult);
        GameActionHandler endTurnHandler = handler(GameActionType.END_TURN, new GameActionExecutionResult(sampleState(), List.of()));
        GameActionExecutor executor = new GameActionExecutorImpl(List.of(drawHandler, endTurnHandler));

        GameActionExecutionResult result = executor.execute(context(GameActionType.DRAW_CARD));

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectUnsupportedActionTypes() {
        GameActionExecutor executor = new GameActionExecutorImpl(List.of(handler(
                GameActionType.END_TURN,
                new GameActionExecutionResult(sampleState(), List.of()))));

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                executor.execute(context(GameActionType.ATTACH_ENERGY));
            }
        })
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("ATTACH_ENERGY");
    }

    @Test
    void shouldRejectDuplicateHandlersForSameActionType() {
        GameActionHandler firstHandler = handler(GameActionType.END_TURN, new GameActionExecutionResult(sampleState(), List.of()));
        GameActionHandler duplicateHandler = handler(GameActionType.END_TURN, new GameActionExecutionResult(sampleState(), List.of()));

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                new GameActionExecutorImpl(List.of(firstHandler, duplicateHandler));
            }
        })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("END_TURN");
    }

    @Test
    void shouldRejectNullActionTypeAtExecutionTime() {
        GameActionExecutor executor = new GameActionExecutorImpl(List.of(handler(
                GameActionType.END_TURN,
                new GameActionExecutionResult(sampleState(), List.of()))));

        GameActionContext context = new GameActionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new GameActionRequestDto(UUID.randomUUID(), UUID.randomUUID(), null, 0, Map.of()),
                sampleState());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                executor.execute(context);
            }
        })
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Action type is required");
    }

    @Test
    void shouldRecognizeUseAbilityAndDelegateToAbilityService() {
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(sampleState(), List.of());
        AbilityService abilityService = new AbilityService() {
            @Override
            public GameActionExecutionResult useAbility(GameActionContext context) {
                return expectedResult;
            }
        };
        GameActionExecutor executor = new GameActionExecutorImpl(List.of(new UseAbilityActionHandler(abilityService)));

        GameActionExecutionResult result = executor.execute(context(GameActionType.USE_ABILITY));

        assertThat(result).isSameAs(expectedResult);
    }

    private GameActionHandler handler(GameActionType actionType, GameActionExecutionResult result) {
        return new GameActionHandler() {
            @Override
            public GameActionType supportedAction() {
                return actionType;
            }

            @Override
            public GameActionExecutionResult execute(GameActionContext context) {
                return result;
            }
        };
    }

    private GameActionContext context(GameActionType actionType) {
        return new GameActionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new GameActionRequestDto(UUID.randomUUID(), UUID.randomUUID(), actionType, 4, Map.of()),
                sampleState());
    }

    private GameStateDto sampleState() {
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                4,
                UUID.randomUUID(),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-24T12:00:00Z"));
    }

    private GameEventDto sampleEvent() {
        return new GameEventDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                GameEventType.TURN_STARTED,
                4,
                false,
                Instant.parse("2026-05-24T12:00:01Z"),
                Map.of("turn", 2));
    }
}
