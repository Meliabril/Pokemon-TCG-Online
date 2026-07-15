package ar.edu.utn.frc.tup.piii.services.game.outcome;

import ar.edu.utn.frc.tup.piii.entities.Card;

public interface PrizeValueService {

    int prizeCardsFor(Card knockedOutPokemonCard);
}
