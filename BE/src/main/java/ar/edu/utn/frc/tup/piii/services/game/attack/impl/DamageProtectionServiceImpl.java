package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageProtectionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DamageProtectionServiceImpl implements DamageProtectionService {

    private final PokemonInPlayStateService pokemonInPlayStateService;

    public boolean consumeProtection(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        return consumeProtection(pokemonInPlay, currentTurnNumber, 0);
    }

    @Override
    public boolean consumeProtection(PokemonInPlay pokemonInPlay, int currentTurnNumber, int incomingDamage) {
        Integer protectionTurn = pokemonInPlay.getDamageProtectionTurn();
        if (protectionTurn == null || protectionTurn != currentTurnNumber) {
            return false;
        }
        Integer threshold = pokemonInPlay.getDamageProtectionThreshold();
        boolean prevented = threshold == null || incomingDamage <= threshold;

        // The shield is consumed by being attacked during its protection window, not by the
        // outcome of that attack: once this Pokemon has been targeted, the effect must be
        // cleared right away whether or not the damage was low enough to be prevented.
        // Returning early here (the previous behaviour) left the flag set whenever
        // incomingDamage exceeded the threshold, relying on a later, unrelated cleanup
        // (expireProtection, run from the between-turns resolution of this same action) to
        // remove it - a cleanup step that is skipped entirely if this same attack knocks the
        // Pokemon out, leaving the field stale.
        pokemonInPlay.setDamageProtectionTurn(null);
        pokemonInPlay.setDamageProtectionThreshold(null);
        pokemonInPlayStateService.save(pokemonInPlay);
        return prevented;
    }

    @Override
    public void expireProtection(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer protectionTurn = pokemonInPlay.getDamageProtectionTurn();
        if (protectionTurn == null || protectionTurn > currentTurnNumber) {
            return;
        }

        pokemonInPlay.setDamageProtectionTurn(null);
        pokemonInPlay.setDamageProtectionThreshold(null);
        pokemonInPlayStateService.save(pokemonInPlay);
    }
}
