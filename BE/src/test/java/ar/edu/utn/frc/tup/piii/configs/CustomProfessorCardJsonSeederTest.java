package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.services.card.CustomProfessorCardJsonSeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomProfessorCardJsonSeederTest {

    @Mock
    private CustomProfessorCardJsonSeedService customProfessorCardJsonSeedService;

    @Mock
    private Environment environment;

    private CustomProfessorCardJsonSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new CustomProfessorCardJsonSeeder(customProfessorCardJsonSeedService, environment);
    }

    @Test
    void shouldSeedCustomProfessorCardsWhenDatasourceDriverIsH2() {
        when(environment.getProperty("spring.datasource.driver-class-name", "")).thenReturn("org.h2.Driver");
        when(environment.getProperty("spring.datasource.url", "")).thenReturn("jdbc:postgresql://example");

        seeder.run();

        verify(customProfessorCardJsonSeedService).seedCustomProfessorCards();
    }

    @Test
    void shouldSeedCustomProfessorCardsWhenDatasourceUrlIsH2() {
        when(environment.getProperty("spring.datasource.driver-class-name", "")).thenReturn("org.postgresql.Driver");
        when(environment.getProperty("spring.datasource.url", "")).thenReturn("jdbc:h2:mem:pokemon_tcg");

        seeder.run();

        verify(customProfessorCardJsonSeedService).seedCustomProfessorCards();
    }

    @Test
    void shouldSkipCustomProfessorCardsWhenDatasourceIsNotH2() {
        when(environment.getProperty("spring.datasource.driver-class-name", "")).thenReturn("org.postgresql.Driver");
        when(environment.getProperty("spring.datasource.url", "")).thenReturn("jdbc:postgresql://example");

        seeder.run();

        verify(customProfessorCardJsonSeedService, never()).seedCustomProfessorCards();
    }

    @Test
    void shouldNotPropagateSeedFailure() {
        when(environment.getProperty("spring.datasource.driver-class-name", "")).thenReturn("org.h2.Driver");
        when(environment.getProperty("spring.datasource.url", "")).thenReturn("jdbc:h2:mem:pokemon_tcg");
        when(customProfessorCardJsonSeedService.seedCustomProfessorCards())
                .thenThrow(new IllegalStateException("JSON unavailable"));

        assertThatCode(() -> seeder.run()).doesNotThrowAnyException();
    }
}
