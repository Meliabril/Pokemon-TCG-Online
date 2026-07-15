package ar.edu.utn.frc.tup.piii.services.matchmaking;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;

import java.util.UUID;

public interface MatchmakingQueueStatusNotifier {

    void notifyQueueStatus(UUID userId, MatchmakingQueueStatusDto status);
}
