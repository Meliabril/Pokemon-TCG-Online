package ar.edu.utn.frc.tup.piii.services.card.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.CardImportException;
import ar.edu.utn.frc.tup.piii.services.card.PokemonTcgApiService;
import ar.edu.utn.frc.tup.piii.services.card.PokemonTcgCardPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PokemonTcgApiServiceImpl implements PokemonTcgApiService {

    private static final String XY1_URL = "https://api.pokemontcg.io/v2/cards?q=set.id:xy1&pageSize=250";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PokemonTcgApiServiceImpl(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PokemonTcgCardPayload> fetchXy1Cards() {
        JsonNode root = restClient.get()
                .uri(XY1_URL)
                .retrieve()
                .body(JsonNode.class);
        if (root == null || !root.path("data").isArray()) {
            throw new CardImportException("Pokemon TCG API returned an invalid response");
        }
        List<PokemonTcgCardPayload> cards = new ArrayList<>();
        for (JsonNode cardNode : root.path("data")) {
            cards.add(toPayload(cardNode));
        }
        return cards;
    }

    private PokemonTcgCardPayload toPayload(JsonNode node) {
        CardSupertype supertype = supertype(node.path("supertype").asText());
        List<String> subtypes = strings(node.path("subtypes"));
        return new PokemonTcgCardPayload(
                text(node, "id"),
                node.path("set").path("id").asText(Card.XY1_SET_CODE),
                node.path("set").path("name").asText("XY"),
                text(node, "number"),
                text(node, "name"),
                supertype,
                category(supertype, subtypes),
                subtypes.isEmpty() ? null : subtypes.get(0),
                nullableText(node, "evolvesFrom"),
                integerText(node, "hp"),
                firstOrNull(strings(node.path("types"))),
                node.path("retreatCost").isArray() ? node.path("retreatCost").size() : null,
                node.path("images").path("small").asText(null),
                node.path("images").path("large").asText(null),
                raw(node),
                attacks(node.path("attacks")),
                relations(node.path("weaknesses")),
                relations(node.path("resistances")));
    }

    private List<PokemonTcgCardPayload.AttackPayload> attacks(JsonNode attacksNode) {
        List<PokemonTcgCardPayload.AttackPayload> attacks = new ArrayList<>();
        if (!attacksNode.isArray()) {
            return attacks;
        }
        int order = 0;
        for (JsonNode attackNode : attacksNode) {
            attacks.add(new PokemonTcgCardPayload.AttackPayload(
                    text(attackNode, "name"),
                    nullableText(attackNode, "damage"),
                    parseLeadingDamage(nullableText(attackNode, "damage")),
                    nullableText(attackNode, "text"),
                    order++,
                    costs(attackNode.path("cost"))));
        }
        return attacks;
    }

    private Map<String, Integer> costs(JsonNode costNode) {
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (!costNode.isArray()) {
            return costs;
        }
        for (JsonNode node : costNode) {
            String energyType = node.asText();
            costs.merge(energyType, 1, Integer::sum);
        }
        return costs;
    }

    private List<PokemonTcgCardPayload.CardRelationPayload> relations(JsonNode relationNode) {
        List<PokemonTcgCardPayload.CardRelationPayload> relations = new ArrayList<>();
        if (!relationNode.isArray()) {
            return relations;
        }
        for (JsonNode node : relationNode) {
            relations.add(new PokemonTcgCardPayload.CardRelationPayload(
                    text(node, "type"),
                    text(node, "value")));
        }
        return relations;
    }

    private CardSupertype supertype(String value) {
        String normalized = normalizeCardText(value);
        if ("POKEMON".equals(normalized)) {
            return CardSupertype.POKEMON;
        }
        if ("TRAINER".equals(normalized)) {
            return CardSupertype.TRAINER;
        }
        return CardSupertype.ENERGY;
    }

    private CardCategory category(CardSupertype supertype, List<String> subtypes) {
        List<String> normalized = subtypes.stream()
                .map(this::normalizeCardText)
                .toList();
        if (supertype == CardSupertype.POKEMON) {
            if (normalized.contains("MEGA")) {
                return CardCategory.MEGA_POKEMON;
            }
            if (normalized.contains("EX")) {
                return CardCategory.POKEMON_EX;
            }
            if (normalized.contains("STAGE 2")) {
                return CardCategory.STAGE_2_POKEMON;
            }
            if (normalized.contains("STAGE 1")) {
                return CardCategory.STAGE_1_POKEMON;
            }
            return CardCategory.BASIC_POKEMON;
        }
        if (supertype == CardSupertype.ENERGY) {
            return normalized.contains("SPECIAL") ? CardCategory.SPECIAL_ENERGY : CardCategory.BASIC_ENERGY;
        }
        if (normalized.contains("SUPPORTER")) {
            return CardCategory.SUPPORTER_TRAINER;
        }
        if (normalized.contains("STADIUM")) {
            return CardCategory.STADIUM_TRAINER;
        }
        if (normalized.contains("POKEMON TOOL")) {
            return CardCategory.POKEMON_TOOL_TRAINER;
        }
        return CardCategory.ITEM_TRAINER;
    }

    private String raw(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new CardImportException("Could not serialize Pokemon TCG card payload");
        }
    }

    private List<String> strings(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    private Integer integerText(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private Integer parseLeadingDamage(String damageText) {
        if (damageText == null || damageText.isBlank()) {
            return null;
        }
        String digits = damageText.replaceFirst("^(\\d+).*$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : null;
    }

    private String text(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null) {
            throw new CardImportException("Pokemon TCG card is missing required field: " + field);
        }
        return value;
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private String firstOrNull(List<String> values) {
        return values.isEmpty() ? null : values.get(0);
    }

    private String normalizeCardText(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

}
