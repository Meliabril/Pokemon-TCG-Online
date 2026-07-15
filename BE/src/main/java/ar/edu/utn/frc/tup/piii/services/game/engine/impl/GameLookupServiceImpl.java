package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameLookupServiceImpl implements GameLookupService {

    private final GameDataService gameDataService;
    private final GameParticipantStateService gameParticipantStateService;

    @Override
    @Transactional(readOnly = true)
    public Game getRequiredGame(UUID gameId) {
        return gameDataService.getRequiredGame(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertGameExists(UUID gameId) {
        gameDataService.assertGameExists(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertParticipant(UUID gameId, UUID userId) {
        assertGameExists(gameId);

        if (userId == null || !gameParticipantStateService.existsByGameIdAndUserId(gameId, userId)) {
            throw new ForbiddenActionException("The authenticated user is not a participant of this game.");
        }
    }
}
