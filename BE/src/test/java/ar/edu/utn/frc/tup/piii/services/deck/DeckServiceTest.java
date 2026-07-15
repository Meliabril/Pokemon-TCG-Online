package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckActivationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckUpsertRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.mappers.DeckMapper;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckPersistenceService;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckValidationServiceImpl;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckPersistenceService deckPersistenceService;

    @Mock
    private CardService cardService;

    @Mock
    private UserService userService;

    @Mock
    private DeckRandomizationService deckRandomizationService;

    @Mock
    private MatchmakingService matchmakingService;

    private DeckServiceImpl deckService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        deckService = new DeckServiceImpl(
                deckPersistenceService,
                cardService,
                userService,
                new DeckValidationServiceImpl(),
                deckRandomizationService,
                matchmakingService,
                new DeckMapper(new CardMapper(CardTranslationService.empty(), mock(AbilityCatalogService.class), objectMapper), objectMapper),
                objectMapper);
    }

    @Test
    void shouldCreateFirstValidDeckAsActive() {
        User owner = user();
        Card pikachu = card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON);
        Card energy = card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        when(userService.getUserEntityById(owner.getId())).thenReturn(owner);
        when(deckPersistenceService.countByOwnerId(owner.getId())).thenReturn(0L);
        when(cardService.getCardEntitiesByIds(List.of(pikachu.getId(), energy.getId())))
                .thenReturn(List.of(pikachu, energy));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.createDeck(owner.getId(), new DeckUpsertRequestDto(
                "Mazo XY1",
                List.of(
                        new DeckCardRequestDto(pikachu.getId(), 4),
                        new DeckCardRequestDto(energy.getId(), 56))));

        assertThat(response.active()).isTrue();
        assertThat(response.valid()).isTrue();
        verify(cardService).getCardEntitiesByIds(List.of(pikachu.getId(), energy.getId()));
        verify(cardService, never()).getCardEntityById(any(UUID.class));
    }

    @Test
    void shouldCreateValidDeckWithCustomProfessorCard() {
        User owner = user();
        Card customBasic = card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE, CardCategory.BASIC_POKEMON);
        Card energy = card("Lightning Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        when(userService.getUserEntityById(owner.getId())).thenReturn(owner);
        when(deckPersistenceService.countByOwnerId(owner.getId())).thenReturn(0L);
        when(cardService.getCardEntitiesByIds(List.of(customBasic.getId(), energy.getId())))
                .thenReturn(List.of(customBasic, energy));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.createDeck(owner.getId(), new DeckUpsertRequestDto(
                "Mazo Profesores",
                List.of(
                        new DeckCardRequestDto(customBasic.getId(), 4),
                        new DeckCardRequestDto(energy.getId(), 56))));

        assertThat(response.active()).isTrue();
        assertThat(response.valid()).isTrue();
        assertThat(response.cards()).anySatisfy(deckCard -> {
            assertThat(deckCard.cardId()).isEqualTo(customBasic.getId());
            assertThat(deckCard.card().name()).isEqualTo("AngularQuin");
        });
    }

    @Test
    void shouldRejectFourthDeckCreation() {
        User owner = user();
        when(userService.getUserEntityById(owner.getId())).thenReturn(owner);
        when(deckPersistenceService.countByOwnerId(owner.getId())).thenReturn(3L);

        assertThatThrownBy(() -> deckService.createDeck(owner.getId(), new DeckUpsertRequestDto("Extra", List.of())))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("at most 3 decks");
    }

    @Test
    void shouldRejectActivatingInvalidDeck() {
        User owner = user();
        Deck invalidDeck = existingDeck(owner, false, false);
        when(deckPersistenceService.findMetadataByIdAndOwnerId(invalidDeck.getId(), owner.getId()))
                .thenReturn(Optional.of(invalidDeck));
        when(deckPersistenceService.getValidationSummary(invalidDeck.getId()))
                .thenReturn(new DeckValidationSummary(4, false, false, List.of()));

        assertThatThrownBy(() -> deckService.activateDeck(owner.getId(), invalidDeck.getId()))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessage("Deck must contain exactly 60 cards; Deck must contain at least 1 Basic Pokemon");

        verify(deckPersistenceService, never()).findByIdAndOwnerIdWithCards(invalidDeck.getId(), owner.getId());
    }

    @Test
    void shouldRejectDeckActivationWhileUserHasActiveGame() {
        User owner = user();
        UUID deckId = UUID.randomUUID();
        when(matchmakingService.hasActiveGame(owner.getId())).thenReturn(true);

        assertThatThrownBy(() -> deckService.activateDeck(owner.getId(), deckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("active game");

        verify(deckPersistenceService, never()).findMetadataByIdAndOwnerId(deckId, owner.getId());
    }

    @Test
    void shouldActivateDeckAfterFlushingPreviousActiveDeckDeactivation() {
        User owner = user();
        Deck activeDeck = existingDeck(owner, true, true);
        Deck targetDeck = existingDeck(owner, false, true);
        targetDeck.setId(UUID.randomUUID());
        when(deckPersistenceService.findMetadataByIdAndOwnerId(targetDeck.getId(), owner.getId()))
                .thenReturn(Optional.of(targetDeck));
        when(deckPersistenceService.getValidationSummary(targetDeck.getId()))
                .thenReturn(new DeckValidationSummary(60, false, true, List.of()));
        when(deckPersistenceService.findActiveDeckMetadataByOwnerId(owner.getId())).thenReturn(Optional.of(activeDeck));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckActivationResponseDto response = deckService.activateDeck(owner.getId(), targetDeck.getId());

        assertThat(response.active()).isTrue();
        assertThat(activeDeck.isActive()).isFalse();
        verify(deckPersistenceService, never()).findByIdAndOwnerIdWithCards(targetDeck.getId(), owner.getId());
        verify(deckPersistenceService, never()).findActiveDeckByOwnerId(owner.getId());

        InOrder inOrder = inOrder(deckPersistenceService);
        inOrder.verify(deckPersistenceService).save(activeDeck);
        inOrder.verify(deckPersistenceService).flush();
        inOrder.verify(deckPersistenceService).save(targetDeck);
    }

    @Test
    void shouldRejectEditingActiveDeckIntoInvalidState() {
        User owner = user();
        Card pikachu = card("Pikachu", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON);
        Card energy = card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        Deck activeDeck = existingDeck(owner, true, true);
        activeDeck.addCard(deckCard(pikachu, 4));
        activeDeck.addCard(deckCard(energy, 56));
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(activeDeck.getId(), owner.getId())).thenReturn(Optional.of(activeDeck));

        assertThatThrownBy(() -> deckService.removeCard(owner.getId(), activeDeck.getId(), pikachu.getId()))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessage("Deck must contain exactly 60 cards; Deck must contain at least 1 Basic Pokemon");

        verify(deckPersistenceService, never()).save(any(Deck.class));
    }

    @Test
    void shouldAddNewCardWithOneBulkLookup() {
        User owner = user();
        Deck deck = existingDeck(owner, false, false);
        Card card = card("Potion", Card.XY1_SET_CODE, CardCategory.ITEM_TRAINER);
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(deck.getId(), owner.getId())).thenReturn(Optional.of(deck));
        when(cardService.getCardEntitiesByIds(List.of(card.getId()))).thenReturn(List.of(card));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.addCard(
                owner.getId(),
                deck.getId(),
                new DeckCardRequestDto(card.getId(), 2));

        assertThat(response.cards()).singleElement().satisfies(deckCard -> {
            assertThat(deckCard.cardId()).isEqualTo(card.getId());
            assertThat(deckCard.quantity()).isEqualTo(2);
        });
        verify(cardService).getCardEntitiesByIds(List.of(card.getId()));
        verify(cardService, never()).getCardEntityById(any(UUID.class));
    }

    @Test
    void shouldRejectDeletingLastDeck() {
        User owner = user();
        Deck deck = existingDeck(owner, true, true);
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(deck.getId(), owner.getId())).thenReturn(Optional.of(deck));
        when(deckPersistenceService.findAllMetadataByOwnerIdOrderByCreatedAtAsc(owner.getId())).thenReturn(List.of(deck));

        assertThatThrownBy(() -> deckService.deleteDeck(owner.getId(), deck.getId()))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("at least one deck");
    }

    @Test
    void shouldDeleteInactiveDeckUsingOwnerDeckMetadata() {
        User owner = user();
        Deck target = existingDeck(owner, false, true);
        Deck activeDeck = existingDeck(owner, true, true);
        activeDeck.setId(UUID.randomUUID());
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(target.getId(), owner.getId()))
                .thenReturn(Optional.of(target));
        when(deckPersistenceService.findAllMetadataByOwnerIdOrderByCreatedAtAsc(owner.getId()))
                .thenReturn(List.of(target, activeDeck));

        deckService.deleteDeck(owner.getId(), target.getId());

        verify(deckPersistenceService).findAllMetadataByOwnerIdOrderByCreatedAtAsc(owner.getId());
        verify(deckPersistenceService, never()).findAllByOwnerIdOrderByCreatedAtAsc(owner.getId());
        verify(deckPersistenceService).delete(target);
        verify(deckPersistenceService).flush();
    }

    @Test
    void shouldRandomizeDeckAndAutoActivateIt() {
        User owner = user();
        Card randomBasic = card("Squirtle", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON);
        Card randomEnergy = card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        Deck activeDeck = existingDeck(owner, true, true);
        Deck targetDeck = existingDeck(owner, false, false);
        targetDeck.setId(UUID.randomUUID());
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(targetDeck.getId(), owner.getId())).thenReturn(Optional.of(targetDeck));
        when(deckPersistenceService.findActiveDeckByOwnerId(owner.getId())).thenReturn(Optional.of(activeDeck));
        when(deckRandomizationService.generateRandomDeck()).thenReturn(randomComposition(
                "Random Water Deck",
                randomBasic, 4,
                randomEnergy, 56));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.randomizeDeck(owner.getId(), targetDeck.getId());

        assertThat(response.active()).isTrue();
        assertThat(response.name()).isEqualTo("Random Water Deck");

        InOrder inOrder = inOrder(deckPersistenceService);
        inOrder.verify(deckPersistenceService).save(activeDeck);
        inOrder.verify(deckPersistenceService).flush();
        inOrder.verify(deckPersistenceService).save(targetDeck);
    }

    @Test
    void shouldRejectDeckRandomizationWhileUserHasActiveGame() {
        User owner = user();
        UUID deckId = UUID.randomUUID();
        when(matchmakingService.hasActiveGame(owner.getId())).thenReturn(true);

        assertThatThrownBy(() -> deckService.randomizeDeck(owner.getId(), deckId))
                .isInstanceOf(InvalidDeckException.class)
                .hasMessageContaining("active game");

        verify(deckPersistenceService, never()).findByIdAndOwnerIdWithCards(deckId, owner.getId());
    }

    @Test
    void shouldCreateRandomDeckAfterFlushingPreviousActiveDeckDeactivation() {
        User owner = user();
        Card randomBasic = card("Squirtle", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON);
        Card randomEnergy = card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        Deck activeDeck = existingDeck(owner, true, true);
        when(userService.getUserEntityById(owner.getId())).thenReturn(owner);
        when(deckPersistenceService.countByOwnerId(owner.getId())).thenReturn(1L);
        when(deckPersistenceService.findActiveDeckByOwnerId(owner.getId()))
                .thenReturn(Optional.of(activeDeck));
        when(deckRandomizationService.generateRandomDeck()).thenReturn(randomComposition(
                "Random Water Deck",
                randomBasic, 4,
                randomEnergy, 56));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.createRandomDeck(owner.getId());

        assertThat(response.active()).isTrue();
        assertThat(activeDeck.isActive()).isFalse();

        InOrder inOrder = inOrder(deckPersistenceService);
        inOrder.verify(deckPersistenceService).save(activeDeck);
        inOrder.verify(deckPersistenceService).flush();
        inOrder.verify(deckPersistenceService).save(any(Deck.class));
        verify(deckPersistenceService).findActiveDeckByOwnerId(owner.getId());
    }

    @Test
    void shouldRandomizeExistingDeckWithoutDuplicatingKeptCards() {
        User owner = user();
        Card randomBasic = card("Squirtle", Card.XY1_SET_CODE, CardCategory.BASIC_POKEMON);
        Card randomEnergy = card("Water Energy", Card.XY1_SET_CODE, CardCategory.BASIC_ENERGY);
        Deck targetDeck = existingDeck(owner, true, true);
        DeckCard existingBasic = deckCard(randomBasic, 4);
        targetDeck.addCard(existingBasic);
        when(deckPersistenceService.findByIdAndOwnerIdWithCards(targetDeck.getId(), owner.getId())).thenReturn(Optional.of(targetDeck));
        when(deckPersistenceService.findActiveDeckByOwnerId(owner.getId())).thenReturn(Optional.of(targetDeck));
        when(deckRandomizationService.generateRandomDeck()).thenReturn(randomComposition(
                "Random Water Deck",
                randomBasic, 3,
                randomEnergy, 57));
        when(deckPersistenceService.save(any(Deck.class))).thenAnswer(invocation -> savedDeck(invocation.getArgument(0)));

        DeckResponseDto response = deckService.randomizeDeck(owner.getId(), targetDeck.getId());

        assertThat(response.cards()).hasSize(2);
        assertThat(response.cards()).anySatisfy(card -> {
            assertThat(card.id()).isEqualTo(existingBasic.getId());
            assertThat(card.cardId()).isEqualTo(randomBasic.getId());
            assertThat(card.quantity()).isEqualTo(3);
        });
        assertThat(response.cards()).anySatisfy(card -> {
            assertThat(card.cardId()).isEqualTo(randomEnergy.getId());
            assertThat(card.quantity()).isEqualTo(57);
        });
        verify(cardService, never()).getCardEntityById(any(UUID.class));
    }

    private RandomDeckComposition randomComposition(
            String name,
            Card firstCard,
            int firstQuantity,
            Card secondCard,
            int secondQuantity) {
        return new RandomDeckComposition(
                name,
                List.of(
                        new RandomDeckComposition.CardEntry(firstCard, firstQuantity),
                        new RandomDeckComposition.CardEntry(secondCard, secondQuantity)));
    }

    private Deck savedDeck(Deck deck) {
        if (deck.getId() == null) {
            deck.setId(UUID.randomUUID());
        }
        deck.setCreatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        deck.setUpdatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        for (DeckCard deckCard : deck.getCards()) {
            if (deckCard.getId() == null) {
                deckCard.setId(UUID.randomUUID());
            }
        }
        return deck;
    }

    private Deck existingDeck(User owner, boolean active, boolean valid) {
        Deck deck = new Deck();
        deck.setId(UUID.randomUUID());
        deck.setOwner(owner);
        deck.setName("Mazo XY1");
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        deck.setActive(active);
        deck.setValid(valid);
        deck.setCreatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        deck.setUpdatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        return deck;
    }

    private DeckCard deckCard(Card card, int quantity) {
        DeckCard deckCard = new DeckCard();
        deckCard.setId(UUID.randomUUID());
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private Card card(String name, String setCode, CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(UUID.randomUUID().toString());
        card.setSetCode(setCode);
        card.setSetName("XY");
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(supertype(category));
        card.setCategory(category);
        card.setRawJson("{}");
        if (category == CardCategory.BASIC_POKEMON || category == CardCategory.STAGE_1_POKEMON || category == CardCategory.STAGE_2_POKEMON) {
            card.setPokemonType("Water");
        }
        if (category == CardCategory.BASIC_POKEMON) {
            card.setSubtype("Basic");
        }
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

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("player@gmail.com");
        user.setUsername("player");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-18T12:00:00Z"));
        return user;
    }
}
