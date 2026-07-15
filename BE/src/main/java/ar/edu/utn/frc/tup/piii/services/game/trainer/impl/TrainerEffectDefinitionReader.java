package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.entities.Card;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Reads explicit engine metadata from card catalog raw JSON.
 * Falls back to a hardcoded registry for cards that predate the engineEffect field.
 */
@Component
@RequiredArgsConstructor
public class TrainerEffectDefinitionReader {

    private static final String ENGINE_EFFECT_KEY = "engineEffect";
    private static final String TYPE_KEY = "type";
    private static final String AMOUNT_KEY = "amount";

    // Hardcoded fallback for XY1 trainer cards whose raw_json lacks engineEffect.
    // Key = externalId, value = TrainerEffectDefinition.
    private static final Map<String, TrainerEffectDefinition> HARDCODED_EFFECTS = Map.ofEntries(
            Map.entry("xy1-122", new TrainerEffectDefinition("DISCARD_HAND_AND_DRAW", 7)),        // Professor Sycamore
            Map.entry("xy1-127", new TrainerEffectDefinition("SHUFFLE_HAND_AND_DRAW", 5)),        // Shauna
            Map.entry("xy1-128", new TrainerEffectDefinition("HEAL_DAMAGE", 60)),                 // Super Potion
            Map.entry("xy1-125", new TrainerEffectDefinition("DRAW_CARDS", 3)),                   // Roller Skates
            Map.entry("xy1-124", new TrainerEffectDefinition("SHUFFLE_OPPONENT_HAND_AND_DRAW", 4)), // Red Card
            Map.entry("xy1-129", new TrainerEffectDefinition("DISCARD_OPPONENT_ACTIVE_ENERGY", 0)), // Team Flare Grunt
            Map.entry("xy1-123", new TrainerEffectDefinition("SEARCH_BASIC_ENERGIES_FROM_DECK", 2)), // Professor's Letter
            Map.entry("xy1-115", new TrainerEffectDefinition("RETURN_BENCHED_POKEMON_TO_HAND", 0)),  // Cassius
            Map.entry("xy1-116", new TrainerEffectDefinition("SEARCH_EVOLUTION_FROM_DECK", 1)),      // Evosoda
            Map.entry("xy1-118", new TrainerEffectDefinition("SEARCH_POKEMON_FROM_DECK", 1)),        // Super Ball
            Map.entry("xy1-120", new TrainerEffectDefinition("REVIVE_POKEMON_FROM_DISCARD", 0))     // Max Revive
    );

    private final ObjectMapper objectMapper;

    public TrainerEffectDefinition read(Card card) {
        if (card == null || card.getRawJson() == null || card.getRawJson().isBlank()) {
            return hardcodedFallback(card);
        }

        try {
            JsonNode rootNode = objectMapper.readTree(card.getRawJson());
            JsonNode effectNode = rootNode.get(ENGINE_EFFECT_KEY);
            if (effectNode == null || effectNode.isMissingNode() || effectNode.isNull()) {
                return hardcodedFallback(card);
            }

            String type = textValue(effectNode.get(TYPE_KEY));
            int amount = intValue(effectNode.get(AMOUNT_KEY));
            return new TrainerEffectDefinition(type, amount);
        } catch (JsonProcessingException exception) {
            return hardcodedFallback(card);
        }
    }

    private TrainerEffectDefinition hardcodedFallback(Card card) {
        if (card == null || card.getExternalId() == null) {
            return TrainerEffectDefinition.empty();
        }
        return HARDCODED_EFFECTS.getOrDefault(card.getExternalId(), TrainerEffectDefinition.empty());
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private int intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }

        return node.asInt();
    }
}
