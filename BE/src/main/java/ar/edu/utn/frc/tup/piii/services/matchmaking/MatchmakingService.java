package ar.edu.utn.frc.tup.piii.services.matchmaking;

import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;

import java.util.UUID;

public interface MatchmakingService {

    MatchmakingQueueStatusDto joinQueue(UUID userId);

    void leaveQueue(UUID userId);

    boolean isQueued(UUID userId);

    boolean hasActiveGame(UUID userId);

    MatchmakingQueueStatusDto getMyQueueStatus(UUID userId);

    MatchmakingQueueStatusDto joinCustomQueue(UUID userId);

    MatchmakingQueueStatusDto joinCustomGame(UUID userId, String customMatchCode);
}
