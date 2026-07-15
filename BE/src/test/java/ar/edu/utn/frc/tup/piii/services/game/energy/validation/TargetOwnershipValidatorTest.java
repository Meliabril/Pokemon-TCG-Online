package ar.edu.utn.frc.tup.piii.services.game.energy.validation;




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
class TargetOwnershipValidatorTest {

    @Mock
    private GameActionContext context;

    private final TargetOwnershipValidator validator = new TargetOwnershipValidator();

    @Test
    void shouldAllowEnergyAttachmentWhenTargetBelongsToActor() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("targetPokemonId", targetPokemonId),
                state(actorUserId, Map.<UUID, UUID>of(targetPokemonId, actorUserId)));

        validator.validate(context);
    }

    @Test
    void shouldAllowEnergyAttachmentUsingPokemonInPlayFallback() {
        UUID actorUserId = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("pokemonInPlayId", pokemonInPlayId.toString()),
                state(actorUserId, Map.<UUID, UUID>of(pokemonInPlayId, actorUserId)));

        validator.validate(context);
    }

    @Test
    void shouldRejectEnergyAttachmentWhenTargetBelongsToOpponent() {
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("targetPokemonId", targetPokemonId),
                state(actorUserId, Map.<UUID, UUID>of(targetPokemonId, opponentUserId)));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Cannot target an opponent's card for this action");
    }

    @Test
    void shouldRejectEnergyAttachmentWhenTargetIsMissing() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of(),
                state(actorUserId, Map.<UUID, UUID>of()));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Cannot target an opponent's card for this action");
    }

    @Test
    void shouldRejectEnergyAttachmentWhenOwnerIsMissing() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("targetPokemonId", targetPokemonId),
                state(actorUserId, Map.<UUID, UUID>of()));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Cannot target an opponent's card for this action");
    }

    @Test
    void shouldAllowNonEnergyActionWithoutOwnershipCheck() {
        UUID actorUserId = UUID.randomUUID();
        configureContextWithoutActor(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of(),
                state(actorUserId, Map.<UUID, UUID>of()));

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

    private void configureContext(
            UUID actorUserId,
            GameActionType actionType,
            Map<String, Object> payload,
            GameStateDto state) {
        when(context.actorUserId()).thenReturn(actorUserId);
        when(context.request()).thenReturn(request(actionType, payload));
        when(context.currentState()).thenReturn(state);
    }

    private void configureContextWithoutActor(
            GameActionType actionType,
            Map<String, Object> payload,
            GameStateDto state) {
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

    private GameStateDto state(UUID actorUserId, Map<UUID, UUID> cardOwners) {
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
                Set.<UUID>of(),
                Map.<UUID, String>of(),
                cardOwners,
                List.of(GameActionType.ATTACH_ENERGY),
                Instant.now());
    }
}
