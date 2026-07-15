package ar.edu.utn.frc.tup.piii.services.deck.impl;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckActivationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckUpsertRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.DeckMapper;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckPersistenceService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckRandomizationService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import ar.edu.utn.frc.tup.piii.services.deck.RandomDeckComposition;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {

    private static final int MAX_DECKS_PER_USER = 3;

    private final DeckPersistenceService deckPersistenceService;
    private final CardService cardService;
    private final UserService userService;
    private final DeckValidationService deckValidationService;
    private final DeckRandomizationService deckRandomizationService;
    private final MatchmakingService matchmakingService;
    private final DeckMapper deckMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DeckResponseDto> getMyDecks(UUID ownerUserId) {
        return deckPersistenceService.findAllByOwnerIdOrderByCreatedAtAsc(ownerUserId).stream()
                .map(deckMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DeckResponseDto createDeck(UUID ownerUserId, DeckUpsertRequestDto request) {
        User owner = userService.getUserEntityById(ownerUserId);
        long currentDeckCount = deckPersistenceService.countByOwnerId(ownerUserId);
        if (currentDeckCount >= MAX_DECKS_PER_USER) {
            throw new InvalidDeckException("User can have at most 3 decks");
        }

        Deck deck = new Deck();
        deck.setOwner(owner);
        deck.setName(request.name());
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        replaceCards(deck, resolveCards(request));
        DeckValidationResult validation = applyValidation(deck);

        if (currentDeckCount == 0) {
            ensureDeckValidForActivation(validation);
            deck.setActive(true);
        } else {
            deck.setActive(false);
        }

        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional
    public DeckResponseDto createRandomDeck(UUID ownerUserId) {
        assertCanChangeDeckDuringGame(ownerUserId);
        User owner = userService.getUserEntityById(ownerUserId);
        long currentDeckCount = deckPersistenceService.countByOwnerId(ownerUserId);
        if (currentDeckCount >= MAX_DECKS_PER_USER) {
            throw new InvalidDeckException("User can have at most 3 decks");
        }

        RandomDeckComposition generatedDeck = deckRandomizationService.generateRandomDeck();
        Deck currentActive = deckPersistenceService.findActiveDeckByOwnerId(ownerUserId).orElse(null);
        leaveQueueIfNeeded(ownerUserId, currentActive != null);
        if (currentActive != null) {
            deactivateActiveDeck(currentActive);
        }

        Deck deck = new Deck();
        deck.setOwner(owner);
        deck.setName(generatedDeck.name());
        deck.setFormat(Deck.XY1_UNLIMITED_FORMAT);
        replaceCards(deck, generatedDeck);
        DeckValidationResult validation = applyValidation(deck);
        ensureDeckValidForActivation(validation);
        deck.setActive(true);
        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional(readOnly = true)
    public DeckResponseDto getMyDeck(UUID ownerUserId, UUID deckId) {
        return deckMapper.toDto(loadDeck(ownerUserId, deckId));
    }

    @Override
    @Transactional
    public DeckResponseDto replaceMyDeck(UUID ownerUserId, UUID deckId, DeckUpsertRequestDto request) {
        Deck deck = loadDeck(ownerUserId, deckId);
        ResolvedDeckCards resolvedCards = resolveCards(request);
        boolean queueAffected = deck.isActive();
        if (deck.isActive()) {
            assertCanChangeDeckDuringGame(ownerUserId);
            Deck preview = copyDeck(deck);
            preview.setName(request.name());
            replaceCards(preview, resolvedCards);
            DeckValidationResult validation = applyValidation(preview);
            ensureDeckValidForActivation(validation);
        }
        leaveQueueIfNeeded(ownerUserId, queueAffected);
        deck.setName(request.name());
        replaceCards(deck, resolvedCards);
        applyValidation(deck);
        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional
    public DeckResponseDto addCard(UUID ownerUserId, UUID deckId, DeckCardRequestDto request) {
        Deck deck = loadDeck(ownerUserId, deckId);
        Card requestedCard = resolveCardForDeck(deck, request.cardId());
        boolean queueAffected = deck.isActive();
        if (deck.isActive()) {
            assertCanChangeDeckDuringGame(ownerUserId);
            Deck preview = copyDeck(deck);
            addCardToDeck(preview, request, requestedCard);
            DeckValidationResult validation = applyValidation(preview);
            ensureDeckValidForActivation(validation);
        }
        leaveQueueIfNeeded(ownerUserId, queueAffected);
        addCardToDeck(deck, request, requestedCard);
        applyValidation(deck);
        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional
    public DeckResponseDto removeCard(UUID ownerUserId, UUID deckId, UUID cardId) {
        Deck deck = loadDeck(ownerUserId, deckId);
        boolean queueAffected = deck.isActive();
        if (deck.isActive()) {
            assertCanChangeDeckDuringGame(ownerUserId);
            Deck preview = copyDeck(deck);
            removeCardFromDeck(preview, cardId);
            DeckValidationResult validation = applyValidation(preview);
            ensureDeckValidForActivation(validation);
        }
        leaveQueueIfNeeded(ownerUserId, queueAffected);
        removeCardFromDeck(deck, cardId);
        applyValidation(deck);
        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional
    public DeckValidationResponseDto validateMyDeck(UUID ownerUserId, UUID deckId) {
        Deck deck = loadDeckMetadata(ownerUserId, deckId);
        DeckValidationResult result = applyPersistedValidation(deck);
        deckPersistenceService.save(deck);
        return new DeckValidationResponseDto(deck.getId(), result.valid(), result.errors());
    }

    @Override
    @Transactional
    public DeckActivationResponseDto activateDeck(UUID ownerUserId, UUID deckId) {
        assertCanChangeDeckDuringGame(ownerUserId);
        Deck target = loadDeckMetadata(ownerUserId, deckId);
        DeckValidationResult validation = applyPersistedValidation(target);
        ensureDeckValidForActivation(validation);
        Deck currentActive = deckPersistenceService.findActiveDeckMetadataByOwnerId(ownerUserId).orElse(null);
        if (currentActive != null && currentActive.getId().equals(target.getId())) {
            return activationResponse(target, validation);
        }
        leaveQueueIfNeeded(ownerUserId, currentActive != null);
        if (currentActive != null) {
            deactivateActiveDeck(currentActive);
        }
        target.setActive(true);
        return activationResponse(deckPersistenceService.save(target), validation);
    }

    @Override
    @Transactional
    public DeckResponseDto randomizeDeck(UUID ownerUserId, UUID deckId) {
        assertCanChangeDeckDuringGame(ownerUserId);
        Deck deck = loadDeck(ownerUserId, deckId);
        RandomDeckComposition generatedDeck = deckRandomizationService.generateRandomDeck();
        Deck currentActive = deckPersistenceService.findActiveDeckByOwnerId(ownerUserId).orElse(null);
        leaveQueueIfNeeded(ownerUserId, deck.isActive() || currentActive != null);
        if (currentActive != null && !currentActive.getId().equals(deck.getId())) {
            deactivateActiveDeck(currentActive);
        }
        deck.setName(generatedDeck.name());
        replaceCards(deck, generatedDeck);
        DeckValidationResult validation = applyValidation(deck);
        ensureDeckValidForActivation(validation);
        deck.setActive(true);
        return deckMapper.toDto(deckPersistenceService.save(deck));
    }

    @Override
    @Transactional
    public void deleteDeck(UUID ownerUserId, UUID deckId) {
        Deck target = loadDeck(ownerUserId, deckId);
        List<Deck> ownerDecks = deckPersistenceService.findAllMetadataByOwnerIdOrderByCreatedAtAsc(ownerUserId);
        if (ownerDecks.size() <= 1) {
            throw new InvalidDeckException("User must keep at least one deck");
        }

        Deck replacementActive = null;
        if (target.isActive()) {
            assertCanChangeDeckDuringGame(ownerUserId);
            replacementActive = ownerDecks.stream()
                    .filter(deck -> !deck.getId().equals(deckId))
                    .filter(Deck::isValid)
                    .findFirst()
                    .orElse(null);
            if (replacementActive == null) {
                throw new InvalidDeckException("Cannot delete the active deck without another valid deck");
            }
            leaveQueueIfNeeded(ownerUserId, true);
            target.setActive(false);
            deckPersistenceService.save(target);
            deckPersistenceService.flush();
            replacementActive.setActive(true);
            deckPersistenceService.save(replacementActive);
        }

        try {
            deckPersistenceService.delete(target);
            deckPersistenceService.flush();
        } catch (DataIntegrityViolationException exception) {
            if (replacementActive != null) {
                replacementActive.setActive(false);
                deckPersistenceService.save(replacementActive);
            }
            throw new InvalidDeckException("Deck cannot be deleted because it is in use");
        }
    }

    private void replaceCards(Deck deck, ResolvedDeckCards resolvedCards) {
        Map<UUID, Integer> quantitiesByCardId = resolvedCards.quantitiesByCardId();
        deck.getCards().removeIf(deckCard -> !quantitiesByCardId.containsKey(deckCard.getCard().getId()));

        quantitiesByCardId.forEach((cardId, quantity) -> {
            DeckCard existing = deck.getCards().stream()
                    .filter(deckCard -> deckCard.getCard().getId().equals(cardId))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                deck.addCard(deckCard(resolvedCards.cardsById().get(cardId), quantity));
            } else {
                existing.setQuantity(quantity);
            }
        });
    }

    private void replaceCards(Deck deck, RandomDeckComposition composition) {
        Map<UUID, Integer> quantitiesByCardId = new LinkedHashMap<>();
        Map<UUID, Card> cardsById = new LinkedHashMap<>();
        for (RandomDeckComposition.CardEntry entry : composition.cards()) {
            quantitiesByCardId.merge(entry.card().getId(), entry.quantity(), Integer::sum);
            cardsById.put(entry.card().getId(), entry.card());
        }
        replaceCards(deck, new ResolvedDeckCards(quantitiesByCardId, cardsById));
    }

    private void addCardToDeck(Deck deck, DeckCardRequestDto request, Card requestedCard) {
        DeckCard existing = deck.getCards().stream()
                .filter(deckCard -> deckCard.getCard().getId().equals(request.cardId()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            deck.addCard(deckCard(requestedCard, request.quantity()));
        } else {
            existing.setQuantity(existing.getQuantity() + request.quantity());
        }
    }

    private void removeCardFromDeck(Deck deck, UUID cardId) {
        boolean removed = deck.getCards().removeIf(deckCard -> deckCard.getCard().getId().equals(cardId));
        if (!removed) {
            throw new ResourceNotFoundException("Card not found in deck: " + cardId);
        }
    }

    private Deck loadDeck(UUID ownerUserId, UUID deckId) {
        return deckPersistenceService.findByIdAndOwnerIdWithCards(deckId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found for user: " + ownerUserId));
    }

    private Deck loadDeckMetadata(UUID ownerUserId, UUID deckId) {
        return deckPersistenceService.findMetadataByIdAndOwnerId(deckId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found for user: " + ownerUserId));
    }

    private void leaveQueueIfNeeded(UUID ownerUserId, boolean queueAffected) {
        if (queueAffected && matchmakingService.isQueued(ownerUserId)) {
            matchmakingService.leaveQueue(ownerUserId);
        }
    }

    private void assertCanChangeDeckDuringGame(UUID ownerUserId) {
        if (matchmakingService.hasActiveGame(ownerUserId)) {
            throw new InvalidDeckException("Cannot change deck while an active game exists");
        }
    }

    private void ensureDeckValidForActivation(DeckValidationResult validation) {
        if (!validation.valid()) {
            throw new InvalidDeckException(String.join("; ", validation.errors()));
        }
    }

    private void deactivateActiveDeck(Deck activeDeck) {
        activeDeck.setActive(false);
        deckPersistenceService.save(activeDeck);
        deckPersistenceService.flush();
    }

    private Map<UUID, Integer> cardsById(DeckUpsertRequestDto request) {
        Map<UUID, Integer> cards = new LinkedHashMap<>();
        for (DeckCardRequestDto card : request.cards()) {
            cards.merge(card.cardId(), card.quantity(), Integer::sum);
        }
        return cards;
    }

    private ResolvedDeckCards resolveCards(DeckUpsertRequestDto request) {
        Map<UUID, Integer> quantitiesByCardId = cardsById(request);
        List<Card> cards = cardService.getCardEntitiesByIds(new ArrayList<>(quantitiesByCardId.keySet()));
        Map<UUID, Card> cardsById = new LinkedHashMap<>();
        for (Card card : cards) {
            cardsById.put(card.getId(), card);
        }
        return new ResolvedDeckCards(quantitiesByCardId, cardsById);
    }

    private Card resolveCardForDeck(Deck deck, UUID cardId) {
        for (DeckCard deckCard : deck.getCards()) {
            if (deckCard.getCard().getId().equals(cardId)) {
                return deckCard.getCard();
            }
        }
        return cardService.getCardEntitiesByIds(List.of(cardId)).getFirst();
    }

    private DeckCard deckCard(Card card, int quantity) {
        if (!Card.isPlayableSetCode(card.getSetCode())) {
            throw new InvalidDeckException("Deck can only contain cards from playable sets");
        }
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private DeckValidationResult applyValidation(Deck deck) {
        DeckValidationResult result = deckValidationService.validate(deck);
        applyValidationResult(deck, result);
        return result;
    }

    private DeckValidationResult applyPersistedValidation(Deck deck) {
        DeckValidationResult result = deckValidationService.validate(
                deckPersistenceService.getValidationSummary(deck.getId()));
        applyValidationResult(deck, result);
        return result;
    }

    private void applyValidationResult(Deck deck, DeckValidationResult result) {
        deck.setValid(result.valid());
        try {
            deck.setValidationErrors(objectMapper.writeValueAsString(result.errors()));
        } catch (JsonProcessingException exception) {
            throw new InvalidDeckException("Could not serialize deck validation errors");
        }
    }

    private DeckActivationResponseDto activationResponse(Deck deck, DeckValidationResult validation) {
        return new DeckActivationResponseDto(
                deck.getId(),
                deck.isActive(),
                validation.valid(),
                validation.errors());
    }

    private Deck copyDeck(Deck source) {
        Deck copy = new Deck();
        copy.setId(source.getId());
        copy.setOwner(source.getOwner());
        copy.setName(source.getName());
        copy.setFormat(source.getFormat());
        copy.setValid(source.isValid());
        copy.setActive(source.isActive());
        copy.setValidationErrors(source.getValidationErrors());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setCards(new ArrayList<>());
        for (DeckCard existing : source.getCards()) {
            DeckCard duplicate = new DeckCard();
            duplicate.setId(existing.getId());
            duplicate.setCard(existing.getCard());
            duplicate.setQuantity(existing.getQuantity());
            copy.addCard(duplicate);
        }
        return copy;
    }

    private record ResolvedDeckCards(Map<UUID, Integer> quantitiesByCardId, Map<UUID, Card> cardsById) {
    }
}
