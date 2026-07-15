package ar.edu.utn.frc.tup.piii.services.game.energy.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.board.MainPhaseActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttachEnergyActionHandler implements GameActionHandler {

    private final MainPhaseActionService mainPhaseActionService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.ATTACH_ENERGY;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return mainPhaseActionService.attachEnergy(context);
    }
}
