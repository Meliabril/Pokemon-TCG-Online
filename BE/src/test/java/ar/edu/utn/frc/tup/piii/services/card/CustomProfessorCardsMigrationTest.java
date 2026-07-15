package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomProfessorCardsMigrationTest {

    private static final String CUSTOM_IMAGE_BASE_URL =
            "https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/";

    private static final String ANGULARQUIN_IMAGE_URL =
            CUSTOM_IMAGE_BASE_URL + "147-151-angularquinINGLES.png";

    private static final List<String> FINAL_IMAGE_URLS = List.of(
            CUSTOM_IMAGE_BASE_URL + "147-151-angularquinINGLES.png",
            CUSTOM_IMAGE_BASE_URL + "148-151-ramavenINGLES.png",
            CUSTOM_IMAGE_BASE_URL + "149-151-belgranodeINGLES.png",
            CUSTOM_IMAGE_BASE_URL + "150-151-santormentoINGLES.png",
            CUSTOM_IMAGE_BASE_URL + "151-151-hernullpointerINGLES.png");

    private static final List<String> EXPECTED_ATTACK_SPECS = List.of(
            "custom-professors-pr-001|0|RelampagoLM|20|20|Colorless:0|Lightning:1",
            "custom-professors-pr-001|1|TypeScriptazo|50|50|Colorless:1|Lightning:1",
            "custom-professors-pr-002|0|Scirocco|20|20|Colorless:0|Metal:1",
            "custom-professors-pr-002|1|Scaffolding Destructor|70|70|Colorless:1|Metal:2",
            "custom-professors-pr-003|0|Roba Tokens|30|30|Colorless:0|Darkness:1",
            "custom-professors-pr-003|1|Deploy Pirata|80|80|Colorless:1|Darkness:2",
            "custom-professors-pr-004|0|Cuestionario Incendiario|30|30|Colorless:1|Fire:1",
            "custom-professors-pr-004|1|Dos del Toro|90|90|Colorless:2|Fire:2",
            "custom-professors-pr-005|0|Ruleta Denigrante|30|30|Colorless:0|Psychic:1",
            "custom-professors-pr-005|1|Idempotencia Inmutable|90|90|Colorless:1|Psychic:2");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CardRepository cardRepository;

    @Test
    void shouldSeedCustomProfessorCardsWithCombatData() {
        assertThat(count("""
                select count(*)
                from cards
                where set_code = 'custom-professors'
                """)).isEqualTo(5);
        assertThat(count("""
                select count(*)
                from cards
                where set_code = 'custom-professors'
                  and category = 'BASIC_POKEMON'
                  and source = 'CUSTOM_PROFESSORS'
                """)).isEqualTo(5);
        assertThat(names()).containsExactly(
                "AngularQuin",
                "RaMaven",
                "BelgraNode",
                "SantorMento",
                "HernullPointer");
        assertThat(count("""
                select count(*)
                from attacks a
                join cards c on c.id = a.card_id
                where c.set_code = 'custom-professors'
                """)).isEqualTo(10);
        assertThat(count("""
                select count(*)
                from attacks a
                join cards c on c.id = a.card_id
                where c.set_code = 'custom-professors'
                  and a.effect_text is not null
                """)).isZero();
        assertThat(count("""
                select count(*)
                from attack_costs ac
                join attacks a on a.id = ac.attack_id
                join cards c on c.id = a.card_id
                where c.set_code = 'custom-professors'
                """)).isEqualTo(16);
        assertThat(count("""
                select count(*)
                from card_weaknesses cw
                join cards c on c.id = cw.card_id
                where c.set_code = 'custom-professors'
                """)).isEqualTo(5);
        assertThat(count("""
                select count(*)
                from card_resistances cr
                join cards c on c.id = cr.card_id
                where c.set_code = 'custom-professors'
                """)).isEqualTo(5);
    }

    @Test
    void shouldSeedExactCustomProfessorAttackCostsAndDamage() {
        List<Card> cards = cardRepository.findBySetCodeOrderByNumberAsc(Card.CUSTOM_PROFESSORS_SET_CODE);

        assertThat(cards)
                .flatExtracting(Card::getAttacks)
                .extracting(Attack::getId)
                .allSatisfy(id -> assertThat(id).isInstanceOf(UUID.class))
                .doesNotHaveDuplicates();
        assertThat(attackSpecs(cards)).containsExactlyElementsOf(EXPECTED_ATTACK_SPECS);
    }

    @Test
    void shouldKeepXy1ConstraintCompatibleWithPlayableSets() {
        assertThat(Card.PLAYABLE_SET_CODES).containsExactly(Card.XY1_SET_CODE, Card.CUSTOM_PROFESSORS_SET_CODE);
    }

    @Test
    void shouldSetFinalImageUrlsForAllCustomProfessorCards() {
        assertThat(imageUrls("image_small_url")).containsExactlyElementsOf(FINAL_IMAGE_URLS);
        assertThat(imageUrls("image_large_url")).containsExactlyElementsOf(FINAL_IMAGE_URLS);

        assertThat(count("""
                select count(*)
                from cards
                where set_code = 'custom-professors'
                  and (
                      image_small_url = ''
                      or image_large_url = ''
                      or image_small_url like '%pr-001-angularquin-large-test%'
                      or image_large_url like '%pr-001-angularquin-large-test%'
                  )
                """)).isZero();
    }

    @Test
    void shouldSetFinalCustomProfessorAttackNames() {
        assertThat(attackNames()).contains(
                "RelampagoLM",
                "Dos del Toro",
                "Idempotencia Inmutable");
        assertThat(attackNames()).doesNotContain(
                "Dos del Cornudo",
                "Singleton Supremo");
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result;
    }

    private List<String> names() {
        return jdbcTemplate.queryForList("""
                select name
                from cards
                where set_code = 'custom-professors'
                order by number
                """, String.class);
    }

    private List<String> imageUrls(String columnName) {
        return jdbcTemplate.queryForList("""
                select %s
                from cards
                where set_code = 'custom-professors'
                order by number
                """.formatted(columnName), String.class);
    }

    private List<String> attackNames() {
        return jdbcTemplate.queryForList("""
                select a.name
                from attacks a
                join cards c on c.id = a.card_id
                where c.set_code = 'custom-professors'
                order by c.number, a.attack_order
                """, String.class);
    }

    private List<String> attackSpecs(List<Card> cards) {
        return cards.stream()
                .flatMap(card -> card.getAttacks().stream()
                        .sorted(Comparator.comparingInt(Attack::getAttackOrder))
                        .map(attack -> attackSpec(card, attack)))
                .toList();
    }

    private String attackSpec(Card card, Attack attack) {
        return String.join("|",
                card.getExternalId(),
                String.valueOf(attack.getAttackOrder()),
                attack.getName(),
                attack.getDamageText(),
                String.valueOf(attack.getBaseDamage()),
                costQuantity(attack, "Colorless"),
                typedCost(attack));
    }

    private String costQuantity(Attack attack, String energyType) {
        int quantity = attack.getCosts().stream()
                .filter(cost -> energyType.equals(cost.getEnergyType()))
                .mapToInt(AttackCost::getQuantity)
                .sum();
        return energyType + ":" + quantity;
    }

    private String typedCost(Attack attack) {
        return attack.getCosts().stream()
                .filter(cost -> !"Colorless".equals(cost.getEnergyType()))
                .findFirst()
                .map(cost -> cost.getEnergyType() + ":" + cost.getQuantity())
                .orElse("Typed:0");
    }
}
