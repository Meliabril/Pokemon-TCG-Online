package ar.edu.utn.frc.tup.piii.services.game.outcome;

public interface VictoryConditionService {

    boolean attackerWinsAfterKnockout(int remainingPrizeCards, boolean defenderHasBenchedPokemon);
}
