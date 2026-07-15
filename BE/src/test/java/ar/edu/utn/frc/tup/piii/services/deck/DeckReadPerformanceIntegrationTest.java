package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.mappers.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.projections.DeckValidationSummaryProjection;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.open-in-view=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
class DeckReadPerformanceIntegrationTest {

    private static final int EXPECTED_READ_QUERY_COUNT = 6;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldMapDeckDetailsWithBoundedBatchQueries() {
        User owner = user();
        entityManager.persist(owner);
        Deck deck = deck(owner);
        for (int index = 1; index <= 3; index++) {
            Card card = card(index);
            entityManager.persist(card);
            deck.addCard(deckCard(card));
        }
        entityManager.persist(deck);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<Deck> orderedDecks = deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(owner.getId());
        List<UUID> deckIds = orderedDecks.stream().map(Deck::getId).toList();
        Deck loadedDeck = deckRepository.findAllWithCardsByIdIn(deckIds).getFirst();
        ObjectMapper objectMapper = new ObjectMapper();
        DeckResponseDto response = new DeckMapper(
                new CardMapper(CardTranslationService.empty(), mock(AbilityCatalogService.class), objectMapper),
                objectMapper).toDto(loadedDeck);

        assertThat(response.cards()).hasSize(3);
        assertThat(response.cards()).allSatisfy(deckCard -> {
            assertThat(deckCard.card().attacks()).hasSize(1);
            assertThat(deckCard.card().attacks().getFirst().costs()).hasSize(1);
            assertThat(deckCard.card().weaknesses()).hasSize(1);
            assertThat(deckCard.card().resistances()).hasSize(1);
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(EXPECTED_READ_QUERY_COUNT);
    }

    @Test
    void shouldValidatePersistedDeckWithoutHydratingCards() {
        User owner = user();
        entityManager.persist(owner);
        Deck deck = deck(owner);
        Card card = card(1);
        entityManager.persist(card);
        DeckCard deckCard = deckCard(card);
        deckCard.setQuantity(60);
        deck.addCard(deckCard);
        entityManager.persist(deck);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Deck loadedDeck = deckRepository.findMetadataByIdAndOwnerId(deck.getId(), owner.getId()).orElseThrow();
        DeckValidationSummaryProjection summary = deckRepository.findValidationSummary(
                deck.getId(),
                Card.PLAYABLE_SET_CODES);
        List<String> duplicatedNames = deckRepository.findDuplicatedCardNames(
                deck.getId(),
                CardCategory.BASIC_ENERGY,
                4);

        assertThat(Hibernate.isInitialized(loadedDeck.getCards())).isFalse();
        assertThat(summary.getTotalCards()).isEqualTo(60);
        assertThat(summary.getCardsOutsideSet()).isZero();
        assertThat(summary.getBasicPokemonCards()).isEqualTo(1);
        assertThat(duplicatedNames).containsExactly("Pokemon 1");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    private User user() {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setUsername("user-" + UUID.randomUUID());
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Deck deck(User owner) {
        Deck deck = new Deck();
        deck.setOwner(owner);
        deck.setName("Performance Deck");
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        deck.setValid(true);
        deck.setActive(true);
        deck.setValidationErrors("[]");
        return deck;
    }

    private DeckCard deckCard(Card card) {
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setQuantity(1);
        return deckCard;
    }

    private Card card(int index) {
        Card card = new Card();
        card.setExternalId("xy1-performance-" + index);
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setNumber(Integer.toString(index));
        card.setName("Pokemon " + index);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setSubtype("Basic");
        card.setRawJson("{}");
        card.addAttack(attack(index));
        card.addWeakness(weakness(index));
        card.addResistance(resistance(index));
        return card;
    }

    private Attack attack(int index) {
        Attack attack = new Attack();
        attack.setName("Attack " + index);
        attack.setAttackOrder(1);
        attack.addCost(cost());
        return attack;
    }

    private AttackCost cost() {
        AttackCost cost = new AttackCost();
        cost.setEnergyType("Water");
        cost.setQuantity(1);
        return cost;
    }

    private CardWeakness weakness(int index) {
        CardWeakness weakness = new CardWeakness();
        weakness.setEnergyType("Fire-" + index);
        weakness.setMultiplier("x2");
        return weakness;
    }

    private CardResistance resistance(int index) {
        CardResistance resistance = new CardResistance();
        resistance.setEnergyType("Metal-" + index);
        resistance.setValue("-20");
        return resistance;
    }
}
