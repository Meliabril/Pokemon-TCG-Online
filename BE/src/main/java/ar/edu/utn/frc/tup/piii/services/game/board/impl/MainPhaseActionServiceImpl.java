package ar.edu.utn.frc.tup.piii.services.game.board.impl;

import ar.edu.utn.frc.tup.piii.services.game.energy.AttachEnergyService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.board.MainPhaseActionService;
import ar.edu.utn.frc.tup.piii.services.game.board.PlayBasicPokemonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MainPhaseActionServiceImpl implements MainPhaseActionService {

    private final PlayBasicPokemonService playBasicPokemonService;
    private final AttachEnergyService attachEnergyService;

    @Override
    public GameActionExecutionResult playBasicPokemon(GameActionContext context) {
        return playBasicPokemonService.playBasicPokemon(context);
    }

    @Override
    public GameActionExecutionResult attachEnergy(GameActionContext context) {
        return attachEnergyService.attachEnergy(context);
    }
}
