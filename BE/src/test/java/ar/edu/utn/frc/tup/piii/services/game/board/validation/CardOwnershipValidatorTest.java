package ar.edu.utn.frc.tup.piii.services.game.board.validation;




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
class CardOwnershipValidatorTest {

    @Mock
    private GameActionContext context;

    private final CardOwnershipValidator validator = new CardOwnershipValidator();

    @Test
    void shouldAllowBasicPokemonWhenCardIsInPlayerHand() {
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_BASIC_POKEMON,
                Map.<String, Object>of("cardId", cardId),
                state(actorUserId, Map.<UUID, List<UUID>>of(actorUserId, List.of(cardId))));

        validator.validate(context);
    }

    @Test
    void shouldAllowEnergyAttachmentWhenStringCardIdIsInPlayerHand() {
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.ATTACH_ENERGY,
                Map.<String, Object>of("cardId", cardId.toString()),
                state(actorUserId, Map.<UUID, List<UUID>>of(actorUserId, List.of(cardId))));

        validator.validate(context);
    }

    @Test
    void shouldAllowTrainerWhenCardInstanceIdIsInPlayerHand() {
        UUID actorUserId = UUID.randomUUID();
        UUID cardInstanceId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_TRAINER,
                Map.<String, Object>of("cardInstanceId", cardInstanceId.toString()),
                stateWithInstances(actorUserId, Map.<UUID, List<UUID>>of(actorUserId, List.of(cardInstanceId))));

        validator.validate(context);
    }

    @Test
    void shouldRejectTrainerWhenCardIsNotInPlayerHand() {
        UUID actorUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_TRAINER,
                Map.<String, Object>of("cardId", cardId),
                state(actorUserId, Map.<UUID, List<UUID>>of(actorUserId, List.<UUID>of())));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Card is not in player's hand");
    }

    @Test
    void shouldRejectCardActionWhenPayloadDoesNotContainCardId() {
        UUID actorUserId = UUID.randomUUID();
        configureContext(
                actorUserId,
                GameActionType.PLAY_BASIC_POKEMON,
                Map.<String, Object>of(),
                state(actorUserId, Map.<UUID, List<UUID>>of(actorUserId, List.<UUID>of())));

        assertThatThrownBy(validationCall())
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Card is not in player's hand");
    }

    @Test
    void shouldAllowNonCardActionWithoutHandOwnershipCheck() {
        UUID actorUserId = UUID.randomUUID();
        configureContextWithoutActor(
                GameActionType.DECLARE_ATTACK,
                Map.<String, Object>of(),
                state(actorUserId, Map.<UUID, List<UUID>>of()));

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

    private GameStateDto state(UUID actorUserId, Map<UUID, List<UUID>> cardsInHandByPlayer) {
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
                cardsInHandByPlayer,
                Map.<UUID, Set<UUID>>of(),
                List.of(GameActionType.PLAY_BASIC_POKEMON),
                Instant.now());
    }

    private GameStateDto stateWithInstances(UUID actorUserId, Map<UUID, List<UUID>> cardInstancesInHandByPlayer) {
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
                Set.of(),
                Map.<UUID, String>of(),
                Map.<UUID, UUID>of(),
                Map.<UUID, Integer>of(),
                Map.<UUID, Boolean>of(),
                Map.<UUID, UUID>of(),
                Map.<UUID, List<UUID>>of(),
                cardInstancesInHandByPlayer,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.now());
    }
}
