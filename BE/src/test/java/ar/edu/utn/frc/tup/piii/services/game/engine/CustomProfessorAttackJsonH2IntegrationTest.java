package ar.edu.utn.frc.tup.piii.services.game.engine;

import ar.edu.utn.frc.tup.piii.configs.Xy1CardSeeder;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.card.CustomProfessorCardJsonSeedService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:custom_professors_attack_json_h2;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomProfessorAttackJsonH2IntegrationTest extends CustomProfessorAttackIntegrationTestSupport {

    @Autowired
    private CustomProfessorCardJsonSeedService customProfessorCardJsonSeedService;

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
    void shouldDeclareEveryJsonSeededCustomAttackWithEnoughEnergy(
            String externalId,
            String attackName,
            String energyTypes,
            int expectedDamageCounters) {
        Card attacker = customProfessorCard(externalId);
        Attack attack = attackByName(attacker, attackName);
        Card defender = saveTestPokemon("test-json-defender-" + attackName.replace(" ", "-"), "Training Dummy", "Colorless", 100);

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
    void shouldDeclareJsonSeededCustomAttackWithColorlessCostAndApplyBaseDamage() {
        Card angularQuin = customProfessorCard("custom-professors-pr-001");
        Attack typescriptazo = attackByName(angularQuin, "TypeScriptazo");
        Card defender = saveTestPokemon("test-json-defender-colorless", "Training Dummy", "Colorless", 100);

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
    void shouldKeepJsonSeedAttackCostsCompleteAndIdempotent() {
        customProfessorCardJsonSeedService.seedCustomProfessorCards();
        customProfessorCardJsonSeedService.seedCustomProfessorCards();

        Long cardCount = jdbcTemplate.queryForObject(
                "select count(*) from cards where set_code = 'custom-professors'",
                Long.class);
        Long attackCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from attacks a
                        join cards c on c.id = a.card_id
                        where c.set_code = 'custom-professors'
                        """,
                Long.class);
        Long costCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from attack_costs ac
                        join attacks a on a.id = ac.attack_id
                        join cards c on c.id = a.card_id
                        where c.set_code = 'custom-professors'
                        """,
                Long.class);

        assertThat(cardCount).isEqualTo(5);
        assertThat(attackCount).isEqualTo(10);
        assertThat(costCount).isEqualTo(16);
    }

    private List<Card> energiesFromCsv(String energyTypes) {
        return Arrays.stream(energyTypes.split(","))
                .map(String::trim)
                .map(this::saveBasicEnergy)
                .toList();
    }
}
