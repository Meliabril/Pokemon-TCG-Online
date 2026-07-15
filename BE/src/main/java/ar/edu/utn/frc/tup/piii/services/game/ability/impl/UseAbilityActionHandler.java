package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UseAbilityActionHandler implements GameActionHandler {

    private final AbilityService abilityService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.USE_ABILITY;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return abilityService.useAbility(context);
    }
}
