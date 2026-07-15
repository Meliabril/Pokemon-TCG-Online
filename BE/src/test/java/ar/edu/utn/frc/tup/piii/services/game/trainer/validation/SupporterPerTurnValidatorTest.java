package ar.edu.utn.frc.tup.piii.services.game.trainer.validation;




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
class SupporterPerTurnValidatorTest {

    @Mock
    private GameActionContext context;

    private final SupporterPerTurnValidator validator = new SupporterPerTurnValidator();

    @Test
    void shouldAllowSupporterWhenNoSupporterWasPlayedThisTurn() {
        configureContext(
                GameActionType.PLAY_TRAINER,
                Map.<String, Object>of("trainerSubtype", "SUPPORTER"),
                state(false, false, false));

        validator.validate(context);
    }

    @Test
    void shouldAllowNonSupporterTrainerWhenSupporterWasPlayedThisTurn() {
        configureContext(
                GameActionType.PLAY_TRAINER,
                Map.<String, Object>of("trainerSubtype", "ITEM"),
                state(false, true, false));

        validator.validate(context);
    }

    @Test
    void shouldAllowNonTrainerActionWhenSupporterWasPlayedThisTurn() {
        configureContext(
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("trainerSubtype", "SUPPORTER"),
                state(false, true, false));

        validator.validate(context);
    }

    @Test
    void shouldRejectSupporterWhenSupporterWasAlreadyPlayedThisTurn() {
        configureContext(
                GameActionType.PLAY_TRAINER,
                Map.<String, Object>of("trainerSubtype", "SUPPORTER"),
                state(false, true, false));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Supporter already played this turn");
    }

    private ThrowingCallable validationCall() {
        return new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        };
    }

    private void configureContext(GameActionType actionType, Map<String, Object> payload, GameStateDto state) {
        when(context.request()).thenReturn(request(actionType, payload));
        when(context.currentState()).thenReturn(state);
    }

    private GameActionRequestDto request(GameActionType actionType, Map<String, Object> payload) {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                actionType,
                1,
                payload);
    }

    private GameStateDto state(
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn) {
        UUID activePlayerId = UUID.randomUUID();
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                1,
                1,
                activePlayerId,
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.now());
    }
}
