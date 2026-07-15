package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AbilityCatalogServiceImpl implements AbilityCatalogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbilityCatalogServiceImpl.class);
    private static final String ABILITIES_RESOURCE = "game-engine/xy1-abilities.json";

    private final Map<String, List<AbilityDefinition>> abilitiesByExternalId;

    public AbilityCatalogServiceImpl(ObjectMapper objectMapper) {
        this.abilitiesByExternalId = loadAbilities(objectMapper);
    }

    @Override
    public List<AbilityDefinition> abilitiesFor(String cardExternalId) {
        if (cardExternalId == null || cardExternalId.isBlank()) {
            return List.of();
        }
        return abilitiesByExternalId.getOrDefault(cardExternalId, List.of());
    }

    @Override
    public Optional<AbilityDefinition> find(String cardExternalId, AbilityCode abilityCode) {
        if (abilityCode == null) {
            return Optional.empty();
        }
        return abilitiesFor(cardExternalId).stream()
                .filter(ability -> abilityCode.equals(ability.code()))
                .findFirst();
    }

    private Map<String, List<AbilityDefinition>> loadAbilities(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(ABILITIES_RESOURCE);
        if (!resource.exists()) {
            LOGGER.warn("Ability catalog resource {} was not found. Pokemon abilities will be empty.", ABILITIES_RESOURCE);
            return Map.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, List<AbilityDefinition>> abilities = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            if (abilities == null) {
                return Map.of();
            }
            LOGGER.info("Loaded {} normalized ability definitions from {}", abilities.size(), ABILITIES_RESOURCE);
            return Map.copyOf(abilities);
        } catch (IOException exception) {
            LOGGER.warn("Could not read ability catalog resource {}. Pokemon abilities will be empty.", ABILITIES_RESOURCE, exception);
            return Map.of();
        }
    }
}
