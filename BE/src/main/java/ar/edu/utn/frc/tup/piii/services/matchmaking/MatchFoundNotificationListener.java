package ar.edu.utn.frc.tup.piii.services.matchmaking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MatchFoundNotificationListener {

    private final MatchFoundNotifier matchFoundNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchFound(MatchFoundApplicationEvent event) {
        matchFoundNotifier.notifyMatchFound(
                event.firstUserId(),
                event.secondUserId(),
                event.gameId(),
                event.matchedAt());
    }
}
