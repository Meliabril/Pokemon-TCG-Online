package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HealDamageTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "HEAL_DAMAGE";
    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public boolean supports(Card card) {
        if (!isItemOrSupporter(card)) {
            return false;
        }

        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(card);
        return EFFECT_TYPE.equals(definition.type()) && definition.amount() > 0;
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(context.trainerCard());
        int healingAmount = definition.amount();
        if (healingAmount <= 0 || healingAmount % 10 != 0) {
            throw new InvalidGameActionException("Trainer heal amount must be a positive multiple of 10");
        }

        UUID targetPokemonInPlayId = requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);
        Optional<PokemonInPlay> targetPokemonResult = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonInPlayId,
                context.gameId(),
                context.actorUserId());
        if (targetPokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Target Pokemon was not found for this Trainer effect");
        }

        PokemonInPlay targetPokemon = targetPokemonResult.get();
        int previousDamageCounters = 0;
        if (targetPokemon.getDamageCounters() != null) {
            previousDamageCounters = targetPokemon.getDamageCounters();
        }
        int healedCounters = healingAmount / 10;
        int newDamageCounters = previousDamageCounters - healedCounters;
        if (newDamageCounters < 0) {
            newDamageCounters = 0;
        }
        targetPokemon.setDamageCounters(newDamageCounters);
        pokemonInPlayStateService.save(targetPokemon);

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("targetPokemonInPlayId", targetPokemonInPlayId.toString());
        effectData.put("healedDamage", (previousDamageCounters - newDamageCounters) * 10);
        effectData.put("remainingDamageCounters", newDamageCounters);
        return new TrainerEffectResult(Map.copyOf(effectData), List.of());
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }

        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }

    private UUID requiredUuid(Map<String, Object> payload, String key) {
        Object value = null;
        if (payload != null) {
            value = payload.get(key);
        }
        if (value instanceof UUID uuidValue) {
            return uuidValue;
        }
        if (value instanceof String stringValue) {
            return UUID.fromString(stringValue);
        }
        throw new InvalidGameActionException("Payload field '" + key + "' must be a UUID");
    }
}
