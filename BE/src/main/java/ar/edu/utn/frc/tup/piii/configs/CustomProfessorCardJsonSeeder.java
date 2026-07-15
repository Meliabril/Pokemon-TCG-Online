package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.services.card.CustomProfessorCardJsonSeedService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class CustomProfessorCardJsonSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomProfessorCardJsonSeeder.class);
    private static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String H2_JDBC_PREFIX = "jdbc:h2:";
    private static final String DATASOURCE_DRIVER_PROPERTY = "spring.datasource.driver-class-name";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";

    private final CustomProfessorCardJsonSeedService customProfessorCardJsonSeedService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (!isH2Datasource()) {
            LOGGER.info("Custom professor JSON seed skipped: datasource is not H2");
            return;
        }

        try {
            int seededCards = customProfessorCardJsonSeedService.seedCustomProfessorCards();
            LOGGER.info("Custom professor JSON seed finished: {} cards ready", seededCards);
        } catch (Exception exception) {
            LOGGER.warn("Custom professor JSON seed failed during startup: {}", exception.getMessage(), exception);
        }
    }

    private boolean isH2Datasource() {
        String driverClassName = environment.getProperty(DATASOURCE_DRIVER_PROPERTY, "");
        String datasourceUrl = environment.getProperty(DATASOURCE_URL_PROPERTY, "");
        return H2_DRIVER_CLASS_NAME.equals(driverClassName) || datasourceUrl.startsWith(H2_JDBC_PREFIX);
    }
}
