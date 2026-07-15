package ar.edu.utn.frc.tup.piii.services.game.turn.validation;




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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhaseValidatorTest {

    private final PhaseValidator validator = new PhaseValidator();

    @Test
    void shouldAllowDrawCardDuringDrawPhase() {
        GameActionContext context = context(GameActionType.DRAW_CARD, TurnPhase.DRAW);

        validator.validate(context);
    }

    @Test
    void shouldAllowMainPhaseActionsDuringMainPhase() {
        validator.validate(context(GameActionType.PLAY_BASIC_POKEMON, TurnPhase.MAIN));
        validator.validate(context(GameActionType.EVOLVE_POKEMON, TurnPhase.MAIN));
        validator.validate(context(GameActionType.ATTACH_ENERGY, TurnPhase.MAIN));
        validator.validate(context(GameActionType.PLAY_TRAINER, TurnPhase.MAIN));
        validator.validate(context(GameActionType.RETREAT, TurnPhase.MAIN));
        validator.validate(context(GameActionType.DECLARE_ATTACK, TurnPhase.MAIN));
        validator.validate(context(GameActionType.END_TURN, TurnPhase.MAIN));
    }

    @Test
    void shouldAllowAttackPhaseActionsDuringAttackPhase() {
        validator.validate(context(GameActionType.DECLARE_ATTACK, TurnPhase.ATTACK));
        validator.validate(context(GameActionType.SELECT_TARGET, TurnPhase.ATTACK));
        validator.validate(context(GameActionType.END_TURN, TurnPhase.ATTACK));
    }

    @Test
    void shouldAllowBetweenTurnsActionsDuringBetweenTurnsPhase() {
        validator.validate(context(GameActionType.PROMOTE_BENCH_POKEMON, TurnPhase.BETWEEN_TURNS));
        validator.validate(context(GameActionType.TAKE_PRIZE_CARD, TurnPhase.BETWEEN_TURNS));
    }

    @Test
    void shouldAllowPhaseIndependentActions() {
        validator.validate(context(GameActionType.CREATE_GAME, TurnPhase.MAIN));
        validator.validate(context(GameActionType.JOIN_GAME, TurnPhase.MAIN));
        validator.validate(context(GameActionType.START_GAME, TurnPhase.MAIN));
        validator.validate(context(GameActionType.PAUSE_GAME, TurnPhase.MAIN));
        validator.validate(context(GameActionType.RESUME_GAME, TurnPhase.MAIN));
        validator.validate(context(GameActionType.CONCEDE, TurnPhase.MAIN));
    }

    @Test
    void shouldRejectDrawCardOutsideDrawPhase() {
        GameActionContext context = context(GameActionType.DRAW_CARD, TurnPhase.MAIN);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not valid during the current turn phase");
    }

    @Test
    void shouldRejectMainPhaseActionOutsideMainPhase() {
        GameActionContext context = context(GameActionType.ATTACH_ENERGY, TurnPhase.DRAW);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not valid during the current turn phase");
    }

    @Test
    void shouldRejectAttackActionOutsideMainOrAttackPhase() {
        GameActionContext context = context(GameActionType.DECLARE_ATTACK, TurnPhase.DRAW);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not valid during the current turn phase");
    }

    @Test
    void shouldRejectEndTurnOutsideMainOrAttackPhase() {
        GameActionContext context = context(GameActionType.END_TURN, TurnPhase.DRAW);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not valid during the current turn phase");
    }

    @Test
    void shouldRejectMissingActionType() {
        GameActionContext context = context(null, TurnPhase.MAIN);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Action type is required");
    }

    private ThrowingCallable validationCall(GameActionContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        };
    }

    private GameActionContext context(GameActionType actionType, TurnPhase turnPhase) {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        GameStateDto currentState = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                turnPhase,
                1,
                1,
                actorUserId,
                List.of(actorUserId),
                List.of(GameActionType.DRAW_CARD),
                Instant.now());

        return new GameActionContext(
                gameId,
                actorUserId,
                request(actionType),
                currentState);
    }

    private GameActionRequestDto request(GameActionType actionType) {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actionType,
                1,
                Map.<String, Object>of());
    }
}
