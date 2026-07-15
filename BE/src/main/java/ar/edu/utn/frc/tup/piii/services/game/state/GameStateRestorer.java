package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;

public interface GameStateRestorer {

    void restoreFromSnapshot(Game game, GameStateDto snapshot);
}
