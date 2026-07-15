package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetreatActionHandler implements GameActionHandler {

    private final RetreatService retreatService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.RETREAT;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return retreatService.retreat(context);
    }
}
