package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantStateService {

    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    List<GameParticipant> findOrderedByGameId(UUID gameId);

    Optional<GameParticipant> findByGameIdAndUserId(UUID gameId, UUID userId);

    GameParticipant save(GameParticipant participant);

    List<UUID> findPlayerIds(UUID gameId);

    UUID findOpponentUserId(UUID gameId, UUID actorUserId);

    void resetConsecutiveTimeouts(UUID gameId, UUID userId);
}
