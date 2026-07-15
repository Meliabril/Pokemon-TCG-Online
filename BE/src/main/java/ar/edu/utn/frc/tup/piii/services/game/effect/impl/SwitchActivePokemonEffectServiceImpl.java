package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.effect.SwitchActivePokemonEffectService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SwitchActivePokemonEffectServiceImpl implements SwitchActivePokemonEffectService {

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public void switchWithBench(PokemonInPlay activePokemon, PokemonInPlay benchPokemon) {
        if (activePokemon == null || benchPokemon == null) {
            throw new InvalidGameActionException("Active and bench Pokemon are required to switch");
        }
        if (activePokemon.getActiveCardInstance() == null || benchPokemon.getActiveCardInstance() == null) {
            throw new InvalidGameActionException("Pokemon must have an active card instance to switch");
        }
        Integer formerBenchSlot = benchPokemon.getSlotPosition();
        GameCardInstance activeCardInstance = activePokemon.getActiveCardInstance();
        GameCardInstance benchCardInstance = benchPokemon.getActiveCardInstance();
        gameCardInstanceStateService.swapActiveWithBench(activeCardInstance.getId(), benchCardInstance.getId(), formerBenchSlot);
        pokemonInPlayStateService.swapActiveWithBench(activePokemon.getId(), benchPokemon.getId(), formerBenchSlot);
    }
}
