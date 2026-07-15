package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.OutgoingDamageReductionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutgoingDamageReductionServiceImpl implements OutgoingDamageReductionService {

    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public int consumeReduction(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer reducedTurn = pokemonInPlay.getDamageReductionNextTurn();
        Integer reductionAmount = pokemonInPlay.getDamageReductionAmount();
        if (reducedTurn == null || reducedTurn != currentTurnNumber || reductionAmount == null || reductionAmount <= 0) {
            return 0;
        }

        pokemonInPlay.setDamageReductionNextTurn(null);
        pokemonInPlay.setDamageReductionAmount(null);
        pokemonInPlayStateService.save(pokemonInPlay);
        return reductionAmount;
    }

    @Override
    public void expireReduction(PokemonInPlay pokemonInPlay, int currentTurnNumber) {
        Integer reducedTurn = pokemonInPlay.getDamageReductionNextTurn();
        if (reducedTurn == null || reducedTurn > currentTurnNumber) {
            return;
        }

        pokemonInPlay.setDamageReductionNextTurn(null);
        pokemonInPlay.setDamageReductionAmount(null);
        pokemonInPlayStateService.save(pokemonInPlay);
    }
}
