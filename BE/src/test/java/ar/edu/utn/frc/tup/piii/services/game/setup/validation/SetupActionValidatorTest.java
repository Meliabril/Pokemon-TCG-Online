package ar.edu.utn.frc.tup.piii.services.game.setup.validation;




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
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetupActionValidatorTest {

    private final SetupActionValidator validator = new SetupActionValidator();

    @Test
    void shouldAllowStartGameDuringWaitingStatus() {
        assertThatCode(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.START_GAME, GameStatus.WAITING));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectStartGameOutsideWaitingStatus() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.START_GAME, GameStatus.SETUP));
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Only waiting games can be started");
    }

    @Test
    void shouldAllowInitialPokemonSelectionDuringSetupStatus() {
        assertThatCode(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.CHOOSE_INITIAL_POKEMON, GameStatus.SETUP));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowMulliganNoticeAcknowledgementDuringSetupStatus() {
        assertThatCode(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.ACK_MULLIGAN_NOTICE, GameStatus.SETUP));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMulliganNoticeAcknowledgementOutsideSetupStatus() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.ACK_MULLIGAN_NOTICE, GameStatus.ACTIVE));
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Mulligan notices can only be acknowledged during setup");
    }

    @Test
    void shouldRejectInitialPokemonSelectionOutsideSetupStatus() {
        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context(GameActionType.CHOOSE_INITIAL_POKEMON, GameStatus.ACTIVE));
            }
        }).isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Initial Pokemon can only be selected during setup");
    }

    private GameActionContext context(GameActionType actionType, GameStatus status) {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                status,
                null,
                0,
                1,
                null,
                List.of(actorUserId),
                List.of(actionType),
                Instant.parse("2026-05-24T20:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                actionType,
                1,
                Map.of());
        return new GameActionContext(gameId, actorUserId, request, state);
    }
}
