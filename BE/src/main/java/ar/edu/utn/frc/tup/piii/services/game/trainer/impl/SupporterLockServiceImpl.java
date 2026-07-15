package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.SupporterLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupporterLockServiceImpl implements SupporterLockService {

    private final GameParticipantStateService gameParticipantStateService;

    @Override
    public void lockSupporters(UUID gameId, UUID playerId, int lockedTurn) {
        GameParticipant participant = gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)
                .orElseThrow(() -> new InvalidGameActionException("Player participant was not found for supporter lock"));
        participant.setSupporterLockedTurn(lockedTurn);
        gameParticipantStateService.save(participant);
    }

    @Override
    public boolean isSupporterLocked(UUID gameId, UUID playerId, int currentTurnNumber) {
        GameParticipant participant = gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)
                .orElseThrow(() -> new InvalidGameActionException("Player participant was not found for supporter lock"));
        Integer lockedTurn = participant.getSupporterLockedTurn();
        if (lockedTurn == null) {
            return false;
        }
        if (lockedTurn == currentTurnNumber) {
            return true;
        }
        if (lockedTurn < currentTurnNumber) {
            participant.setSupporterLockedTurn(null);
            gameParticipantStateService.save(participant);
        }
        return false;
    }

    @Override
    public void expireLock(GameParticipant participant, int currentTurnNumber) {
        if (participant == null || participant.getSupporterLockedTurn() == null) {
            return;
        }
        if (participant.getSupporterLockedTurn() <= currentTurnNumber) {
            participant.setSupporterLockedTurn(null);
            gameParticipantStateService.save(participant);
        }
    }
}
