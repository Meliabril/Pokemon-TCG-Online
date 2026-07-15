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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyValidatorTest {

    @Mock
    private GameActionContext context;

    private final IdempotencyValidator validator = new IdempotencyValidator();

    @Test
    void shouldAllowNewClientActionId() {
        UUID clientActionId = UUID.randomUUID();
        configureContext(request(clientActionId), state(Set.<UUID>of()));

        validator.validate(context);
    }

    @Test
    void shouldRejectAlreadyProcessedClientActionId() {
        UUID clientActionId = UUID.randomUUID();
        configureContext(request(clientActionId), state(Set.of(clientActionId)));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Action already processed");
    }

    @Test
    void shouldIgnoreNullClientActionId() {
        configureContext(request(null), state(Set.of(UUID.randomUUID())));

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

    private void configureContext(GameActionRequestDto request, GameStateDto state) {
        when(context.request()).thenReturn(request);
        when(context.currentState()).thenReturn(state);
    }

    private GameActionRequestDto request(UUID clientActionId) {
        return new GameActionRequestDto(
                UUID.randomUUID(),
                clientActionId,
                GameActionType.END_TURN,
                1,
                Map.<String, Object>of());
    }

    private GameStateDto state(Set<UUID> processedClientActionIds) {
        UUID actorUserId = UUID.randomUUID();
        return GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                3,
                1,
                actorUserId,
                List.of(actorUserId),
                false,
                false,
                false,
                Map.<UUID, Integer>of(),
                Map.<UUID, List<SpecialConditionType>>of(),
                3,
                actorUserId,
                Map.<UUID, Integer>of(),
                Map.<UUID, List<UUID>>of(),
                Map.<UUID, Set<UUID>>of(),
                processedClientActionIds,
                Map.<UUID, String>of(),
                Map.<UUID, UUID>of(),
                List.of(GameActionType.END_TURN),
                Instant.now());
    }
}
