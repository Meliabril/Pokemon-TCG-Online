package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Order(2)
public class LocalTestingDeckSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTestingDeckSeeder.class);
    private static final List<SeededDeckConfig> SEEDED_DECKS = List.of();

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckValidationService deckValidationService;
    private final ObjectMapper objectMapper;

    public LocalTestingDeckSeeder(
            UserRepository userRepository,
            DeckRepository deckRepository,
            CardRepository cardRepository,
            DeckValidationService deckValidationService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckValidationService = deckValidationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (SEEDED_DECKS.isEmpty()) {
            LOGGER.info("Local testing deck seed skipped: no default seeded decks configured");
            return;
        }

        List<Card> xy1Cards = cardRepository.findBySetCodeOrderByNumberAsc(Card.XY1_SET_CODE);
        if (xy1Cards.isEmpty()) {
            LOGGER.warn("Local testing deck seed skipped: no XY1 cards loaded yet");
            return;
        }

        for (SeededDeckConfig config : SEEDED_DECKS) {
            userRepository.findByEmailIgnoreCase(config.email())
                    .ifPresent(user -> seedDeckIfMissing(user, config, xy1Cards));
        }

        LOGGER.info("Local testing fixtures ready for permanent admins");
    }

    private void seedDeckIfMissing(User user, SeededDeckConfig config, List<Card> xy1Cards) {
        List<Deck> existingDecks = deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(user.getId());
        boolean hasActiveDeck = existingDecks.stream().anyMatch(Deck::isActive);
        boolean seedDeckExists = existingDecks.stream()
                .anyMatch(deck -> deck.getName().equalsIgnoreCase(config.deckName()));

        if (seedDeckExists) {
            LOGGER.info("Local testing deck seed skipped for {}: {} already exists", user.getEmail(), config.deckName());
            return;
        }

        Deck deck = new Deck();
        deck.setOwner(user);
        deck.setName(config.deckName());
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        deck.setActive(!hasActiveDeck);
        buildDeckCards(deck, xy1Cards, config);
        applyValidation(deck);
        deckRepository.save(deck);
        LOGGER.info("Local testing deck seeded for {}: {}", user.getEmail(), config.deckName());
    }

    private void buildDeckCards(Deck deck, List<Card> xy1Cards, SeededDeckConfig config) {
        Card basicPokemon = requiredCard(xy1Cards, CardCategory.BASIC_POKEMON, config.basicIndex());
        Card trainer = optionalCard(xy1Cards, CardCategory.SUPPORTER_TRAINER, config.trainerIndex())
                .or(() -> optionalCard(xy1Cards, CardCategory.ITEM_TRAINER, config.trainerIndex()))
                .orElseThrow(() -> new IllegalStateException("Could not build local testing deck: missing XY1 trainer card"));
        Card basicEnergy = requiredCard(xy1Cards, CardCategory.BASIC_ENERGY, config.energyIndex());

        deck.addCard(deckCard(basicPokemon, 4));
        deck.addCard(deckCard(trainer, 4));
        deck.addCard(deckCard(basicEnergy, 52));
    }

    private Card requiredCard(List<Card> xy1Cards, CardCategory category, int preferredIndex) {
        return optionalCard(xy1Cards, category, preferredIndex)
                .orElseThrow(() -> new IllegalStateException("Could not build local testing deck: missing XY1 " + category));
    }

    private Optional<Card> optionalCard(List<Card> xy1Cards, CardCategory category, int preferredIndex) {
        List<Card> matchingCards = xy1Cards.stream()
                .filter(card -> card.getCategory() == category)
                .toList();
        if (matchingCards.isEmpty()) {
            return Optional.empty();
        }
        int resolvedIndex = Math.min(preferredIndex, matchingCards.size() - 1);
        return Optional.of(matchingCards.get(resolvedIndex));
    }

    private DeckCard deckCard(Card card, int quantity) {
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private void applyValidation(Deck deck) {
        DeckValidationResult validation = deckValidationService.validate(deck);
        if (validation == null) {
            LOGGER.warn("Local testing deck validation returned null; defaulting seeded deck to valid");
            validation = new DeckValidationResult(true, List.of());
        }

        deck.setValid(validation.valid());
        try {
            deck.setValidationErrors(objectMapper.writeValueAsString(new ArrayList<>(validation.errors())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize local testing deck validation errors", exception);
        }
    }

    private record SeededDeckConfig(String email, String deckName, int basicIndex, int trainerIndex, int energyIndex) {
    }
}
