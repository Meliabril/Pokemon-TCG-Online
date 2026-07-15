package ar.edu.utn.frc.tup.piii.services.game.trainer;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;

import java.util.UUID;

public interface SupporterLockService {

    void lockSupporters(UUID gameId, UUID playerId, int lockedTurn);

    boolean isSupporterLocked(UUID gameId, UUID playerId, int currentTurnNumber);

    void expireLock(GameParticipant participant, int currentTurnNumber);
}
