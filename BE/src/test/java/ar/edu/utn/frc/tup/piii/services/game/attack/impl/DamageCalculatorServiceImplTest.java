package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationRequest;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationResult;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the Weakness/Resistance pipeline using a Dark-type attacker (e.g. Malamar XY1-77),
 * including the "Puncture" ("Perforacion") case where the card text says the attack's damage
 * "no se ve afectado por Resistencia" and {@code ignoreResistance} must skip the resistance step
 * entirely while still applying Weakness normally.
 */
class DamageCalculatorServiceImplTest {

    private PassiveAbilityService passiveAbilityService;
    private StadiumModifierService stadiumModifierService;
    private DamageCalculatorServiceImpl service;

    @BeforeEach
    void setUp() {
        passiveAbilityService = mock(PassiveAbilityService.class);
        stadiumModifierService = mock(StadiumModifierService.class);
        when(passiveAbilityService.applyIncomingAttackDamageModifiers(any(), any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        service = new DamageCalculatorServiceImpl(passiveAbilityService, stadiumModifierService);
    }

    @Test
    void shouldApplyDarkTypeAttackerResistanceReduction() {
        Attack attack = attackWithBaseDamage(60);
        Card darkAttacker = cardWithType("Dark");
        Card defender = cardWithType("Colorless");
        defender.addResistance(resistance("Dark", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, darkAttacker, defender, null, 0, 0));

        assertThat(result.baseDamage()).isEqualTo(60);
        assertThat(result.afterResistance()).isEqualTo(40);
        assertThat(result.finalDamage()).isEqualTo(40);
        assertThat(result.damageCounters()).isEqualTo(4);
    }

    @Test
    void shouldNotApplyResistanceForAttackerTypeNotListedInDefenderResistances() {
        Attack attack = attackWithBaseDamage(50);
        Card darkAttacker = cardWithType("Dark");
        Card defender = cardWithType("Colorless");
        defender.addResistance(resistance("Psychic", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, darkAttacker, defender, null, 0, 0));

        assertThat(result.finalDamage()).isEqualTo(50);
    }

    @Test
    void shouldSkipResistanceWhenAttackIgnoresItLikeMalamarPuncture() {
        Attack attack = attackWithBaseDamage(60);
        Card darkAttacker = cardWithType("Dark");
        Card defender = cardWithType("Colorless");
        defender.addResistance(resistance("Dark", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, darkAttacker, defender, null, 0, 0, false, true, false));

        assertThat(result.afterResistance()).isEqualTo(60);
        assertThat(result.finalDamage()).isEqualTo(60);
    }

    @Test
    void shouldStillApplyWeaknessWhenResistanceIsIgnored() {
        Attack attack = attackWithBaseDamage(30);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        defender.addResistance(resistance("Dark", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, fightingAttacker, defender, null, 0, 0, false, true, false));

        assertThat(result.afterWeakness()).isEqualTo(60);
        assertThat(result.finalDamage()).isEqualTo(60);
    }

    @Test
    void shouldSkipWeaknessAndResistanceWhenAttackIgnoresBothLikeRhyperiorRockWrecker() {
        Attack attack = attackWithBaseDamage(130);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        defender.addResistance(resistance("Fighting", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(attack, fightingAttacker, defender, 0, 0, true, true));

        assertThat(result.afterWeakness()).isEqualTo(130);
        assertThat(result.afterResistance()).isEqualTo(130);
        assertThat(result.finalDamage()).isEqualTo(130);
    }

    @Test
    void shouldSkipWeaknessResistanceAndDefenderEffectsWhenAttackIgnoresDefenderEffectsLikeGreninjaMistSlash() {
        Attack attack = attackWithBaseDamage(50);
        Card waterAttacker = cardWithType("Water");
        Card defender = cardWithType("Colorless");
        defender.setExternalId("xy1-114");
        defender.addWeakness(weakness("Water", "x2"));
        defender.addResistance(resistance("Water", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(attack, waterAttacker, defender, 0, -30, true, true, true));

        assertThat(result.afterWeakness()).isEqualTo(50);
        assertThat(result.afterResistance()).isEqualTo(50);
        assertThat(result.finalDamage()).isEqualTo(50);
    }

    @Test
    void shouldNeverReturnNegativeDamageAfterResistanceOrDefenderModifier() {
        Attack attack = attackWithBaseDamage(10);
        Card darkAttacker = cardWithType("Dark");
        Card defender = cardWithType("Colorless");
        defender.addResistance(resistance("Dark", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, darkAttacker, defender, null, 0, -50));

        assertThat(result.afterResistance()).isZero();
        assertThat(result.finalDamage()).isZero();
    }

    @Test
    void shouldApplyWeaknessBeforeResistanceInThatOrder() {
        Attack attack = attackWithBaseDamage(20);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        defender.addResistance(resistance("Fighting", "-20"));

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(null, attack, fightingAttacker, defender, null, 0, 0));

        assertThat(result.afterWeakness()).isEqualTo(40);
        assertThat(result.afterResistance()).isEqualTo(20);
        assertThat(result.finalDamage()).isEqualTo(20);
    }

    @Test
    void shouldSkipWeaknessOnlyWhenShadowCircleProtectsDefender() {
        Attack attack = attackWithBaseDamage(30);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        Game game = new Game();
        game.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defender.addWeakness(weakness("Fighting", "x2"));
        when(stadiumModifierService.isWeaknessNegated(game.getId(), defenderPokemon)).thenReturn(true);

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(game, attack, fightingAttacker, defender, defenderPokemon, 0, 0));

        assertThat(result.afterWeakness()).isEqualTo(30);
        assertThat(result.finalDamage()).isEqualTo(30);
    }

    // ─── Shadow Circle mandatory test matrix (bug report cases A/B/C) ──────────

    @Test
    void shadowCircleCaseA_defenderHasDarknessEnergy_baseDamageIsNotDoubled() {
        // Base attack 10, defender weak to attacker's type, defender has Darkness Energy attached
        // and Shadow Circle is active -> final damage must stay 10, never 20.
        Attack attack = attackWithBaseDamage(10);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        Game game = new Game();
        game.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        when(stadiumModifierService.isWeaknessNegated(game.getId(), defenderPokemon)).thenReturn(true);

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(game, attack, fightingAttacker, defender, defenderPokemon, 0, 0));

        assertThat(result.afterWeakness()).isEqualTo(10);
        assertThat(result.finalDamage()).isEqualTo(10);
    }

    @Test
    void shadowCircleCaseB_defenderHasNoDarknessEnergy_weaknessStillDoubles() {
        // Same setup as Case A, but the defender has no Darkness Energy: weakness must still apply
        // normally even though Shadow Circle is the active stadium. Base 10 -> final 20.
        Attack attack = attackWithBaseDamage(10);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        Game game = new Game();
        game.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        when(stadiumModifierService.isWeaknessNegated(game.getId(), defenderPokemon)).thenReturn(false);

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(game, attack, fightingAttacker, defender, defenderPokemon, 0, 0));

        assertThat(result.afterWeakness()).isEqualTo(20);
        assertThat(result.finalDamage()).isEqualTo(20);
    }

