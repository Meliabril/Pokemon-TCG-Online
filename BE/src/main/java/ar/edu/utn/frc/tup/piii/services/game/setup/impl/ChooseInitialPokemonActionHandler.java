package ar.edu.utn.frc.tup.piii.services.game.setup.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import ar.edu.utn.frc.tup.piii.services.game.setup.SetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChooseInitialPokemonActionHandler implements GameActionHandler {

    private final SetupService setupService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.CHOOSE_INITIAL_POKEMON;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return setupService.chooseInitialPokemon(context);
    }
}
