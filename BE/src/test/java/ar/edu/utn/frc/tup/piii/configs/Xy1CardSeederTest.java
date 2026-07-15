package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Xy1CardSeederTest {

    @Mock
    private CardImportService cardImportService;

    private Xy1CardSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new Xy1CardSeeder(cardImportService);
    }

    @Test
    void shouldSkipImportWhenXy1CardsAreComplete() {
        when(cardImportService.getXy1Status())
                .thenReturn(new CardImportStatusDto(Card.XY1_SET_CODE, 146, 146, true));

        seeder.run();

        verify(cardImportService, never()).importXy1();
    }

    @Test
    void shouldImportXy1CardsWhenStatusIsIncomplete() {
        when(cardImportService.getXy1Status())
                .thenReturn(new CardImportStatusDto(Card.XY1_SET_CODE, 0, 146, false));
        when(cardImportService.importXy1())
                .thenReturn(new CardImportResultDto(Card.XY1_SET_CODE, 146, true, "XY1 import completed"));

        seeder.run();

        verify(cardImportService).importXy1();
    }

    @Test
    void shouldNotPropagateImportFailure() {
        when(cardImportService.getXy1Status())
                .thenReturn(new CardImportStatusDto(Card.XY1_SET_CODE, 0, 146, false));
        when(cardImportService.importXy1())
                .thenThrow(new IllegalStateException("Pokemon TCG API unavailable"));

        assertThatCode(() -> seeder.run()).doesNotThrowAnyException();
    }
}
