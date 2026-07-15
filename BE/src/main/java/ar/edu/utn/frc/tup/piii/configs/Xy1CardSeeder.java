package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Component
@Order(0)
@ConditionalOnProperty(
        name = "app.cards.seed.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class Xy1CardSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Xy1CardSeeder.class);

    private final CardImportService cardImportService;

    public Xy1CardSeeder(CardImportService cardImportService) {
        this.cardImportService = cardImportService;
    }

    @Override
    public void run(String... args) {
        try {
            CardImportStatusDto status = cardImportService.getXy1Status();
            if (status.complete()) {
                LOGGER.info(
                        "XY1 card import skipped: {}/{} cards already loaded",
                        status.importedCards(),
                        status.expectedCards());
                return;
            }

            LOGGER.info(
                    "XY1 card import starting: {}/{} cards loaded",
                    status.importedCards(),
                    status.expectedCards());
            CardImportResultDto result = cardImportService.importXy1();
            LOGGER.info(
                    "XY1 card import finished: setCode={}, importedCards={}, complete={}, message={}",
                    result.setCode(),
                    result.importedCards(),
                    result.complete(),
                    result.message());
        } catch (Exception exception) {
            LOGGER.warn("XY1 card import failed during startup: {}", exception.getMessage(), exception);
        }
    }
}
