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
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
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

class PlayerTurnValidatorTest {

    private final PlayerTurnValidator validator = new PlayerTurnValidator();

    @Test
    void shouldAllowActionWhenActorIsActivePlayer() {
        UUID actorUserId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, actorUserId);

        validator.validate(context);
    }

    @Test
    void shouldRejectActionWhenActorIsNotActivePlayer() {
        UUID actorUserId = UUID.randomUUID();
        UUID activePlayerId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, activePlayerId);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not this player's turn");
    }

    @Test
    void shouldRejectActionWhenActorIsMissing() {
        UUID activePlayerId = UUID.randomUUID();
        GameActionContext context = context(null, activePlayerId);

        assertThatThrownBy(validationCall(context))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not this player's turn");
    }

    @Test
    void shouldAllowInitialPokemonSelectionWithoutActivePlayer() {
        UUID actorUserId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, null, GameActionType.CHOOSE_INITIAL_POKEMON, GameStatus.SETUP);

        assertThatCode(validationCall(context)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowMulliganNoticeAcknowledgementWithoutActivePlayer() {
        UUID actorUserId = UUID.randomUUID();
        GameActionContext context = context(actorUserId, null, GameActionType.ACK_MULLIGAN_NOTICE, GameStatus.SETUP);

        assertThatCode(validationCall(context)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowPendingAttackChoiceOwnerToResolveOutsideTheirTurn() {
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameStateDto currentState = GameStateTestFactory.state(
                        gameId,
                        GameStatus.ACTIVE,
                        TurnPhase.BETWEEN_TURNS,
                        1,
                        1,
                        attackerUserId,
                        List.of(attackerUserId, defenderUserId),
                        List.of(GameActionType.RESOLVE_ATTACK_CHOICE),
                        Instant.now())
                .toBuilder()
                .resolution(ResolutionStateDto.builder()
                        .resolutionType(ResolutionStateDto.ATTACK_CHOICE_REQUIRED)
                        .pendingChoicePlayerId(defenderUserId)
                        .pendingChoiceType("SELECT_HAND_CARDS_TO_DISCARD")
                        .turnEndingPlayerId(attackerUserId)
                        .build())
                .build();
        GameActionContext context = new GameActionContext(
                gameId,
                defenderUserId,
                request(GameActionType.RESOLVE_ATTACK_CHOICE),
                currentState);

        assertThatCode(validationCall(context)).doesNotThrowAnyException();
    }

    private ThrowingCallable validationCall(GameActionContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                validator.validate(context);
            }
        };
    }

    private GameActionContext context(UUID actorUserId, UUID activePlayerId) {
        return context(actorUserId, activePlayerId, GameActionType.ATTACH_ENERGY, GameStatus.ACTIVE);
    }

    private GameActionContext context(UUID actorUserId, UUID activePlayerId, GameActionType actionType, GameStatus status) {
        UUID gameId = UUID.randomUUID();
        GameStateDto currentState = GameStateTestFactory.state(
                gameId,
                status,
                TurnPhase.MAIN,
                1,
                1,
                activePlayerId,
                playerIds(actorUserId, activePlayerId),
                List.of(actionType),
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

    private List<UUID> playerIds(UUID actorUserId, UUID activePlayerId) {
        if (activePlayerId != null) {
            return List.of(activePlayerId);
        }
        if (actorUserId != null) {
            return List.of(actorUserId);
        }
        return List.of();
    }
}
