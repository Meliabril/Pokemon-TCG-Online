package ar.edu.utn.frc.tup.piii.services.game.presence.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.services.game.presence.GameParticipantPresenceChangedEvent;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GamePresenceNotificationListener {

    private final GameRealtimeEventService gameRealtimeEventService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPresenceChanged(GameParticipantPresenceChangedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", event.userId().toString());
        payload.put("connected", event.connected());
        if (event.lastSeenAt() == null) {
            payload.put("lastSeenAt", null);
        } else {
            payload.put("lastSeenAt", event.lastSeenAt().toString());
        }

        gameRealtimeEventService.dispatchPublic(
                event.gameId(),
                GameEventType.PARTICIPANT_PRESENCE_CHANGED,
                event.stateVersion(),
                payload);
    }
}
