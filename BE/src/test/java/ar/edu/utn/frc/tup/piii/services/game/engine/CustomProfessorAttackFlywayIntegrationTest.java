package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.configs.CustomProfessorCardJsonSeeder;
import ar.edu.utn.frc.tup.piii.configs.Xy1CardSeeder;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:custom_professors_attack_flyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CustomProfessorAttackFlywayIntegrationTest extends CustomProfessorAttackIntegrationTestSupport {

    @MockBean
    private CustomProfessorCardJsonSeeder customProfessorCardJsonSeeder;

    @MockBean
    private Xy1CardSeeder xy1CardSeeder;

    @ParameterizedTest
    @CsvSource({
            "custom-professors-pr-001, RelampagoLM, Lightning, 2",
            "custom-professors-pr-001, TypeScriptazo, 'Lightning,Water', 5",
            "custom-professors-pr-002, Scirocco, Metal, 2",
            "custom-professors-pr-002, Scaffolding Destructor, 'Metal,Metal,Water', 7",
            "custom-professors-pr-003, Roba Tokens, Darkness, 3",
            "custom-professors-pr-003, Deploy Pirata, 'Darkness,Darkness,Water', 8",
            "custom-professors-pr-004, Cuestionario Incendiario, 'Fire,Water', 3",
            "custom-professors-pr-004, Dos del Toro, 'Fire,Fire,Water,Water', 9",
            "custom-professors-pr-005, Ruleta Denigrante, Psychic, 3",
            "custom-professors-pr-005, Idempotencia Inmutable, 'Psychic,Psychic,Water', 9"
    })
    void shouldDeclareEveryFlywayCustomAttackWithEnoughEnergy(
            String externalId,
            String attackName,
            String energyTypes,
            int expectedDamageCounters) {
        Card attacker = customProfessorCard(externalId);
        Attack attack = attackByName(attacker, attackName);
        Card defender = saveTestPokemon("test-flyway-defender-" + attackName.replace(" ", "-"), "Training Dummy", "Colorless", 100);

        assertThat(attack.getId()).isNotNull();
        assertThat(attack.getCosts()).isNotEmpty();
        assertThat(attack.getEffectText()).isNull();

        AttackGameFixture fixture = insertAttackGame(
                attacker,
                attack,
                energiesFromCsv(energyTypes),
                defender);

        assertAttackVisibleAndAffordableBeforeAction(fixture);
        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, expectedDamageCounters);
    }

    @Test
    void shouldDeclareFlywayCustomAttackWithExactEnergyAndApplyBaseDamage() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack relampago = attackByName(angularQuin, "RelampagoLM");
        Card defender = saveTestPokemon("test-flyway-defender-exact", "Training Dummy", "Colorless", 100);

        AttackGameFixture fixture = insertAttackGame(
                angularQuin,
                relampago,
                energies("Lightning"),
                defender);

        assertAttackVisibleAndAffordableBeforeAction(fixture);
        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, 2);
    }

    @Test
    void shouldDeclareFlywayCustomColorlessCostAttackWithEnoughEnergy() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack typescriptazo = attackByName(angularQuin, "TypeScriptazo");
        Card defender = saveTestPokemon("test-flyway-defender-colorless", "Training Dummy", "Colorless", 100);

        AttackGameFixture fixture = insertAttackGame(
                angularQuin,
                typescriptazo,
                energies("Lightning", "Water"),
                defender);

        assertAttackVisibleAndAffordableBeforeAction(fixture);
        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, 5);
    }

    @Test
    void shouldRejectFlywayCustomAttackWithoutEnoughEnergy() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack typescriptazo = attackByName(angularQuin, "TypeScriptazo");
        Card defender = saveTestPokemon("test-flyway-defender-reject", "Training Dummy", "Colorless", 100);

        AttackGameFixture fixture = insertAttackGame(
                angularQuin,
                typescriptazo,
                energies("Lightning"),
                defender);

        assertThatThrownBy(() -> declareAttack(fixture))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Not enough energy attached to perform this attack");
    }

    @Test
    void shouldApplyWeaknessToFlywayCustomAttackDamage() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack relampago = attackByName(angularQuin, "RelampagoLM");
        Card defender = saveTestPokemonWithWeakness(
                "test-flyway-defender-lightning-weakness",
                "Weak Dummy",
                "Colorless",
                100,
                "Lightning",
                "x2");

        AttackGameFixture fixture = insertAttackGame(
                angularQuin,
                relampago,
                energies("Lightning"),
                defender);

        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, 4);
    }

    @Test
    void shouldApplyResistanceToFlywayCustomAttackDamage() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack relampago = attackByName(angularQuin, "RelampagoLM");
        Card defender = saveTestPokemonWithResistance(
                "test-flyway-defender-lightning-resistance",
                "Resistant Dummy",
                "Colorless",
                100,
                "Lightning",
                "-20");

        AttackGameFixture fixture = insertAttackGame(
                angularQuin,
                relampago,
                energies("Lightning"),
                defender);

        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, 0);
    }

    @Test
    void shouldKeepXy1DamageOnlyAttackWorking() {
        Card attacker = saveTestPokemon("xy1-test-basic-attacker", "Fire Test Mon", "Fire", 100);
        Attack flare = saveAttack(attacker, "Flare", 20, "Fire", 1);
        Card defender = saveTestPokemon("test-xy1-defender", "Training Dummy", "Colorless", 100);

        AttackGameFixture fixture = insertAttackGame(
                cardRepository.findWithDetailsById(attacker.getId()).orElseThrow(),
                flare,
                List.of(saveBasicEnergy("Fire")),
                defender);

        GameActionResponseDto response = declareAttack(fixture);

        assertThat(response.success()).isTrue();
        assertDamageAndEvents(fixture, 2);
    }

    private List<Card> energiesFromCsv(String energyTypes) {
        return Arrays.stream(energyTypes.split(","))
                .map(String::trim)
                .map(this::saveBasicEnergy)
                .toList();
    }
}
