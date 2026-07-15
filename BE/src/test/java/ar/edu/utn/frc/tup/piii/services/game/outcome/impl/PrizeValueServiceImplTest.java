package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrizeValueServiceImplTest {

    private final PrizeValueServiceImpl service = new PrizeValueServiceImpl();

    @Test
    void prizeCardsFor_nullCard_returnsOne() {
        assertThat(service.prizeCardsFor(null)).isEqualTo(1);
    }

    @Test
    void prizeCardsFor_nullCategory_returnsOne() {
        Card card = new Card();
        assertThat(service.prizeCardsFor(card)).isEqualTo(1);
    }

    @Test
    void prizeCardsFor_pokemonEx_returnsTwo() {
        Card card = new Card();
        card.setCategory(CardCategory.POKEMON_EX);
        assertThat(service.prizeCardsFor(card)).isEqualTo(2);
    }

    @Test
    void prizeCardsFor_megaPokemon_returnsTwo() {
        Card card = new Card();
        card.setCategory(CardCategory.MEGA_POKEMON);
        assertThat(service.prizeCardsFor(card)).isEqualTo(2);
    }

    @Test
    void prizeCardsFor_basicPokemon_returnsOne() {
        Card card = new Card();
        card.setCategory(CardCategory.BASIC_POKEMON);
        assertThat(service.prizeCardsFor(card)).isEqualTo(1);
    }

    @Test
    void prizeCardsFor_itemTrainer_returnsOne() {
        Card card = new Card();
        card.setCategory(CardCategory.ITEM_TRAINER);
        assertThat(service.prizeCardsFor(card)).isEqualTo(1);
    }
}
