package ar.edu.utn.frc.tup.piii.services.game.turn.validation;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.validation.ActionValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class PhaseValidator implements ActionValidator {

    @Override
    public void validate(GameActionContext context) {
        if (context == null || context.currentState() == null) {
            return;
        }

        if (context.request() == null || context.request().actionType() == null) {
            throw new InvalidGameActionException("Action type is required to validate the game phase.");
        }

        GameActionType actionType = context.request().actionType();
        TurnPhase currentPhase = context.currentState().turn().currentPhase();
        if (GameActionType.END_TURN.equals(actionType)) {
            if (TurnPhase.MAIN.equals(currentPhase) || TurnPhase.ATTACK.equals(currentPhase)) {
                return;
            }
            throw new InvalidGameActionException("The action is not valid during the current turn phase.");
        }

        if (GameActionType.DECLARE_ATTACK.equals(actionType)) {
            if (TurnPhase.MAIN.equals(currentPhase) || TurnPhase.ATTACK.equals(currentPhase)) {
                return;
            }
            throw new InvalidGameActionException("The action is not valid during the current turn phase.");
        }

        TurnPhase requiredPhase = requiredPhaseFor(actionType);
        if (requiredPhase == null) {
            return;
        }

        if (!requiredPhase.equals(currentPhase)) {
            throw new InvalidGameActionException("The action is not valid during the current turn phase.");
        }
    }

    private TurnPhase requiredPhaseFor(GameActionType actionType) {
        switch (actionType) {
            case DRAW_CARD:
                return TurnPhase.DRAW;
            case PLAY_BASIC_POKEMON:
            case EVOLVE_POKEMON:
            case ATTACH_ENERGY:
            case PLAY_TRAINER:
            case RETREAT:
                return TurnPhase.MAIN;
            case SELECT_TARGET:
                return TurnPhase.ATTACK;
            case PROMOTE_BENCH_POKEMON:
            case TAKE_PRIZE_CARD:
            case RESOLVE_ATTACK_CHOICE:
                return TurnPhase.BETWEEN_TURNS;
            default:
                return null;
        }
    }
}
