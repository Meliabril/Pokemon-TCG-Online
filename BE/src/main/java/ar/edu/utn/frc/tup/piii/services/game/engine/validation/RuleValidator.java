package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;

/**

 * Validates whether a game action is legal for the current state.
 *
 * Implementations must not execute actions, modify state, persist changes, or
 * publish events. Invalid actions should be rejected with domain exceptions.

 */
public interface RuleValidator {

    /**
     * Performs rule validation for an incoming game action.
     *
     * @param context orchestration context
     */
    void validate(GameActionContext context);
}
