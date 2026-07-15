package ar.edu.utn.frc.tup.piii.services.game.turn.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.turn.TurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrawCardActionHandler implements GameActionHandler {

    private final TurnService turnService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.DRAW_CARD;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return turnService.drawCard(context);
    }
}
