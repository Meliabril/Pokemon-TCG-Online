package ar.edu.utn.frc.tup.piii.services.game.attack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttackEffectOperation(
        String type,
        AttackEffectPhase phase,
        String target,
        int amount,
        String conditionType,
        CoinRequirement coinRequirement,
        boolean cancelOnTails,
        int coinCount,
        String coinGroupKey,
        String dynamicCoinCountSource,
        String energyType,
        String pokemonType,
        String cardName,
        String cardCategory,
        List<String> conditionTypes) {

    public AttackEffectOperation {
        if (conditionTypes == null) {
            conditionTypes = List.of();
        } else {
            conditionTypes = List.copyOf(conditionTypes);
        }
    }

    public AttackEffectOperation(
            String type,
            AttackEffectPhase phase,
            String target,
            int amount,
            String conditionType,
            CoinRequirement coinRequirement,
            boolean cancelOnTails,
            int coinCount,
            String coinGroupKey) {
        this(
                type,
                phase,
                target,
                amount,
                conditionType,
                coinRequirement,
                cancelOnTails,
                coinCount,
                coinGroupKey,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public AttackEffectOperation(
            String type,
            AttackEffectPhase phase,
            String target,
            int amount,
            String conditionType,
            CoinRequirement coinRequirement,
            boolean cancelOnTails,
            int coinCount,
            String coinGroupKey,
            String dynamicCoinCountSource,
            String energyType) {
        this(
                type,
                phase,
                target,
                amount,
                conditionType,
                coinRequirement,
                cancelOnTails,
                coinCount,
                coinGroupKey,
                dynamicCoinCountSource,
                energyType,
                null,
                null,
                null,
                null);
    }

    public AttackEffectOperation(
            String type,
            AttackEffectPhase phase,
            String target,
            int amount,
            String conditionType,
            CoinRequirement coinRequirement,
            boolean cancelOnTails,
            int coinCount,
            String coinGroupKey,
            String dynamicCoinCountSource,
            String energyType,
            String pokemonType,
            String cardName) {
        this(
                type,
                phase,
                target,
                amount,
                conditionType,
                coinRequirement,
                cancelOnTails,
                coinCount,
                coinGroupKey,
                dynamicCoinCountSource,
                energyType,
                pokemonType,
                cardName,
                null,
                null);
    }

    public AttackEffectOperation(
            String type,
            AttackEffectPhase phase,
            String target,
            int amount,
            String conditionType,
            CoinRequirement coinRequirement,
            boolean cancelOnTails,
            int coinCount,
            String coinGroupKey,
            String dynamicCoinCountSource,
            String energyType,
            String pokemonType,
            String cardName,
            String cardCategory) {
        this(
                type,
                phase,
                target,
                amount,
                conditionType,
                coinRequirement,
                cancelOnTails,
                coinCount,
                coinGroupKey,
                dynamicCoinCountSource,
                energyType,
                pokemonType,
                cardName,
                cardCategory,
                null);
    }
}
