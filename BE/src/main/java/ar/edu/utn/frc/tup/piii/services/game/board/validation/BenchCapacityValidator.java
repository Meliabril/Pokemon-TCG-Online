package ar.edu.utn.frc.tup.piii.services.game.board.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Order(9)
public class BenchCapacityValidator implements ActionValidator {

    private static final int MAXIMUM_BENCH_CAPACITY = 5;

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.request() == null || context.currentState() == null) {
            return;
        }

        if (!GameActionType.PLAY_BASIC_POKEMON.equals(context.request().actionType())) {
            return;
        }

        UUID actorUserId = context.actorUserId();
        if (actorUserId == null) {
            return;
        }

        PlayerStateDto playerState = context.currentState().players().get(actorUserId);
        if (playerState == null) {
            return;
        }

        if (playerState.benchPokemonCount() >= MAXIMUM_BENCH_CAPACITY) {
            throw new InvalidGameActionException("Bench is full. Maximum capacity is 5");
        }
    }
}
