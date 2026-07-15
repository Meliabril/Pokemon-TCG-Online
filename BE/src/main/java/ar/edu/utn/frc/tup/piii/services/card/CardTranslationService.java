package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardTranslationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Service
public class CardTranslationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardTranslationService.class);
    private static final String SPANISH_TRANSLATIONS_RESOURCE = "i18n/cards-es.json";
    private static final String ENGLISH_TRANSLATIONS_RESOURCE = "i18n/cards-en.json";

    private final Map<String, CardTranslationDto> spanishTranslationsByExternalId;
    private final Map<String, CardTranslationDto> englishTranslationsByExternalId;

    @Autowired
    public CardTranslationService(ObjectMapper objectMapper) {
        spanishTranslationsByExternalId = loadTranslations(objectMapper, SPANISH_TRANSLATIONS_RESOURCE, "Spanish");
        englishTranslationsByExternalId = loadTranslations(objectMapper, ENGLISH_TRANSLATIONS_RESOURCE, "English");
    }

    private CardTranslationService(
            Map<String, CardTranslationDto> spanishTranslationsByExternalId,
            Map<String, CardTranslationDto> englishTranslationsByExternalId) {
        this.spanishTranslationsByExternalId = Map.copyOf(spanishTranslationsByExternalId);
        this.englishTranslationsByExternalId = Map.copyOf(englishTranslationsByExternalId);
    }

    public static CardTranslationService empty() {
        return new CardTranslationService(Map.of(), Map.of());
    }

    public Optional<CardTranslationDto> findByExternalId(String externalId) {
        return findTranslation(spanishTranslationsByExternalId, externalId);
    }

    public Optional<CardTranslationDto> findEnglishByExternalId(String externalId) {
        return findTranslation(englishTranslationsByExternalId, externalId);
    }

    private Optional<CardTranslationDto> findTranslation(
            Map<String, CardTranslationDto> translationsByExternalId,
            String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(translationsByExternalId.get(externalId));
    }

    private Map<String, CardTranslationDto> loadTranslations(
            ObjectMapper objectMapper,
            String translationsResource,
            String languageLabel) {
        ClassPathResource resource = new ClassPathResource(translationsResource);
        if (!resource.exists()) {
            LOGGER.warn("Card translation resource {} was not found. {} card display fields will be empty.", translationsResource, languageLabel);
            return Map.of();
        }

        try {
            if (resource.contentLength() == 0) {
                LOGGER.warn("Card translation resource {} is empty. {} card display fields will be empty.", translationsResource, languageLabel);
                return Map.of();
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not inspect card translation resource {}. {} card display fields will be empty.", translationsResource, languageLabel, exception);
            return Map.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, CardTranslationDto> translations = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            if (translations == null) {
                LOGGER.warn("Card translation resource {} returned null content. {} card display fields will be empty.", translationsResource, languageLabel);
                return Map.of();
            }
            LOGGER.info("Loaded {} card translations from {}", translations.size(), translationsResource);
            return Map.copyOf(translations);
        } catch (IOException exception) {
            LOGGER.warn("Could not read card translation resource {}. {} card display fields will be empty.", translationsResource, languageLabel, exception);
            return Map.of();
        }
    }
}