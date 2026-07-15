package ar.edu.utn.frc.tup.piii.services.game.engine.impl;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRandomServiceImplTest {

    private final GameRandomServiceImpl service = new GameRandomServiceImpl();

    @Test
    void shuffledCopy_returnsAllElements() {
        List<Integer> source = List.of(1, 2, 3, 4, 5);
        List<Integer> result = service.shuffledCopy(source);
        assertThat(result).hasSize(5).containsExactlyInAnyOrderElementsOf(source);
    }

    @Test
    void shuffledCopy_doesNotMutateOriginal() {
        List<Integer> source = List.of(1, 2, 3);
        service.shuffledCopy(source);
        assertThat(source).containsExactly(1, 2, 3);
    }

    @Test
    void shuffledCopy_emptyList_returnsEmpty() {
        assertThat(service.shuffledCopy(List.of())).isEmpty();
    }

    @Test
    void chooseOne_nullSource_throws() {
        assertThatThrownBy(() -> service.chooseOne(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chooseOne_emptySource_throws() {
        assertThatThrownBy(() -> service.chooseOne(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chooseOne_singleElement_returnsThatElement() {
        assertThat(service.chooseOne(List.of("only"))).isEqualTo("only");
    }

    @RepeatedTest(10)
    void chooseOne_multipleElements_returnsElementFromList() {
        List<String> source = List.of("a", "b", "c");
        assertThat(service.chooseOne(source)).isIn("a", "b", "c");
    }

    @RepeatedTest(20)
    void flipCoin_returnsTrueOrFalse() {
        boolean result = service.flipCoin();
        assertThat(result).isIn(true, false);
    }
}
