package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class Xy1AttackEffectCatalogTest {

    private static final Path CATALOG_PATH = Path.of("src/main/resources/game-engine/xy1-attack-effects.json");

    @Test
    void shouldHaveUniqueCardAndAttackNameCombinations() throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();

        List<String> keys = definitions.stream()
                .map(definition -> definition.cardExternalId().toLowerCase() + "|" + definition.attackName().toLowerCase())
                .toList();

        assertThat(keys).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @MethodSource("bucketZeroOperations")
    void shouldDefineExpectedBucketZeroOperation(
            String cardExternalId,
            String attackName,
            String expectedType,
            AttackEffectPhase expectedPhase,
            String expectedTarget,
            int expectedAmount,
            String expectedConditionType,
            CoinRequirement expectedCoinRequirement,
            boolean expectedCancelOnTails,
            int expectedCoinCount) throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();

        AttackEffectDefinition definition = definitions.stream()
                .filter(candidate -> candidate.cardExternalId().equalsIgnoreCase(cardExternalId)
                        && candidate.attackName().equalsIgnoreCase(attackName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No definition found for " + cardExternalId + " / " + attackName));

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo(expectedType);
        assertThat(operation.phase()).isEqualTo(expectedPhase);
        assertThat(operation.target()).isEqualTo(expectedTarget);
        assertThat(operation.amount()).isEqualTo(expectedAmount);
        assertThat(operation.conditionType()).isEqualTo(expectedConditionType);
        assertThat(operation.coinRequirement()).isEqualTo(expectedCoinRequirement);
        assertThat(operation.cancelOnTails()).isEqualTo(expectedCancelOnTails);
        assertThat(operation.coinCount()).isEqualTo(expectedCoinCount);
    }

    private static Stream<Arguments> bucketZeroOperations() {
        return Stream.of(
                Arguments.of("xy1-13", "Wood Hammer", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 10, null, CoinRequirement.NONE, false, 0),
                Arguments.of("xy1-12", "Pin Missile", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 10, null, CoinRequirement.NONE, false, 4),
                Arguments.of("xy1-29", "Splash Bomb", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 30, null, CoinRequirement.TAILS, false, 0),
                Arguments.of("xy1-33", "Reckless Charge", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 10, null, CoinRequirement.NONE, false, 0),
                Arguments.of("xy1-40", "Lick", "APPLY_SPECIAL_CONDITION", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 0, "PARALYZED", CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-42", "Nuzzle", "APPLY_SPECIAL_CONDITION", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 0, "PARALYZED", CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-42", "Quick Attack", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 10, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-45", "Eerie Impulse", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 1, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-68", "Rip Claw", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 1, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-70", "Crunch", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 1, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-73", "Night Claw", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 2, null, CoinRequirement.TAILS, false, 0),
                Arguments.of("xy1-81", "Cut Down", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 1, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-84", "Dual Blades", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 30, null, CoinRequirement.NONE, false, 2),
                Arguments.of("xy1-88", "Body Slam", "APPLY_SPECIAL_CONDITION", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 0, "PARALYZED", CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-90", "Double-Edge", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 10, null, CoinRequirement.NONE, false, 0),
                Arguments.of("xy1-91", "Slap Down", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.NONE, false, 2),
                Arguments.of("xy1-98", "Double Hit", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 30, null, CoinRequirement.NONE, false, 2),
                Arguments.of("xy1-99", "Endeavor", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.NONE, false, 2),
                Arguments.of("xy1-100", "Take Down", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 10, null, CoinRequirement.NONE, false, 0),
                Arguments.of("xy1-101", "Glare", "APPLY_SPECIAL_CONDITION", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 0, "PARALYZED", CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-102", "Aerial Ace", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 30, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-105", "Fake Out", "APPLY_SPECIAL_CONDITION", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 0, "PARALYZED", CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-106", "Hyper Fang", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 0, null, CoinRequirement.HEADS, true, 0),
                Arguments.of("xy1-107", "Double Headbutt", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 30, null, CoinRequirement.NONE, false, 2),
                Arguments.of("xy1-109", "Jump On", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.HEADS, false, 0));
    }

    @ParameterizedTest
    @MethodSource("bucketOneOperations")
    void shouldDefineExpectedBucketOneOperation(
            String cardExternalId,
            String attackName,
            String expectedType,
            AttackEffectPhase expectedPhase,
            String expectedTarget,
            int expectedAmount,
            String expectedConditionType,
            CoinRequirement expectedCoinRequirement,
            boolean expectedCancelOnTails,
            int expectedCoinCount) throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();

        AttackEffectDefinition definition = definitions.stream()
                .filter(candidate -> candidate.cardExternalId().equalsIgnoreCase(cardExternalId)
                        && candidate.attackName().equalsIgnoreCase(attackName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No definition found for " + cardExternalId + " / " + attackName));

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo(expectedType);
        assertThat(operation.phase()).isEqualTo(expectedPhase);
        assertThat(operation.target()).isEqualTo(expectedTarget);
        assertThat(operation.amount()).isEqualTo(expectedAmount);
        assertThat(operation.conditionType()).isEqualTo(expectedConditionType);
        assertThat(operation.coinRequirement()).isEqualTo(expectedCoinRequirement);
        assertThat(operation.cancelOnTails()).isEqualTo(expectedCancelOnTails);
        assertThat(operation.coinCount()).isEqualTo(expectedCoinCount);
    }

    private static Stream<Arguments> bucketOneOperations() {
        return Stream.of(
                Arguments.of("xy1-32", "Spike Cannon", "COIN_MULTI_DAMAGE", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 30, null, CoinRequirement.NONE, false, 5),
                Arguments.of("xy1-37", "Water Splash", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-65", "Pummel", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-66", "Pummel", "COIN_DAMAGE_MODIFIER", AttackEffectPhase.BEFORE_DAMAGE, "DEFENDER", 20, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-114", "Energy Cutoff", "DISCARD_ENERGY", AttackEffectPhase.AFTER_DAMAGE, "DEFENDER", 1, null, CoinRequirement.HEADS, false, 0),
                Arguments.of("xy1-142", "Splash Bomb", "RECOIL_DAMAGE", AttackEffectPhase.AFTER_DAMAGE, "ATTACKER", 30, null, CoinRequirement.TAILS, false, 0));
    }

    @ParameterizedTest
    @MethodSource("preventDamageNextTurnOperations")
    void shouldDefinePreventDamageNextTurnOperation(String cardExternalId, String attackName) throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();

        AttackEffectDefinition definition = definitions.stream()
                .filter(candidate -> candidate.cardExternalId().equalsIgnoreCase(cardExternalId)
                        && candidate.attackName().equalsIgnoreCase(attackName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No definition found for " + cardExternalId + " / " + attackName));

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("PREVENT_DAMAGE_NEXT_TURN");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.HEADS);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    private static Stream<Arguments> preventDamageNextTurnOperations() {
        return Stream.of(
                Arguments.of("xy1-13", "Scrunch"),
                Arguments.of("xy1-111", "Dig"),
                Arguments.of("xy1-112", "Dig"));
    }

    @Test
    void clampCrushShouldShareCoinGroupBetweenParalysisAndEnergyDiscard() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-32", "Clamp Crush");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation paralysis = definition.operations().get(0);
        AttackEffectOperation discard = definition.operations().get(1);

        assertThat(paralysis.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(paralysis.conditionType()).isEqualTo("PARALYZED");
        assertThat(paralysis.coinRequirement()).isEqualTo(CoinRequirement.HEADS);

        assertThat(discard.type()).isEqualTo("DISCARD_ENERGY");
        assertThat(discard.coinRequirement()).isEqualTo(CoinRequirement.HEADS);

        assertThat(paralysis.coinGroupKey()).isNotNull().isEqualTo(discard.coinGroupKey());
    }

    @Test
    void dynamicPunchShouldShareCoinGroupBetweenDamageBonusAndConfusion() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-67", "Dynamic Punch");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation damageBonus = definition.operations().get(0);
        AttackEffectOperation confusion = definition.operations().get(1);

        assertThat(damageBonus.type()).isEqualTo("COIN_DAMAGE_MODIFIER");
        assertThat(damageBonus.amount()).isEqualTo(40);
        assertThat(damageBonus.coinRequirement()).isEqualTo(CoinRequirement.HEADS);

        assertThat(confusion.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(confusion.conditionType()).isEqualTo("CONFUSED");
        assertThat(confusion.coinRequirement()).isEqualTo(CoinRequirement.HEADS);

        assertThat(damageBonus.coinGroupKey()).isNotNull().isEqualTo(confusion.coinGroupKey());
    }

    @Test
    void wakeUpSlapShouldAddDamageAndCureDefenderAsleepConditionOnly() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-67", "Wake-Up Slap");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CONDITIONAL_DEFENDER_SPECIAL_CONDITION_DAMAGE_AND_CURE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(60);
        assertThat(operation.conditionType()).isEqualTo("ASLEEP");
        assertThat(operation.conditionTypes()).isEmpty();
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void blazeBallShouldScaleWithAttachedFireEnergy() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-26", "Blaze Ball");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DYNAMIC_ENERGY_COUNT_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(20);
        assertThat(operation.energyType()).isEqualTo("Fire");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void talonflameShouldShuffleOpponentHandAndDiscardFireEnergy() throws IOException {
        AttackEffectDefinition devastatingWind = definitionFor("xy1-28", "Devastating Wind");
        AttackEffectDefinition flareBlitz = definitionFor("xy1-28", "Flare Blitz");

        assertThat(devastatingWind.attackOrder()).isEqualTo(0);
        assertThat(devastatingWind.operations()).hasSize(1);
        AttackEffectOperation shuffleDraw = devastatingWind.operations().get(0);
        assertThat(shuffleDraw.type()).isEqualTo("SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW");
        assertThat(shuffleDraw.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(shuffleDraw.target()).isEqualTo("DEFENDER");
        assertThat(shuffleDraw.amount()).isEqualTo(4);
        assertThat(shuffleDraw.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(flareBlitz.attackOrder()).isEqualTo(1);
        assertThat(flareBlitz.operations()).hasSize(1);
        AttackEffectOperation discardFireEnergy = flareBlitz.operations().get(0);
        assertThat(discardFireEnergy.type()).isEqualTo("DISCARD_ENERGY");
        assertThat(discardFireEnergy.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(discardFireEnergy.target()).isEqualTo("ATTACKER");
        assertThat(discardFireEnergy.amount()).isEqualTo(999);
        assertThat(discardFireEnergy.energyType()).isEqualTo("Fire");
        assertThat(discardFireEnergy.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void mistSlashShouldIgnoreDefenderEffects() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-41", "Mist Slash");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("IGNORE_DEFENDER_EFFECTS");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void busterSwingShouldIgnoreResistance() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-85", "Buster Swing");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("IGNORE_RESISTANCE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void kingsShieldShouldPreventDamageAndLockItselfNextTurn() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-86", "King's Shield");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation preventDamage = definition.operations().get(0);
        AttackEffectOperation attackLock = definition.operations().get(1);

        assertThat(preventDamage.type()).isEqualTo("PREVENT_DAMAGE_NEXT_TURN");
        assertThat(preventDamage.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(preventDamage.target()).isEqualTo("ATTACKER");
        assertThat(preventDamage.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(attackLock.type()).isEqualTo("PREVENT_SELF_ATTACK_NEXT_TURN");
        assertThat(attackLock.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(attackLock.target()).isEqualTo("ATTACKER");
        assertThat(attackLock.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void distortionBeamShouldApplySleepOnHeadsAndConfusionOnTailsFromSameFlip() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-76", "Distortion Beam");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation sleep = definition.operations().get(0);
        AttackEffectOperation confusion = definition.operations().get(1);

        assertThat(sleep.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(sleep.conditionType()).isEqualTo("ASLEEP");
        assertThat(sleep.coinRequirement()).isEqualTo(CoinRequirement.HEADS);

        assertThat(confusion.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(confusion.conditionType()).isEqualTo("CONFUSED");
        assertThat(confusion.coinRequirement()).isEqualTo(CoinRequirement.TAILS);

        assertThat(sleep.coinGroupKey()).isNotNull().isEqualTo(confusion.coinGroupKey());
    }

    @ParameterizedTest
    @MethodSource("coinFlipUntilTailsOperations")
    void shouldDefineExpectedCoinFlipUntilTailsOperation(
            String cardExternalId, String attackName, int expectedAmount) throws IOException {
        AttackEffectDefinition definition = definitionFor(cardExternalId, attackName);

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("COIN_FLIP_UNTIL_TAILS_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(expectedAmount);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    private static Stream<Arguments> coinFlipUntilTailsOperations() {
        return Stream.of(
                Arguments.of("xy1-36", "Spiny Rush", 20),
                Arguments.of("xy1-52", "Continuous Tumble", 30));
    }

    @Test
    void darknessBladeShouldPreventSelfAttackOnTails() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-78", "Darkness Blade");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("PREVENT_SELF_ATTACK_NEXT_TURN");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.TAILS);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    @Test
    void rockBlackShouldFlipPerAttachedFightingEnergy() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-62", "Rock Black");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DYNAMIC_COIN_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(50);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.dynamicCoinCountSource()).isEqualTo("ATTACKER_ATTACHED_ENERGY_TYPE");
        assertThat(operation.energyType()).isEqualTo("Fighting");
    }

    @Test
    void rockWreckerShouldIgnoreWeaknessResistanceAndLockAttacker() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-62", "Rock Wrecker");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation ignoreWeaknessResistance = definition.operations().get(0);
        AttackEffectOperation attackLock = definition.operations().get(1);

        assertThat(ignoreWeaknessResistance.type()).isEqualTo("IGNORE_WEAKNESS_RESISTANCE");
        assertThat(ignoreWeaknessResistance.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(ignoreWeaknessResistance.target()).isEqualTo("DEFENDER");
        assertThat(ignoreWeaknessResistance.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(attackLock.type()).isEqualTo("PREVENT_SELF_ATTACK_NEXT_TURN");
        assertThat(attackLock.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(attackLock.target()).isEqualTo("ATTACKER");
        assertThat(attackLock.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void seethingAngerShouldFlipPerAttackerDamageCounter() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-100", "Seething Anger");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DYNAMIC_COIN_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(30);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.dynamicCoinCountSource()).isEqualTo("ATTACKER_DAMAGE_COUNTERS");
        assertThat(operation.energyType()).isNull();
    }

    @Test
    void botherShouldPreventOpponentSupportersOnHeads() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-71", "Bother");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("PREVENT_OPPONENT_SUPPORTER_NEXT_TURN");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.HEADS);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    @Test
    void knockBackShouldForceOpponentBenchSwap() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-71", "Knock Back");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("OPPONENT_FORCED_BENCH_SWAP");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("OPPONENT_BENCH");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void biteOffShouldAddDamageAgainstPokemonEx() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-110", "Bite Off");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CONDITIONAL_DEFENDER_CATEGORY_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(60);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.cardCategory()).isEqualTo("POKEMON_EX");
    }

    @Test
    void wildBarkingShouldDamageOpponentBench() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-110", "Wild Barking");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("BENCH_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("OPPONENT_BENCH");
        assertThat(operation.amount()).isEqualTo(20);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void flashNeedleShouldDealDamagePerHeadsAndProtectOnAllHeads() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-5", "Flash Needle");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("COIN_MULTI_DAMAGE_ALL_HEADS_PROTECTION");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(40);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.coinCount()).isEqualTo(3);
    }

    @Test
    void poisonRingShouldPoisonAndPreventOpponentRetreat() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-53", "Poison Ring");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation poison = definition.operations().get(0);
        AttackEffectOperation retreatLock = definition.operations().get(1);

        assertThat(poison.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(poison.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(poison.target()).isEqualTo("DEFENDER");
        assertThat(poison.conditionType()).isEqualTo("POISONED");
        assertThat(poison.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(retreatLock.type()).isEqualTo("PREVENT_OPPONENT_RETREAT_NEXT_TURN");
        assertThat(retreatLock.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(retreatLock.target()).isEqualTo("DEFENDER");
        assertThat(retreatLock.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void conversionPowderShouldOfferSleepOrPoisonChoice() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-17", "Conversion Powder");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CHOOSE_SPECIAL_CONDITION");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.conditionTypes()).containsExactly("ASLEEP", "POISONED");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void colorfulWindShouldScaleWithDistinctBasicEnergyTypes() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-17", "Colorful Wind");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DISTINCT_BASIC_ENERGY_TYPE_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(30);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void madMountainShouldDiscardOpponentDeckOnAllHeads() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-61", "Mad Mountain");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("COIN_ALL_HEADS_OPPONENT_DECK_DISCARD");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.coinCount()).isEqualTo(2);
    }

    @Test
    void mentalTrashShouldDiscardOpponentHandCardsForTails() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-76", "Mental Trash");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("OPPONENT_COIN_TAILS_HAND_DISCARD");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.coinCount()).isEqualTo(4);
    }

    @Test
    void leadShouldSearchSupporterFromDeckOnHeads() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-18", "Lead");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("SEARCH_SUPPORTER_FROM_DECK");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.HEADS);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    @Test
    void bounceShouldSwitchAttackerWithBenchOnHeads() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-39", "Bounce");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("SELF_SWITCH_WITH_BENCH");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("OWN_BENCH");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.HEADS);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    @Test
    void luringGlowShouldForceOpponentBenchSwap() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-8", "Luring Glow");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("OPPONENT_FORCED_BENCH_SWAP");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("OPPONENT_BENCH");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void seafaringShouldAttachWaterEnergyFromDiscardToBench() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-35", "Seafaring");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("ATTACH_ENERGY_FROM_DISCARD_TO_BENCH");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.coinCount()).isEqualTo(3);
        assertThat(operation.energyType()).isEqualTo("Water");
    }

    @Test
    void oblivionWingShouldAttachDarknessEnergyFromDiscardToBenchOnYveltal() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-78", "Oblivion Wing");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("ATTACH_ENERGY_FROM_DISCARD_TO_BENCH");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.coinCount()).isZero();
        assertThat(operation.amount()).isEqualTo(1);
        assertThat(operation.energyType()).isEqualTo("Darkness");
    }

    @Test
    void hydroPumpShouldScaleDamageWithAttachedWaterEnergy() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-35", "Hydro Pump");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DYNAMIC_ENERGY_COUNT_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(20);
        assertThat(operation.energyType()).isEqualTo("Water");
    }

    @Test
    void leafMunchShouldAddDamageAgainstGrassDefender() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-3", "Leaf Munch");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CONDITIONAL_OPPONENT_ACTIVE_TYPE_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(20);
        assertThat(operation.pokemonType()).isEqualTo("Grass");
    }

    @Test
    void cosmicSpinShouldAddDamageWhenLunatoneIsOnOwnBench() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-64", "Cosmic Spin");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CONDITIONAL_OWN_BENCH_CARD_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(30);
        assertThat(operation.cardName()).isEqualTo("Lunatone");
    }

    @Test
    void doubleDrawShouldDrawTwoCards() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-63", "Double Draw");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DRAW_CARDS");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(2);
    }

    @Test
    void filchShouldDrawOneCardOnSableye() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-68", "Filch");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DRAW_CARDS");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(1);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void confusionWaveShouldConfuseBothActivePokemonOnInkay() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-74", "Confusion Wave");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation attackerConfusion = definition.operations().get(0);
        AttackEffectOperation defenderConfusion = definition.operations().get(1);

        assertThat(attackerConfusion.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(attackerConfusion.target()).isEqualTo("ATTACKER");
        assertThat(attackerConfusion.conditionType()).isEqualTo("CONFUSED");
        assertThat(attackerConfusion.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(defenderConfusion.type()).isEqualTo("APPLY_SPECIAL_CONDITION");
        assertThat(defenderConfusion.target()).isEqualTo("DEFENDER");
        assertThat(defenderConfusion.conditionType()).isEqualTo("CONFUSED");
        assertThat(defenderConfusion.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void secondBiteShouldScaleDamageByDefenderCountersOnDunsparce() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-101", "Second Bite");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("CONDITIONAL_DEFENDER_DAMAGE_BY_COUNTERS");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(10);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void punctureShouldIgnoreResistanceOnInkay() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-75", "Puncture");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("IGNORE_RESISTANCE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void nastyPlotShouldSearchAnyCardFromDeckOnZorua() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-72", "Nasty Plot");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("SEARCH_ANY_CARD_FROM_DECK");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void heartfeltSongShouldDiscardDarknessEnergyOnJigglypuff() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-87", "Heartfelt Song");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DISCARD_ENERGY");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(1);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.energyType()).isEqualTo("Darkness");
    }

    @Test
    void geomancyShouldAttachFairyEnergyFromDeckToBenchOnXerneas() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-96", "Geomancy");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("SEARCH_ENERGY_FROM_DECK_TO_BENCH");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(2);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.energyType()).isEqualTo("Fairy");
    }

    @Test
    void rainbowSpearShouldDiscardOneEnergyFromXerneas() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-96", "Rainbow Spear");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DISCARD_ENERGY");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(1);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void meFirstShouldDrawOneCardOnFletchling() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-113", "Me First");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DRAW_CARDS");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(1);
    }

    @Test
    void moonblastShouldReduceDefendingPokemonDamageNextTurn() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-63", "Moonblast");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("REDUCE_DEFENDING_POKEMON_DAMAGE_NEXT_TURN");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.amount()).isEqualTo(20);
    }

    @Test
    void pheromotionShouldSearchGrassPokemonFromDeck() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-9", "Pheromotion");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("SEARCH_POKEMON_FROM_DECK_BY_TYPE_TO_HAND");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.pokemonType()).isEqualTo("Grass");
    }

    @Test
    void mineShouldLookOpponentDeckTopCardWithOptionalShuffleChoice() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-58", "Mine");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("LOOK_OPPONENT_DECK_TOP_CARD_OPTIONAL_SHUFFLE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
    }

    @Test
    void digOutShouldDiscardTopDeckCardWithConditionalEnergyAttach() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-60", "Dig Out");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("DISCARD_TOP_DECK_CONDITIONAL_ENERGY_ATTACH");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
    }

    @Test
    void astonishShouldRevealAndShuffleRandomOpponentHandCard() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-54", "Astonish");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
    }

    @Test
    void mentalPanicShouldSetDeferredAttackCancellation() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-77", "Mental Panic");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("MENTAL_PANIC");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("DEFENDER");
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    @Test
    void chargeForwardShouldDealOptionalBonusRecoilDamage() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-19", "Charge Dash");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("OPTIONAL_BONUS_RECOIL_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.BEFORE_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(20);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    private AttackEffectDefinition definitionFor(String cardExternalId, String attackName) throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();
        return definitions.stream()
                .filter(candidate -> candidate.cardExternalId().equalsIgnoreCase(cardExternalId)
                        && candidate.attackName().equalsIgnoreCase(attackName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No definition found for " + cardExternalId + " / " + attackName));
    }

    @ParameterizedTest
    @MethodSource("healDamageOperations")
    void shouldDefineExpectedHealDamageOperation(
            String cardExternalId, String attackName, int expectedAmount) throws IOException {
        AttackEffectDefinition definition = definitionFor(cardExternalId, attackName);

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ATTACKER");
        assertThat(operation.amount()).isEqualTo(expectedAmount);
        assertThat(operation.coinRequirement()).isEqualTo(CoinRequirement.NONE);
        assertThat(operation.cancelOnTails()).isFalse();
    }

    private static Stream<Arguments> healDamageOperations() {
        return Stream.of(
                Arguments.of("xy1-1", "Jungle Hammer", 30),
                Arguments.of("xy1-141", "Jungle Hammer", 30),
                Arguments.of("xy1-10", "Leech Seed", 10),
                Arguments.of("xy1-14", "Touchdown", 20),
                Arguments.of("xy1-95", "Draining Kiss", 30));
    }

    @Test
    void recoverShouldDiscardEnergyAndHealAllDamageFromStarmie() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-34", "Recover");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation discard = definition.operations().get(0);
        AttackEffectOperation heal = definition.operations().get(1);

        assertThat(discard.type()).isEqualTo("DISCARD_ENERGY");
        assertThat(discard.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(discard.target()).isEqualTo("ATTACKER");
        assertThat(discard.amount()).isEqualTo(1);
        assertThat(discard.coinRequirement()).isEqualTo(CoinRequirement.NONE);

        assertThat(heal.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(heal.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(heal.target()).isEqualTo("ATTACKER");
        assertThat(heal.amount()).isEqualTo(999);
        assertThat(heal.coinRequirement()).isEqualTo(CoinRequirement.NONE);
    }

    @Test
    void refreshShouldHealAndCureSpecialConditionsOnCorsola() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-36", "Refresh");

        assertThat(definition.operations()).hasSize(2);
        AttackEffectOperation heal = definition.operations().get(0);
        AttackEffectOperation cure = definition.operations().get(1);

        assertThat(heal.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(heal.target()).isEqualTo("ATTACKER");
        assertThat(heal.amount()).isEqualTo(30);

        assertThat(cure.type()).isEqualTo("CURE_SPECIAL_CONDITIONS");
        assertThat(cure.target()).isEqualTo("ATTACKER");
    }

    @Test
    void healBellShouldHealEveryOwnedPokemonOnSkitty() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-104", "Heal Bell");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(operation.phase()).isEqualTo(AttackEffectPhase.AFTER_DAMAGE);
        assertThat(operation.target()).isEqualTo("ALL_OWN");
        assertThat(operation.amount()).isEqualTo(10);
    }

    @Test
    void massageShouldHealASelectedBenchedPokemonOnMrMime() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-91", "Massage");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(operation.target()).isEqualTo("OWN_BENCH");
        assertThat(operation.amount()).isEqualTo(60);
    }

    @Test
    void sweetScentShouldHealASelectedOwnPokemonOnSpritzee() throws IOException {
        AttackEffectDefinition definition = definitionFor("xy1-92", "Sweet Scent");

        assertThat(definition.operations()).hasSize(1);
        AttackEffectOperation operation = definition.operations().get(0);

        assertThat(operation.type()).isEqualTo("HEAL_DAMAGE");
        assertThat(operation.target()).isEqualTo("OWN_ANY");
        assertThat(operation.amount()).isEqualTo(20);
    }

    @Test
    void electrodeShouldAllowBenchTarget() throws IOException {
        List<AttackEffectDefinition> definitions = readCatalog();

        AttackEffectDefinition electrode = definitions.stream()
                .filter(candidate -> candidate.cardExternalId().equalsIgnoreCase("xy1-45"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No definition found for xy1-45"));

        assertThat(electrode.allowsBenchTarget()).isTrue();
    }

    private List<AttackEffectDefinition> readCatalog() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Catalog catalog = objectMapper.readValue(Files.readString(CATALOG_PATH), Catalog.class);
        return catalog.effects();
    }

    private record Catalog(List<AttackEffectDefinition> effects) {
    }
}
