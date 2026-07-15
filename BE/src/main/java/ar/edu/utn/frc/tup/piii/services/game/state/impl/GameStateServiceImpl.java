package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateRestorer;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameStateServiceImpl implements GameStateService {

    private final GameStateQueryService gameStateQueryService;
    private final GameStateRestorer gameStateRestorer;

    @Override
    public GameStateDto buildVisibleState(Game game) {
        return gameStateQueryService.buildVisibleState(game);
    }

    @Override
    public GameStateDto buildVisibleState(Game game, UUID viewerUserId) {
        return gameStateQueryService.buildVisibleState(game, viewerUserId);
    }

    @Override
    public void restoreFromSnapshot(Game game, GameStateDto snapshot) {
        gameStateRestorer.restoreFromSnapshot(game, snapshot);
    }
}
