package ar.edu.utn.frc.tup.piii.services.game.setup;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface SetupService {

    GameActionExecutionResult startGame(GameActionContext context);

    GameActionExecutionResult ackMulliganNotice(GameActionContext context);

    GameActionExecutionResult chooseInitialPokemon(GameActionContext context);
}
