package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckValidationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalTestingDeckSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CardRepository cardRepository;

    private LocalTestingDeckSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new LocalTestingDeckSeeder(
                userRepository,
                deckRepository,
                cardRepository,
                new DeckValidationServiceImpl(),
                new ObjectMapper());
    }

    @Test
    void shouldSkipWhenNoDefaultSeededDecksAreConfigured() {
        seeder.run();

        verifyNoInteractions(userRepository, deckRepository);
    }

    @Test
    void shouldNotCreateSeedDecksEvenIfUsersExist() {
        Deck existingDeck = new Deck();
        existingDeck.setName("Seeded Admin XY1 Deck");
        existingDeck.setValid(true);
        existingDeck.setActive(true);

        seeder.run();

        verifyNoInteractions(userRepository, deckRepository);
        verify(deckRepository, never()).save(existingDeck);
    }

    private List<Card> seedableCards() {
        return List.of(
                card("xy1-basic-1", "Bulbasaur", "1", CardCategory.BASIC_POKEMON),
                card("xy1-basic-2", "Squirtle", "2", CardCategory.BASIC_POKEMON),
                card("xy1-supporter-1", "Professor Sycamore", "3", CardCategory.SUPPORTER_TRAINER),
                card("xy1-supporter-2", "Shauna", "4", CardCategory.SUPPORTER_TRAINER),
                card("xy1-energy-1", "Grass Energy", "5", CardCategory.BASIC_ENERGY),
                card("xy1-energy-2", "Water Energy", "6", CardCategory.BASIC_ENERGY));
    }

    private Card card(String externalId, String name, String number, CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(externalId);
        card.setSetCode(Card.XY1_SET_CODE);
        card.setSetName("XY");
        card.setNumber(number);
        card.setName(name);
        card.setCategory(category);
        card.setSupertype(supertype(category));
        card.setRawJson("{}");
        return card;
    }

    private CardSupertype supertype(CardCategory category) {
        if (category.name().contains("POKEMON")) {
            return CardSupertype.POKEMON;
        }
        if (category.name().contains("ENERGY")) {
            return CardSupertype.ENERGY;
        }
        return CardSupertype.TRAINER;
    }

    private User user(String email, String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.parse("2026-05-30T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-30T00:00:00Z"));
        return user;
    }
}
