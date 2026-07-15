package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Minimal logging stub based on application logs.
 */
@Service
public class Slf4jGameActionLoggerImpl implements GameActionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(Slf4jGameActionLoggerImpl.class);

    @Override
    public void logAction(
            UUID gameId,
            UUID actorUserId,
            UUID clientActionId,
            Map<String, Object> payload,
            Map<String, Object> result,
            String actionType,
            Integer newStateVersion) {
        LOGGER.info(
                "Action executed gameId={} actorUserId={} clientActionId={} actionType={} newStateVersion={} payload={} result={}",
                gameId,
                actorUserId,
                clientActionId,
                actionType,
                newStateVersion,
                payload,
                result);
    }
}

