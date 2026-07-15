package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

public record DamageCalculationRequest(
        Game game,
        Attack attack,
        Card attackerCard,
        Card defenderCard,
        PokemonInPlay defenderPokemon,
        int attackerModifier,
        int defenderModifier,
        boolean ignoreWeakness,
        boolean ignoreResistance,
        boolean ignoreDefenderEffects) {

    public DamageCalculationRequest(
            Game game,
            Attack attack,
            Card attackerCard,
            Card defenderCard,
            PokemonInPlay defenderPokemon,
            int attackerModifier,
            int defenderModifier) {
        this(game, attack, attackerCard, defenderCard, defenderPokemon, attackerModifier, defenderModifier, false, false, false);
    }

    public DamageCalculationRequest(
            Attack attack,
            Card attackerCard,
            Card defenderCard,
            int attackerModifier,
            int defenderModifier) {
        this(null, attack, attackerCard, defenderCard, null, attackerModifier, defenderModifier, false, false, false);
    }

    public DamageCalculationRequest(
            Attack attack,
            Card attackerCard,
            Card defenderCard,
            int attackerModifier,
            int defenderModifier,
            boolean ignoreResistance) {
        this(null, attack, attackerCard, defenderCard, null, attackerModifier, defenderModifier, false, ignoreResistance, false);
    }

    public DamageCalculationRequest(
            Attack attack,
            Card attackerCard,
            Card defenderCard,
            int attackerModifier,
            int defenderModifier,
            boolean ignoreWeakness,
            boolean ignoreResistance) {
        this(null, attack, attackerCard, defenderCard, null, attackerModifier, defenderModifier, ignoreWeakness, ignoreResistance, false);
    }

    public DamageCalculationRequest(
            Attack attack,
            Card attackerCard,
            Card defenderCard,
            int attackerModifier,
            int defenderModifier,
            boolean ignoreWeakness,
            boolean ignoreResistance,
            boolean ignoreDefenderEffects) {
        this(
                null,
                attack,
                attackerCard,
                defenderCard,
                null,
                attackerModifier,
                defenderModifier,
                ignoreWeakness,
                ignoreResistance,
                ignoreDefenderEffects);
    }
}
