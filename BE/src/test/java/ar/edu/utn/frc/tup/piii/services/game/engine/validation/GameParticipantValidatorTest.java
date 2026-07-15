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
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameParticipantValidatorTest {

    private final GameParticipantValidator validator = new GameParticipantValidator();

    @Test
    void shouldAllowActionWhenActorBelongsToGamePlayers() {
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, actorUserId, List.of(actorUserId, opponentUserId));

        validator.validate(context);
    }

    @Test
    void shouldRejectActionWhenActorDoesNotBelongToGamePlayers() {
        UUID actorUserId = UUID.randomUUID();
        UUID activePlayerId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, activePlayerId, List.of(activePlayerId, opponentUserId));

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void shouldRejectActionWhenActorIsMissing() {
        UUID activePlayerId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        GameActionContext context = context(null, activePlayerId, List.of(activePlayerId, opponentUserId));

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not a participant");
    }

    private ThrowingCallable validationCall(GameActionContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        };
    }

    private GameActionContext context(UUID actorUserId, UUID activePlayerId, List<UUID> playerIds) {
        UUID gameId = UUID.randomUUID();
        GameStateDto currentState = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                1,
                1,
                activePlayerId,
                playerIds,
                List.of(GameActionType.ATTACH_ENERGY),
                Instant.now());

        return new GameActionContext(
                gameId,
                actorUserId,
                request(GameActionType.ATTACH_ENERGY),
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
