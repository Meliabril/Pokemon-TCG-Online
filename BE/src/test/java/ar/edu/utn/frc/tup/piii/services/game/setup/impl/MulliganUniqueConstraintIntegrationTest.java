package ar.edu.utn.frc.tup.piii.services.game.setup.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mulligan_unique_constraint_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MulliganUniqueConstraintIntegrationTest {

    @Autowired
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Test
    @Transactional
    void shouldReorderZoneUsingNegativeTemporaryPositionsWithoutUniqueConstraintViolations() {
        UUID gameId = UUID.fromString("11111111-2222-3333-4444-555555555555");

        // Save a test user (id will be auto-generated)
        User user = new User();
        user.setEmail("test-user@mulligan.com");
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user = userRepository.saveAndFlush(user);

        UUID ownerUserId = user.getId();

        // Save a test game
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.SETUP);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        game = gameRepository.saveAndFlush(game);

        // Find or save a test card
        Card dummyCard = cardRepository.findAll().stream().findFirst().orElseGet(() -> {
            Card card = new Card();
            card.setId(UUID.randomUUID());
            card.setExternalId("dummy-card-id");
            card.setName("Dummy Card");
            card.setSetCode("xy1");
            card.setSetName("XY");
            card.setNumber("1");
            card.setSupertype(CardSupertype.POKEMON);
            card.setCategory(CardCategory.BASIC_POKEMON);
            card.setRawJson("{}");
            return cardRepository.saveAndFlush(card);
        });

        // Prepare a list of 5 game card instances currently in DECK zone
        List<GameCardInstance> cards = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            GameCardInstance card = new GameCardInstance();
            card.setId(UUID.randomUUID());
            card.setGame(game);
            card.setOwnerUserId(ownerUserId);
            card.setCardId(dummyCard.getId());
            card.setZone(CardZone.DECK);
            card.setZonePosition(i);
            card.setFaceDown(true);
            cards.add(card);
        }

        // Save initially
        gameCardInstanceStateService.saveAll(cards);
        gameCardInstanceStateService.flush();

        // Reorder them: shift positions in-place
        // card at 1 -> position 2, card at 2 -> position 1 (solapamiento temporal directo)
        List<GameCardInstance> reorderedCards = List.of(
                cards.get(1),
                cards.get(0),
                cards.get(2),
                cards.get(3),
                cards.get(4)
        );

        // Perform reorder operation
        gameCardInstanceStateService.reorderAndPersistZone(gameId, ownerUserId, CardZone.DECK, reorderedCards);

        // Verify the database has the expected positions
        List<GameCardInstance> persistedCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId, ownerUserId, CardZone.DECK
        );

        assertThat(persistedCards).hasSize(5);
        for (int i = 0; i < persistedCards.size(); i++) {
            assertThat(persistedCards.get(i).getZonePosition()).isEqualTo(i + 1);
        }
    }
}
