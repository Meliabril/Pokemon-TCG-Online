package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeValueService;
import org.springframework.stereotype.Service;

@Service
public class PrizeValueServiceImpl implements PrizeValueService {

    @Override
    public int prizeCardsFor(Card knockedOutPokemonCard) {
        if (knockedOutPokemonCard == null || knockedOutPokemonCard.getCategory() == null) {
            return 1;
        }
        if (CardCategory.POKEMON_EX.equals(knockedOutPokemonCard.getCategory())) {
            return 2;
        }
        if (CardCategory.MEGA_POKEMON.equals(knockedOutPokemonCard.getCategory())) {
            return 2;
        }

        return 1;
    }
}
