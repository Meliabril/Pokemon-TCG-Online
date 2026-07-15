package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

/**
 * Contract for executing one concrete game action type.
 *
 * Implementations must assume the action was already approved by
 * RuleValidator and should not perform cross-cutting validation, persistence
 * orchestration, or WebSocket publishing.
 */
public interface GameActionHandler {

    GameActionType supportedAction();

    GameActionExecutionResult execute(GameActionContext context);
}

