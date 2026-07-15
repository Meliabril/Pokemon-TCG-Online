package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.CardImportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Xy1CardImportValidatorTest {

    private Xy1CardImportValidator validator;

    @BeforeEach
    void setUp() {
        validator = new Xy1CardImportValidator();
    }

    @Test
    void shouldAcceptExactlyOneHundredFortySixXy1Cards() {
        List<PokemonTcgCardPayload> cards = cards(146, Card.XY1_SET_CODE);

        assertThatCode(() -> validator.validateFetchedCards(cards))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectIncompleteXy1Import() {
        List<PokemonTcgCardPayload> cards = cards(145, Card.XY1_SET_CODE);

        assertThatThrownBy(() -> validator.validateFetchedCards(cards))
                .isInstanceOf(CardImportException.class)
                .hasMessageContaining("exactly 146 cards");
    }

    @Test
    void shouldRejectCardsFromAnotherSet() {
        List<PokemonTcgCardPayload> cards = cards(146, "base1");

        assertThatThrownBy(() -> validator.validateFetchedCards(cards))
                .isInstanceOf(CardImportException.class)
                .hasMessage("Only cards from set xy1 can be imported");
    }

    @Test
    void shouldReportImportCompletenessOnlyAtExpectedCount() {
        assertThat(validator.isComplete(146)).isTrue();
        assertThat(validator.isComplete(145)).isFalse();
        assertThat(validator.isComplete(147)).isFalse();
    }

    private List<PokemonTcgCardPayload> cards(int count, String setCode) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(index -> card(index, setCode))
                .toList();
    }

    private PokemonTcgCardPayload card(int index, String setCode) {
        return new PokemonTcgCardPayload(
                "xy1-" + index,
                setCode,
                "XY",
                String.valueOf(index),
                "Card " + index,
                CardSupertype.POKEMON,
                CardCategory.BASIC_POKEMON,
                "Basic",
                null,
                60,
                "Lightning",
                1,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                List.of());
    }
}
