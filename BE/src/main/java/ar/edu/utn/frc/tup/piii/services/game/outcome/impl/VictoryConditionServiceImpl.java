package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.services.game.outcome.VictoryConditionService;
import org.springframework.stereotype.Service;

@Service
public class VictoryConditionServiceImpl implements VictoryConditionService {

    @Override
    public boolean attackerWinsAfterKnockout(int remainingPrizeCards, boolean defenderHasBenchedPokemon) {
        return remainingPrizeCards == 0 || !defenderHasBenchedPokemon;
    }
}
