package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.card.impl.CustomProfessorCardJsonSeedServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomProfessorCardJsonSeedServiceTest {

    private static final String CUSTOM_IMAGE_BASE_URL =
            "https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/";

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
    private CardRepository cardRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CustomProfessorCardJsonSeedServiceImpl seedService;

    @BeforeEach
    void setUp() {
        seedService = new CustomProfessorCardJsonSeedServiceImpl(cardRepository, new ObjectMapper());
    }

    @Test
    void shouldLoadCustomProfessorCardsFromJsonWithoutFlyway() {
        int seededCards = seedService.seedCustomProfessorCards();
        entityManager.flush();
        entityManager.clear();

        assertThat(seededCards).isEqualTo(5);
        List<Card> cards = customProfessorCards();
        assertThat(cards).hasSize(5);
        assertThat(cards).extracting(Card::getName).containsExactly(
                "AngularQuin",
                "RaMaven",
                "BelgraNode",
                "SantorMento",
                "HernullPointer");
        assertThat(cards).allSatisfy(card -> {
            assertThat(card.getSetCode()).isEqualTo(Card.CUSTOM_PROFESSORS_SET_CODE);
            assertThat(card.getSetName()).isEqualTo("Profesores TPI");
            assertThat(card.getSource()).isEqualTo(Card.SOURCE_CUSTOM_PROFESSORS);
            assertThat(card.getSubtype()).isEqualTo("Basic");
            assertThat(card.isBasicStage()).isTrue();
            assertThat(card.getRawJson()).contains(card.getExternalId());
            assertThat(card.getRawJson()).contains("\"rarity\":");
        });
        assertThat(cards).extracting(Card::getImageSmallUrl).containsExactlyElementsOf(FINAL_IMAGE_URLS);
        assertThat(cards).extracting(Card::getImageLargeUrl).containsExactlyElementsOf(FINAL_IMAGE_URLS);
        assertThat(countAttacks(cards)).isEqualTo(10);
        assertThat(countAttackCosts(cards)).isEqualTo(16);
        assertThat(countWeaknesses(cards)).isEqualTo(5);
        assertThat(countResistances(cards)).isEqualTo(5);
        assertThat(attackNames(cards)).contains(
                "RelampagoLM",
                "Dos del Toro",
                "Idempotencia Inmutable");
        assertThat(attackNames(cards)).doesNotContain(
                "Dos del Cornudo",
                "Singleton Supremo");
        assertThat(cards).flatExtracting(Card::getAttacks)
                .allSatisfy(attack -> assertThat(attack.getEffectText()).isNull());
        assertThat(cards).flatExtracting(Card::getAttacks)
                .extracting(Attack::getId)
                .allSatisfy(id -> assertThat(id).isInstanceOf(UUID.class))
                .doesNotHaveDuplicates();
        assertThat(attackSpecs(cards)).containsExactlyElementsOf(EXPECTED_ATTACK_SPECS);
    }

    @Test
    void shouldNotDuplicateCustomProfessorCardsWhenJsonSeedRunsMoreThanOnce() {
        seedService.seedCustomProfessorCards();
        entityManager.flush();
        entityManager.clear();

        seedService.seedCustomProfessorCards();
        entityManager.flush();
        entityManager.clear();

        List<Card> cards = customProfessorCards();
        assertThat(cards).hasSize(5);
        assertThat(cards).extracting(Card::getExternalId).doesNotHaveDuplicates();
        assertThat(countAttacks(cards)).isEqualTo(10);
        assertThat(countAttackCosts(cards)).isEqualTo(16);
        assertThat(countWeaknesses(cards)).isEqualTo(5);
        assertThat(countResistances(cards)).isEqualTo(5);
        assertThat(attackSpecs(cards)).containsExactlyElementsOf(EXPECTED_ATTACK_SPECS);
    }

    private List<Card> customProfessorCards() {
        return cardRepository.findBySetCodeOrderByNumberAsc(Card.CUSTOM_PROFESSORS_SET_CODE);
    }

    private int countAttacks(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getAttacks().size())
                .sum();
    }

    private int countAttackCosts(List<Card> cards) {
        return cards.stream()
                .flatMap(card -> card.getAttacks().stream())
                .mapToInt(attack -> attack.getCosts().size())
                .sum();
    }

    private int countWeaknesses(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getWeaknesses().size())
                .sum();
    }

    private int countResistances(List<Card> cards) {
        return cards.stream()
                .mapToInt(card -> card.getResistances().size())
                .sum();
    }

    private List<String> attackNames(List<Card> cards) {
        return cards.stream()
                .flatMap(card -> card.getAttacks().stream())
                .map(Attack::getName)
                .toList();
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
