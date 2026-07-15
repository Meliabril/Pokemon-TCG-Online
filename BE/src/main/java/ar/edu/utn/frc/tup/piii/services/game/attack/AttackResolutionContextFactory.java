package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;

public interface AttackResolutionContextFactory {

    AttackResolutionContext create(GameActionContext context);
}
