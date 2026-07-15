package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttackEffectDefinitionReader {

    private static final String ATTACK_EFFECTS_RESOURCE = "game-engine/xy1-attack-effects.json";

    private final ObjectMapper objectMapper;

    private List<AttackEffectDefinition> cachedDefinitions;

    public AttackEffectDefinition read(Card card, Attack attack) {
        List<AttackEffectDefinition> definitions = definitions();
        for (AttackEffectDefinition definition : definitions) {
            if (matchesByName(definition, card, attack)) {
                return definition;
            }
        }
        for (AttackEffectDefinition definition : definitions) {
            if (matchesByOrder(definition, card, attack)) {
                return definition;
            }
        }

        if (attack != null && attack.getEffectText() != null && !attack.getEffectText().isBlank()) {
            throw new InvalidGameActionException("Attack effect is not supported by the current XY1 effect engine");
        }

        return AttackEffectDefinition.empty();
    }

    private List<AttackEffectDefinition> definitions() {
        if (cachedDefinitions != null) {
            return cachedDefinitions;
        }

        ClassPathResource resource = new ClassPathResource(ATTACK_EFFECTS_RESOURCE);
        if (!resource.exists()) {
            cachedDefinitions = List.of();
            return cachedDefinitions;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            AttackEffectCatalog catalog = objectMapper.readValue(inputStream, new TypeReference<AttackEffectCatalog>() {
            });
            if (catalog == null || catalog.effects() == null) {
                cachedDefinitions = List.of();
            } else {
                cachedDefinitions = List.copyOf(catalog.effects());
            }
            return cachedDefinitions;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read XY1 attack effect definitions", exception);
        }
    }

    private boolean matchesByName(AttackEffectDefinition definition, Card card, Attack attack) {
        return matchesCard(definition, card, attack)
                && attack.getCard() != null
                && definition.attackName() != null
                && attack.getName() != null
                && definition.attackName().equalsIgnoreCase(attack.getName());
    }

    private boolean matchesByOrder(AttackEffectDefinition definition, Card card, Attack attack) {
        return matchesCard(definition, card, attack)
                && definition.attackOrder() != null
                && definition.attackOrder() == attack.getAttackOrder();
    }

    private boolean matchesCard(AttackEffectDefinition definition, Card card, Attack attack) {
        return definition != null
                && card != null
                && attack != null
                && definition.cardExternalId() != null
                && definition.cardExternalId().equalsIgnoreCase(card.getExternalId());
    }

    private record AttackEffectCatalog(List<AttackEffectDefinition> effects) {
    }
}
