package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardTranslationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardTranslationServiceTest {

    @Test
    void shouldLoadSpanishTranslationsByExternalId() {
        CardTranslationService service = new CardTranslationService(new ObjectMapper());

        assertThat(service.findByExternalId("xy1-1"))
                .isPresent()
                .get()
                .satisfies(translation -> {
                    assertThat(translation.displayName()).isEqualTo("Venusaur-EX");
                    assertThat(translation.displayAttacks()).isNotEmpty();
                });
    }

    @Test
    void shouldLoadCustomProfessorAttackDescriptionsInSpanishAndEnglish() {
        CardTranslationService service = new CardTranslationService(new ObjectMapper());

        List<CardTranslationDto> spanishTranslations = customProfessorTranslations(service, false);
        List<CardTranslationDto> englishTranslations = customProfessorTranslations(service, true);

        assertThat(spanishTranslations).hasSize(5);
        assertThat(englishTranslations).hasSize(5);
        assertThat(spanishTranslations)
                .flatExtracting(CardTranslationDto::displayAttacks)
                .hasSize(10)
                .allSatisfy(attack -> assertThat(attack.displayText()).isNotBlank())
                .extracting(attack -> attack.displayText())
                .contains(
                        "AngularQuin descarga una explicación veloz potenciada por apuntes, diapos y NotebookLM.",
                        "Concentra todo su poder en una única instancia imposible de contrarrestar.");
        assertThat(englishTranslations)
                .flatExtracting(CardTranslationDto::displayAttacks)
                .hasSize(10)
                .allSatisfy(attack -> assertThat(attack.displayText()).isNotBlank())
                .extracting(attack -> attack.displayText())
                .contains(
                        "AngularQuín unleashes a swift explanation powered by notes, slides, and NotebookLM.",
                        "It concentrates all its power into a single instance that is impossible to counter.");
    }

    @Test
    void shouldReturnEmptyWhenExternalIdIsUnknown() {
        CardTranslationService service = CardTranslationService.empty();

        assertThat(service.findByExternalId("missing-card")).isEmpty();
        assertThat(service.findEnglishByExternalId("missing-card")).isEmpty();
    }

    private List<CardTranslationDto> customProfessorTranslations(CardTranslationService service, boolean english) {
        return List.of(
                customProfessorTranslation(service, english, "custom-professors-pr-001"),
                customProfessorTranslation(service, english, "custom-professors-pr-002"),
                customProfessorTranslation(service, english, "custom-professors-pr-003"),
                customProfessorTranslation(service, english, "custom-professors-pr-004"),
                customProfessorTranslation(service, english, "custom-professors-pr-005"));
    }

    private CardTranslationDto customProfessorTranslation(
            CardTranslationService service,
            boolean english,
            String externalId) {
        return english
                ? service.findEnglishByExternalId(externalId).orElseThrow()
                : service.findByExternalId(externalId).orElseThrow();
    }
}