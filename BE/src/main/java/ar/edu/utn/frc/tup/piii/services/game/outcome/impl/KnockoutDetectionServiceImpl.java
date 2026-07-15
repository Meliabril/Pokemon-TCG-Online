package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnockoutDetectionServiceImpl implements KnockoutDetectionService {

    private final CardService cardService;

    @Override
    public boolean isKnockedOut(PokemonInPlay pokemonInPlay) {
        if (pokemonInPlay == null || pokemonInPlay.getActiveCardInstance() == null) {
            return false;
        }

        Card topCard = cardService.getCardEntityById(pokemonInPlay.getActiveCardInstance().getCardId());
        if (topCard.getHp() == null) {
            return false;
        }

        int damageCounters = 0;
        if (pokemonInPlay.getDamageCounters() != null) {
            damageCounters = pokemonInPlay.getDamageCounters();
        }

        return damageCounters * 10 >= topCard.getHp();
    }
}
