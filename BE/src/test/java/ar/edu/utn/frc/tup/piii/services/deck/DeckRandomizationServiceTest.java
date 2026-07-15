package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckRandomizationServiceImpl;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckValidationServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeckRandomizationServiceTest {

    @Test
    void shouldGenerateCompositionWithoutReloadingSelectedCards() {
        CardService cardService = mock(CardService.class);
        DeckRandomizationServiceImpl randomizationService = new DeckRandomizationServiceImpl(
                cardService,
                new DeckValidationServiceImpl());
        List<Card> cards = randomizableCards();
        when(cardService.getCardEntities(Card.XY1_SET_CODE)).thenReturn(cards);

        RandomDeckComposition composition = randomizationService.generateRandomDeck();

        assertThat(composition.cards())
                .isNotEmpty()
                .allSatisfy(entry -> assertThat(cards).contains(entry.card()));
        assertThat(composition.cards())
                .allSatisfy(entry -> assertThat(entry.card().getSetCode()).isEqualTo(Card.XY1_SET_CODE));
        assertThat(composition.cards()).extracting(RandomDeckComposition.CardEntry::quantity)
                .allMatch(quantity -> quantity > 0);
        verify(cardService).getCardEntities(Card.XY1_SET_CODE);
        verify(cardService, never()).getCardEntityById(any(UUID.class));
    }

    private List<Card> randomizableCards() {
        List<Card> cards = new ArrayList<>();
        addCards(cards, "Basic", CardCategory.BASIC_POKEMON, 5);
        addCards(cards, "Supporter", CardCategory.SUPPORTER_TRAINER, 3);
        addCards(cards, "Item", CardCategory.ITEM_TRAINER, 6);
        addCards(cards, "Tool", CardCategory.POKEMON_TOOL_TRAINER, 2);
        addCards(cards, "Stadium", CardCategory.STADIUM_TRAINER, 2);
        cards.add(card("Water Energy", CardCategory.BASIC_ENERGY));
        return cards;
    }

    private void addCards(List<Card> cards, String prefix, CardCategory category, int quantity) {
        for (int index = 1; index <= quantity; index++) {
            cards.add(card(prefix + " " + index, category));
        }
    }

    private Card card(String name, CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(UUID.randomUUID().toString());
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(supertype(category));
        card.setCategory(category);
        card.setRawJson("{}");
        if (category == CardCategory.BASIC_POKEMON) {
            card.setPokemonType("Water");
            card.setSubtype("Basic");
        }
        return card;
    }

    private CardSupertype supertype(CardCategory category) {
        if (category == CardCategory.BASIC_POKEMON) {
            return CardSupertype.POKEMON;
        }
        if (category == CardCategory.BASIC_ENERGY) {
            return CardSupertype.ENERGY;
        }
        return CardSupertype.TRAINER;
    }
}
