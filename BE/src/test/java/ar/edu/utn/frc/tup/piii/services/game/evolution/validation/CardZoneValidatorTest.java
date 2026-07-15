package ar.edu.utn.frc.tup.piii.services.game.evolution.validation;




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
class CardZoneValidatorTest {

    @Mock
    private GameActionContext context;

    private final CardZoneValidator validator = new CardZoneValidator();

    @Test
    void shouldAllowEvolutionWhenTargetCardIsActive() {
        UUID targetCardId = UUID.randomUUID();
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of("targetCardId", targetCardId),
                state(Map.<UUID, String>of(targetCardId, "ACTIVE")));

        validator.validate(context);
    }

    @Test
    void shouldAllowEvolutionWhenTargetCardIsBench() {
        UUID targetCardId = UUID.randomUUID();
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of("targetCardId", targetCardId.toString()),
                state(Map.<UUID, String>of(targetCardId, "BENCH")));

        validator.validate(context);
    }

    @Test
    void shouldAllowEvolutionUsingPokemonInPlayFallback() {
        UUID pokemonInPlayId = UUID.randomUUID();
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of("pokemonInPlayId", pokemonInPlayId),
                state(Map.<UUID, String>of(pokemonInPlayId, "ACTIVE")));

        validator.validate(context);
    }

    @Test
    void shouldRejectEvolutionWhenTargetCardIsInHand() {
        UUID targetCardId = UUID.randomUUID();
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of("targetCardId", targetCardId),
                state(Map.<UUID, String>of(targetCardId, "HAND")));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Target card must be in ACTIVE or BENCH zone");
    }

    @Test
    void shouldRejectEvolutionWhenTargetIsMissing() {
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of(),
                state(Map.<UUID, String>of()));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Target card must be in ACTIVE or BENCH zone");
    }

    @Test
    void shouldRejectEvolutionWhenZoneIsMissing() {
        UUID targetCardId = UUID.randomUUID();
        configureContext(
                GameActionType.EVOLVE_POKEMON,
                Map.<String, Object>of("targetCardId", targetCardId),
                state(Map.<UUID, String>of()));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Target card must be in ACTIVE or BENCH zone");
    }

    @Test
    void shouldAllowNonEvolutionActionWithoutZoneCheck() {
        configureContext(
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of(),
                state(Map.<UUID, String>of()));

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

    private GameStateDto state(Map<UUID, String> cardZones) {
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
                Set.<UUID>of(),
                cardZones,
                Map.<UUID, UUID>of(),
                List.of(GameActionType.EVOLVE_POKEMON),
                Instant.now());
    }
}
