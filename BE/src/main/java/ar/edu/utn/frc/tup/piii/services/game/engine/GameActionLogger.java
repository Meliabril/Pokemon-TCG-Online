package ar.edu.utn.frc.tup.piii.services.game.engine;

import java.util.Map;
import java.util.UUID;

/**
 * Minimal action logging port for orchestration metadata.
 */
public interface GameActionLogger {

    /**
     * Logs game action metadata without coupling to persistence details.
     *
     * @param gameId game identifier
     * @param actorUserId actor user identifier
     * @param clientActionId client action identifier
     * @param payload original action payload
     * @param result execution result payload
     * @param actionType action type name
     * @param newStateVersion resulting state version
     */
    void logAction(
            UUID gameId,
            UUID actorUserId,
            UUID clientActionId,
            Map<String, Object> payload,
            Map<String, Object> result,
            String actionType,
            Integer newStateVersion);
}
