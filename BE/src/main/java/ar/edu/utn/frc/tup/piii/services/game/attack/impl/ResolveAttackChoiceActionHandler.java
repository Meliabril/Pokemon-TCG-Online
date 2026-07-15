package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackChoiceService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResolveAttackChoiceActionHandler implements GameActionHandler {

    private final AttackChoiceService attackChoiceService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.RESOLVE_ATTACK_CHOICE;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return attackChoiceService.resolveAttackChoice(context);
    }
}
