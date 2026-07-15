package ar.edu.utn.frc.tup.piii.services.game.turn;

import java.time.Duration;
import java.util.UUID;

public interface TurnTimeoutService {

    Duration TURN_TIMEOUT = Duration.ofMinutes(2);

    int MAX_CONSECUTIVE_TIMEOUTS = 2;

    /**
     * Re-checks the given game and, if its active turn is still past the timeout window,
     * either forces a turn pass or finishes the game in favor of the opponent.
     */
    void applyTimeoutIfDue(UUID gameId);
}
