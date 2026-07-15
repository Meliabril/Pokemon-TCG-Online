package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DamageApplicationServiceImpl implements DamageApplicationService {

    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public int applyDamage(PokemonInPlay pokemonInPlay, int damage) {
        if (damage <= 0) {
            return currentDamageCounters(pokemonInPlay);
        }

        int addedCounters = damage / 10;
        pokemonInPlay.setDamageCounters(currentDamageCounters(pokemonInPlay) + addedCounters);
        pokemonInPlayStateService.save(pokemonInPlay);
        return pokemonInPlay.getDamageCounters();
    }

    @Override
    public int healDamage(PokemonInPlay pokemonInPlay, int healingAmount) {
        if (healingAmount <= 0) {
            return currentDamageCounters(pokemonInPlay);
        }

        int healedCounters = healingAmount / 10;
        int newDamageCounters = currentDamageCounters(pokemonInPlay) - healedCounters;
        if (newDamageCounters < 0) {
            newDamageCounters = 0;
        }

        pokemonInPlay.setDamageCounters(newDamageCounters);
        pokemonInPlayStateService.save(pokemonInPlay);
        return pokemonInPlay.getDamageCounters();
    }

    private int currentDamageCounters(PokemonInPlay pokemonInPlay) {
        if (pokemonInPlay.getDamageCounters() == null) {
            return 0;
        }

        return pokemonInPlay.getDamageCounters();
    }
}
