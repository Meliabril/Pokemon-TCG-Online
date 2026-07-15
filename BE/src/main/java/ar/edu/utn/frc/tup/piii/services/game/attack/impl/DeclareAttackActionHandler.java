package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeclareAttackActionHandler implements GameActionHandler {

    private final AttackService attackService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.DECLARE_ATTACK;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return attackService.declareAttack(context);
    }
}
