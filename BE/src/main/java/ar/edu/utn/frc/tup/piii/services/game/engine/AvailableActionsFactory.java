package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

import java.util.List;

public interface AvailableActionsFactory {

    List<GameActionType> mainPhaseActions(boolean energyAttachedThisTurn);

    List<GameActionType> mainPhaseActionsAfterRetreat(boolean energyAttachedThisTurn);

    List<GameActionType> attackPhaseActions();

    List<GameActionType> drawPhaseActions();

    List<GameActionType> promotionActions();

    List<GameActionType> noActions();
}
