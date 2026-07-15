package ar.edu.utn.frc.tup.piii.services.game.attack;



import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.DamageCalculatorServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DamageCalculatorServiceTest {

    private final PassiveAbilityService passiveAbilityService = mock(PassiveAbilityService.class);
    private final StadiumModifierService stadiumModifierService = mock(StadiumModifierService.class);
    private final DamageCalculatorService damageCalculatorService = new DamageCalculatorServiceImpl(passiveAbilityService, stadiumModifierService);

    @BeforeEach
    void setUp() {
        when(passiveAbilityService.applyIncomingAttackDamageModifiers(any(), any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @Test
    void shouldApplyWeaknessMultiplierWhenTypesMatch() {
        Attack attack = new Attack();
        attack.setBaseDamage(20);

        Card attacker = new Card();
        attacker.setPokemonType("Fire");

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fire");
        weakness.setMultiplier("x2");
        LinkedHashSet<CardWeakness> weaknesses = new LinkedHashSet<>();
        weaknesses.add(weakness);
        defender.setWeaknesses(weaknesses);

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(40);
        assertThat(result.damageCounters()).isEqualTo(4);
    }

    @Test
    void shouldApplyResistanceReductionWithoutGoingBelowZero() {
        Attack attack = new Attack();
        attack.setBaseDamage(10);

        Card attacker = new Card();
        attacker.setPokemonType("Water");

        Card defender = new Card();
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Water");
        resistance.setValue("-20");
        LinkedHashSet<CardResistance> resistances = new LinkedHashSet<>();
        resistances.add(resistance);
        defender.setResistances(resistances);

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isZero();
        assertThat(result.afterResistance()).isZero();
    }

    @Test
    void shouldIgnoreResistanceWithoutIgnoringWeakness() {
        Attack attack = new Attack();
        attack.setBaseDamage(60);

        Card attacker = new Card();
        attacker.setPokemonType("Fighting");

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fighting");
        weakness.setMultiplier("x2");
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Fighting");
        resistance.setValue("-20");
        defender.setWeaknesses(Set.of(weakness));
        defender.setResistances(Set.of(resistance));

        DamageCalculationResult result = damageCalculatorService.calculateDamage(new DamageCalculationRequest(
                attack,
                attacker,
                defender,
                0,
                0,
                true));

        assertThat(result.afterWeakness()).isEqualTo(120);
        assertThat(result.afterResistance()).isEqualTo(120);
        assertThat(result.finalDamage()).isEqualTo(120);
    }

    @Test
    void shouldUseZeroDamageWhenAttackHasNoBaseDamage() {
        Attack attack = new Attack();

        Card attacker = new Card();
        attacker.setPokemonType("Fire");

        Card defender = new Card();
        defender.setWeaknesses(Set.of());
        defender.setResistances(Set.of());

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isZero();
    }

    @Test
    void shouldSkipWeaknessAndResistanceWhenAttackerHasNoType() {
        Attack attack = new Attack();
        attack.setBaseDamage(30);

        Card attacker = new Card();

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fire");
        weakness.setMultiplier("x2");
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Fire");
        resistance.setValue("-20");
        defender.setWeaknesses(Set.of(weakness));
        defender.setResistances(Set.of(resistance));

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(30);
    }

    @Test
    void shouldDefaultMalformedWeaknessAndResistanceValues() {
        Attack attack = new Attack();
        attack.setBaseDamage(30);

        Card attacker = new Card();
        attacker.setPokemonType("Lightning");

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Lightning");
        weakness.setMultiplier("not-a-number");
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Lightning");
        resistance.setValue("also-bad");
        defender.setWeaknesses(Set.of(weakness));
        defender.setResistances(Set.of(resistance));

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(60);
    }

    @Test
    void shouldParseBlankWeaknessAndResistanceAsDefaults() {
        Attack attack = new Attack();
        attack.setBaseDamage(20);

        Card attacker = new Card();
        attacker.setPokemonType("Grass");

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Grass");
        weakness.setMultiplier(" ");
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Grass");
        resistance.setValue(" ");
        defender.setWeaknesses(Set.of(weakness));
        defender.setResistances(Set.of(resistance));

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(40);
    }

    @Test
    void shouldApplyAttackerAndDefenderModifiersInOfficialOrder() {
        Attack attack = new Attack();
        attack.setBaseDamage(20);

        Card attacker = new Card();
        attacker.setPokemonType("Fire");

        Card defender = new Card();
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fire");
        weakness.setMultiplier("x2");
        defender.setWeaknesses(Set.of(weakness));
        defender.setResistances(Set.of());

        DamageCalculationResult result = calculate(attack, attacker, defender, 10, -20);

        assertThat(result.baseDamage()).isEqualTo(20);
        assertThat(result.afterAttackerModifiers()).isEqualTo(30);
        assertThat(result.afterWeakness()).isEqualTo(60);
        assertThat(result.afterResistance()).isEqualTo(60);
        assertThat(result.finalDamage()).isEqualTo(40);
    }

    @Test
    void shouldReduceDamageByTwentyWhenDefenderIsFurfrou() {
        Attack attack = new Attack();
        attack.setBaseDamage(80);

        Card attacker = new Card();
        attacker.setPokemonType("Colorless");

        Card defender = new Card();
        defender.setExternalId("xy1-114");
        defender.setWeaknesses(Set.of());
        defender.setResistances(Set.of());
        when(passiveAbilityService.applyIncomingAttackDamageModifiers(any(), any(), eq(80))).thenReturn(60);

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(60);
    }

    @Test
    void shouldNotReduceDamageBelowZeroForFurfrou() {
        Attack attack = new Attack();
        attack.setBaseDamage(10);

        Card attacker = new Card();
        attacker.setPokemonType("Colorless");

        Card defender = new Card();
        defender.setExternalId("xy1-114");
        defender.setWeaknesses(Set.of());
        defender.setResistances(Set.of());
        when(passiveAbilityService.applyIncomingAttackDamageModifiers(any(), any(), eq(10))).thenReturn(0);

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isZero();
    }

    @Test
    void shouldNotApplyFurfrouReductionToOtherCards() {
        Attack attack = new Attack();
        attack.setBaseDamage(80);

        Card attacker = new Card();
        attacker.setPokemonType("Colorless");

        Card defender = new Card();
        defender.setExternalId("xy1-1");
        defender.setWeaknesses(Set.of());
        defender.setResistances(Set.of());

        DamageCalculationResult result = calculate(attack, attacker, defender, 0, 0);

        assertThat(result.finalDamage()).isEqualTo(80);
    }

    @Test
    void shouldIgnoreResistanceWhenRequested() {
        Attack attack = new Attack();
        attack.setBaseDamage(20);

        Card attacker = new Card();
        attacker.setPokemonType("Fighting");

        Card defender = new Card();
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Fighting");
        resistance.setValue("-20");
        defender.setResistances(Set.of(resistance));
        defender.setWeaknesses(Set.of());

        DamageCalculationResult result = damageCalculatorService.calculateDamage(new DamageCalculationRequest(
                attack, attacker, defender, 0, 0, true));

        assertThat(result.finalDamage()).isEqualTo(20);
        assertThat(result.afterResistance()).isEqualTo(20);
    }

    private DamageCalculationResult calculate(
            Attack attack,
            Card attacker,
            Card defender,
            int attackerModifier,
            int defenderModifier) {
        return damageCalculatorService.calculateDamage(new DamageCalculationRequest(
                attack,
                attacker,
                defender,
                attackerModifier,
                defenderModifier));
    }
}
