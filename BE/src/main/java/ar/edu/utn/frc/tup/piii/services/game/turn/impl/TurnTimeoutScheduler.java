package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Periodically scans for active games whose current turn has exceeded the timeout window
 * and delegates the pass/finish decision to {@link TurnTimeoutService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TurnTimeoutScheduler {

    private static final long SCAN_INTERVAL_MILLIS = 10_000L;

    private final GameRepository gameRepository;
    private final TurnTimeoutService turnTimeoutService;

    @Scheduled(fixedDelay = SCAN_INTERVAL_MILLIS)
    public void checkExpiredTurns() {
        Instant cutoff = Instant.now().minus(TurnTimeoutService.TURN_TIMEOUT);
        List<UUID> expiredGameIds = gameRepository.findActiveGameIdsWithExpiredTurn(GameStatus.ACTIVE, cutoff);

        for (UUID gameId : expiredGameIds) {
            try {
                turnTimeoutService.applyTimeoutIfDue(gameId);
            } catch (RuntimeException exception) {
                log.warn("Failed to apply turn timeout for game {}", gameId, exception);
            }
        }
    }
}
