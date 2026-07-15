package ar.edu.utn.frc.tup.piii.services.game.evolution.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.services.game.evolution.EvolutionService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvolvePokemonActionHandler implements GameActionHandler {

    private final EvolutionService evolutionService;

    @Override
    public GameActionType supportedAction() {
        return GameActionType.EVOLVE_POKEMON;
    }

    @Override
    public GameActionExecutionResult execute(GameActionContext context) {
        return evolutionService.evolve(context);
    }
}
