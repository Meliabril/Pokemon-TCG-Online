package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutor;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves already-validated actions to small handlers.
 */
@Service
public class GameActionExecutorImpl implements GameActionExecutor {

    private final Map<GameActionType, GameActionHandler> handlersByAction;

    public GameActionExecutorImpl(List<GameActionHandler> handlers) {
        Objects.requireNonNull(handlers, "Game action handlers are required");

        Map<GameActionType, GameActionHandler> resolvedHandlers = new EnumMap<>(GameActionType.class);
        for (GameActionHandler handler : handlers) {
            GameActionType supportedAction = Objects.requireNonNull(
                    handler.supportedAction(),
                    "Game action handler must declare a supported action");
            GameActionHandler previousHandler = resolvedHandlers.putIfAbsent(supportedAction, handler);
            if (previousHandler != null) {
                throw new IllegalStateException(
                        "Multiple game action handlers were registered for action " + supportedAction);
            }
        }
        this.handlersByAction = Map.copyOf(resolvedHandlers);
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        if (context == null || context.request() == null || context.request().actionType() == null) {
            throw new InvalidGameActionException("Action type is required to execute a game action");
        }

        GameActionType actionType = context.request().actionType();
        GameActionHandler handler = handlersByAction.get(actionType);
        if (handler == null) {
            throw new InvalidGameActionException("No game action handler is available for action " + actionType);
        }

        return handler.execute(context);
    }
}
