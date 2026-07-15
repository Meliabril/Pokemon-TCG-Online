package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeckValidationServiceTest {

    private DeckValidationServiceImpl deckValidationService;

    @BeforeEach
    void setUp() {
        deckValidationService = new DeckValidationServiceImpl();
    }

    @Test
    void shouldAcceptValidXy1SixtyCardDeck() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Professor Sycamore", Card.XY1_SET_CODE, CardCategory.SUPPORTER_TRAINER), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 52));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldAcceptDeckWithXy1AndCustomProfessorCards() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 52));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectDeckWithDifferentSizeThanSixty() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck must contain exactly 60 cards");
    }

    @Test
    void shouldRejectCardsOutsidePlayableSets() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Potion", "base1", CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 52));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck can only contain cards from playable sets");
    }

    @Test
    void shouldRejectDeckWithoutBasicPokemon() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Potion", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 56));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck must contain at least 1 Basic Pokemon");
    }

    @Test
    void shouldRejectMoreThanFourCopiesByNameExceptBasicEnergy() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 5));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 55));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck can contain at most 4 copies of Pikachu");
        assertThat(result.errors()).doesNotContain("Deck can contain at most 4 copies of Water Energy");
    }

    @Test
    void shouldAcceptManyDifferentTrainerCardsUpToFourCopiesEach() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Evosoda", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Great Ball", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Max Revive", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Red Card", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Professor's Letter", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Roller Skates", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Super Potion", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Professor Sycamore", Card.XY1_SET_CODE, CardCategory.SUPPORTER_TRAINER), 4));
        deck.addCard(deckCard(card("Hard Charm", Card.XY1_SET_CODE, CardCategory.POKEMON_TOOL_TRAINER), 4));
        deck.addCard(deckCard(card("Shadow Circle", Card.XY1_SET_CODE, CardCategory.STADIUM_TRAINER), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 16));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectMoreThanFourCopiesOfTrainerByName() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Evosoda", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 5));
        deck.addCard(deckCard(card("Great Ball", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 47));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck can contain at most 4 copies of Evosoda");
        assertThat(result.errors()).doesNotContain("Deck can contain at most 4 copies of Great Ball");
    }

    @Test
    void shouldAcceptFourCopiesOfSpecialEnergyByName() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Rainbow Energy", Card.XY1_SET_CODE, CardCategory.SPECIAL_ENERGY), 4));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 52));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectMoreThanFourCopiesOfSpecialEnergyByName() {
        Deck deck = new Deck();
        deck.addCard(deckCard(card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON), 4));
        deck.addCard(deckCard(card("Rainbow Energy", Card.XY1_SET_CODE, CardCategory.SPECIAL_ENERGY), 5));
        deck.addCard(deckCard(card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY), 51));

        DeckValidationResult result = deckValidationService.validate(deck);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Deck can contain at most 4 copies of Rainbow Energy");
        assertThat(result.errors()).doesNotContain("Deck can contain at most 4 copies of Water Energy");
    }

    @Test
    void shouldPreserveValidationOrderForPersistedSummary() {
        DeckValidationResult result = deckValidationService.validate(
                new DeckValidationSummary(59, true, false, List.of("Pikachu")));

        assertThat(result.errors()).containsExactly(
                "Deck must contain exactly 60 cards",
                "Deck can only contain cards from playable sets",
                "Deck must contain at least 1 Basic Pokemon",
                "Deck can contain at most 4 copies of Pikachu");
    }

    private DeckCard deckCard(Card card, int quantity) {
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private Card card(String name, String setCode, CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(UUID.randomUUID().toString());
        card.setSetCode(setCode);
        card.setSetName("XY");
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(supertype(category));
        card.setCategory(category);
        if (category == CardCategory.BASIC_POKEMON) {
            card.setSubtype("Basic");
        }
        card.setRawJson("{}");
        return card;
    }

    private CardSupertype supertype(CardCategory category) {
        if (category.name().contains("POKEMON")) {
            return CardSupertype.POKEMON;
        }
        if (category.name().contains("ENERGY")) {
            return CardSupertype.ENERGY;
        }
        return CardSupertype.TRAINER;
    }
}
