package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.card.AttackCostDto;
import ar.edu.utn.frc.tup.piii.dtos.card.AttackDto;
import ar.edu.utn.frc.tup.piii.dtos.card.AttackTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.AbilityTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardAbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardRelationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.card.TypeValueTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CardMapper {

    private static final AbilityCatalogService EMPTY_ABILITY_CATALOG_SERVICE = new AbilityCatalogService() {
        @Override
        public List<AbilityDefinition> abilitiesFor(String cardExternalId) {
            return List.of();
        }

        @Override
        public Optional<AbilityDefinition> find(String cardExternalId, AbilityCode abilityCode) {
            return Optional.empty();
        }
    };

    private final CardTranslationService cardTranslationService;
    private final AbilityCatalogService abilityCatalogService;
    private final ObjectMapper objectMapper;

    public CardMapper(CardTranslationService cardTranslationService) {
        this(cardTranslationService, EMPTY_ABILITY_CATALOG_SERVICE, new ObjectMapper());
    }

    /*
     * Entities use Sets to keep Hibernate fetch graphs safe. The API contract
     * still returns lists, so every nested collection is sorted here for stable
     * Postman/Swagger responses.
     */
    public CardResponseDto toDto(Card card) {
        CardTranslationDto translation = cardTranslationService.findByExternalId(card.getExternalId()).orElse(null);
        CardTranslationDto englishTranslation = cardTranslationService.findEnglishByExternalId(card.getExternalId()).orElse(null);
        return new CardResponseDto(
                card.getId(),
                card.getExternalId(),
                card.getSetCode(),
                card.getSetName(),
                card.getNumber(),
                card.getName(),
                card.getSupertype(),
                card.getCategory(),
                card.getSubtype(),
                card.getEvolvesFrom(),
                card.getHp(),
                card.getPokemonType(),
                card.getRetreatCost(),
                card.getImageSmallUrl(),
                card.getImageLargeUrl(),
                attacks(card, translation, englishTranslation),
                weaknesses(card, translation),
                resistances(card, translation),
                displayName(translation),
                displayPokemonType(translation),
                displaySupertype(translation),
                displaySubtypes(translation),
                abilities(card, translation),
                displayAbilities(translation),
                rules(card),
                displayRules(translation));
    }

    private List<AttackDto> attacks(
            Card card,
            CardTranslationDto translation,
            CardTranslationDto englishTranslation) {
        Map<Integer, AttackTranslationDto> translationsByOrder = attackTranslationsByOrder(translation);
        Map<Integer, AttackTranslationDto> englishTranslationsByOrder = attackTranslationsByOrder(englishTranslation);
        return card.getAttacks().stream()
                .sorted(Comparator.comparingInt(Attack::getAttackOrder))
                .map(attack -> attack(
                        attack,
                        translationsByOrder.get(attack.getAttackOrder()),
                        englishTranslationsByOrder.get(attack.getAttackOrder())))
                .toList();
    }

    private AttackDto attack(
            Attack attack,
            AttackTranslationDto translation,
            AttackTranslationDto englishTranslation) {
        return new AttackDto(
                attack.getId(),
                attack.getName(),
                attack.getDamageText(),
                attack.getBaseDamage(),
                attack.getEffectText(),
                attack.getAttackOrder(),
                attack.getCosts().stream()
                        .sorted(Comparator.comparing(AttackCost::getEnergyType))
                        .map(this::cost)
                        .toList(),
                translation == null ? null : translation.displayName(),
                translation == null ? null : translation.displayCost(),
                translation == null ? null : translation.displayText(),
                englishTranslation == null ? null : englishTranslation.displayText());
    }


    private List<CardAbilityDto> abilities(Card card, CardTranslationDto translation) {
        List<RawAbility> rawAbilities = rawAbilities(card);
        if (rawAbilities.isEmpty()) {
            return List.of();
        }

        List<AbilityTranslationDto> translations = displayAbilities(translation);
        return java.util.stream.IntStream.range(0, rawAbilities.size())
                .mapToObj(index -> ability(card, rawAbilities.get(index), translationAt(translations, index)))
                .toList();
    }

    private CardAbilityDto ability(Card card, RawAbility rawAbility, AbilityTranslationDto translation) {
        String abilityId = normalizeAbilityId(rawAbility.name());
        AbilityCode abilityCode = abilityCode(abilityId);
        AbilityDefinition definition = abilityCode == null
                ? null
                : abilityCatalogService.find(card.getExternalId(), abilityCode).orElse(null);

        return new CardAbilityDto(
                abilityId,
                abilityCode,
                rawAbility.name(),
                rawAbility.type(),
                translation == null ? null : translation.displayName(),
                translation == null ? null : translation.displayType(),
                rawAbility.text(),
                translation == null ? null : translation.displayText(),
                definition == null ? null : definition.activation(),
                definition == null ? null : definition.timing(),
                definition != null && definition.oncePerTurn(),
                definition != null);
    }

    private List<RawAbility> rawAbilities(Card card) {
        if (card.getRawJson() == null || card.getRawJson().isBlank()) {
            return List.of();
        }

        try {
            JsonNode abilitiesNode = objectMapper.readTree(card.getRawJson()).path("abilities");
            if (!abilitiesNode.isArray()) {
                return List.of();
            }

            List<RawAbility> abilities = new ArrayList<>();
            for (JsonNode abilityNode : abilitiesNode) {
                String name = textValue(abilityNode, "name");
                String text = textValue(abilityNode, "text");
                String type = textValue(abilityNode, "type");
                if (name != null && !name.isBlank()) {
                    abilities.add(new RawAbility(name, type, text));
                }
            }
            return List.copyOf(abilities);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return List.of();
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private List<String> rules(Card card) {
        if (card.getRawJson() == null || card.getRawJson().isBlank()) {
            return List.of();
        }

        try {
            JsonNode rulesNode = objectMapper.readTree(card.getRawJson()).path("rules");
            if (!rulesNode.isArray()) {
                return List.of();
            }

            List<String> rules = new ArrayList<>();
            for (JsonNode ruleNode : rulesNode) {
                if (ruleNode.isTextual() && !ruleNode.asText().isBlank()) {
                    rules.add(ruleNode.asText());
                }
            }
            return List.copyOf(rules);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            return List.of();
        }
    }

    private String normalizeAbilityId(String abilityName) {
        if (abilityName == null || abilityName.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(abilityName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private AbilityCode abilityCode(String abilityId) {
        if (abilityId == null) {
            return null;
        }

        try {
            return AbilityCode.valueOf(abilityId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AttackCostDto cost(AttackCost cost) {
        return new AttackCostDto(cost.getEnergyType(), cost.getQuantity());
    }

    private List<CardRelationDto> weaknesses(Card card, CardTranslationDto translation) {
        List<TypeValueTranslationDto> relationTranslations = relationTranslations(translation)
                .map(CardTranslationDto::displayWeaknesses)
                .orElse(List.of());
        List<CardWeakness> weaknesses = card.getWeaknesses().stream()
                .sorted(Comparator.comparing(CardWeakness::getEnergyType))
                .toList();
        return indexedWeaknesses(weaknesses, relationTranslations);
    }

    private List<CardRelationDto> indexedWeaknesses(
            List<CardWeakness> weaknesses,
            List<TypeValueTranslationDto> translations) {
        return java.util.stream.IntStream.range(0, weaknesses.size())
                .mapToObj(index -> weakness(weaknesses.get(index), translationAt(translations, index)))
                .toList();
    }

    private CardRelationDto weakness(CardWeakness weakness, TypeValueTranslationDto translation) {
        return new CardRelationDto(
                weakness.getEnergyType(),
                weakness.getMultiplier(),
                translation == null ? null : translation.type(),
                translation == null ? null : translation.value());
    }

    private List<CardRelationDto> resistances(Card card, CardTranslationDto translation) {
        List<TypeValueTranslationDto> relationTranslations = relationTranslations(translation)
                .map(CardTranslationDto::displayResistances)
                .orElse(List.of());
        List<CardResistance> resistances = card.getResistances().stream()
                .sorted(Comparator.comparing(CardResistance::getEnergyType))
                .toList();
        return indexedResistances(resistances, relationTranslations);
    }

    private List<CardRelationDto> indexedResistances(
            List<CardResistance> resistances,
            List<TypeValueTranslationDto> translations) {
        return java.util.stream.IntStream.range(0, resistances.size())
                .mapToObj(index -> resistance(resistances.get(index), translationAt(translations, index)))
                .toList();
    }

    private CardRelationDto resistance(CardResistance resistance, TypeValueTranslationDto translation) {
        return new CardRelationDto(
                resistance.getEnergyType(),
                resistance.getValue(),
                translation == null ? null : translation.type(),
                translation == null ? null : translation.value());
    }

    private Map<Integer, AttackTranslationDto> attackTranslationsByOrder(CardTranslationDto translation) {
        if (translation == null || translation.displayAttacks() == null) {
            return Map.of();
        }
        return translation.displayAttacks().stream()
                .collect(Collectors.toMap(
                        AttackTranslationDto::order,
                        Function.identity(),
                        (existing, replacement) -> existing));
    }

    private <T> T translationAt(List<T> translations, int index) {
        if (translations == null || index < 0 || index >= translations.size()) {
            return null;
        }
        return translations.get(index);
    }

    private Optional<CardTranslationDto> relationTranslations(CardTranslationDto translation) {
        return Optional.ofNullable(translation);
    }

    private String displayName(CardTranslationDto translation) {
        return translation == null ? null : translation.displayName();
    }

    private String displayPokemonType(CardTranslationDto translation) {
        return translation == null ? null : translation.displayPokemonType();
    }

    private String displaySupertype(CardTranslationDto translation) {
        return translation == null ? null : translation.displaySupertype();
    }

    private List<String> displaySubtypes(CardTranslationDto translation) {
        return translation == null ? null : translation.displaySubtypes();
    }

    private List<ar.edu.utn.frc.tup.piii.dtos.card.AbilityTranslationDto> displayAbilities(CardTranslationDto translation) {
        return translation == null ? null : translation.displayAbilities();
    }

    private List<String> displayRules(CardTranslationDto translation) {
        return translation == null ? null : translation.displayRules();
    }

    private record RawAbility(String name, String type, String text) {
    }
}
