package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;

import java.util.UUID;

public interface GameStateService {

    GameStateDto buildVisibleState(Game game);

    GameStateDto buildVisibleState(Game game, UUID viewerUserId);

    void restoreFromSnapshot(Game game, GameStateDto snapshot);
}
