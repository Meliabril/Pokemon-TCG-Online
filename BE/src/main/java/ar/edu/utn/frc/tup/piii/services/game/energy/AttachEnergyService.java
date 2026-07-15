package ar.edu.utn.frc.tup.piii.services.game.energy;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;

public interface AttachEnergyService {

    GameActionExecutionResult attachEnergy(GameActionContext context);
}