    @Test
    void shadowCircleCaseC_attackerDarknessEnergyIsIrrelevant_weaknessStillApplies() {
        // The attacker's own Darkness Energy must never matter: DamageCalculatorServiceImpl only
        // ever asks StadiumModifierService about the defender (isWeaknessNegated takes the
        // defender's PokemonInPlay, never the attacker's), so weakness still applies here.
        Attack attack = attackWithBaseDamage(10);
        Card fightingAttacker = cardWithType("Fighting");
        Card defender = cardWithType("Colorless");
        defender.addWeakness(weakness("Fighting", "x2"));
        Game game = new Game();
        game.setId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        when(stadiumModifierService.isWeaknessNegated(game.getId(), defenderPokemon)).thenReturn(false);

        DamageCalculationResult result = service.calculateDamage(
                new DamageCalculationRequest(game, attack, fightingAttacker, defender, defenderPokemon, 0, 0));

        assertThat(result.finalDamage()).isEqualTo(20);
    }

    private Attack attackWithBaseDamage(int baseDamage) {
        Attack attack = new Attack();
        attack.setBaseDamage(baseDamage);
        return attack;
    }

    private Card cardWithType(String pokemonType) {
        Card card = new Card();
        card.setPokemonType(pokemonType);
        return card;
    }

    private CardWeakness weakness(String energyType, String multiplier) {
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType(energyType);
        weakness.setMultiplier(multiplier);
        return weakness;
    }

    private CardResistance resistance(String energyType, String value) {
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType(energyType);
        resistance.setValue(value);
        return resistance;
    }
}
