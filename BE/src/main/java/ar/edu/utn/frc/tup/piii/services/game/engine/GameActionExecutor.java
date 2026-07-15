package ar.edu.utn.frc.tup.piii.services.game.engine;

/**

 * Entry point for executing a game action after it has been validated.
 *
 * Implementations should delegate action-specific behavior to small
 * {@link GameActionHandler} implementations instead of accumulating all game
 * rules in this contract.

 */
public interface GameActionExecutor {

    /**
     * Executes a game action using the supplied context.
     *
     * @param context orchestration context
     * @return execution result with resulting state and emitted events
     */
    GameActionExecutionResult execute(GameActionContext context);
}
