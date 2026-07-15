package ar.edu.utn.frc.tup.piii.services.deck.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.projections.DeckValidationSummaryProjection;
import ar.edu.utn.frc.tup.piii.services.deck.DeckPersistenceService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckPersistenceServiceImpl implements DeckPersistenceService {

    private static final int MAX_COPIES_BY_NAME = 4;

    private final DeckRepository deckRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Deck> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerUserId) {
        List<Deck> orderedDecks = deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(ownerUserId);
        if (orderedDecks.isEmpty()) {
            return orderedDecks;
        }

        List<UUID> deckIds = orderedDecks.stream()
                .map(Deck::getId)
                .toList();
        Map<UUID, Deck> decksWithCardsById = deckRepository.findAllWithCardsByIdIn(deckIds).stream()
                .collect(Collectors.toMap(Deck::getId, Function.identity()));

        return orderedDecks.stream()
                .map(deck -> decksWithCardsById.getOrDefault(deck.getId(), deck))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deck> findAllMetadataByOwnerIdOrderByCreatedAtAsc(UUID ownerUserId) {
        return deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(ownerUserId);
    }

    @Override
    public Optional<Deck> findByIdAndOwnerIdWithCards(UUID deckId, UUID ownerUserId) {
        return deckRepository.findByIdAndOwnerIdWithCards(deckId, ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Deck> findMetadataByIdAndOwnerId(UUID deckId, UUID ownerUserId) {
        return deckRepository.findMetadataByIdAndOwnerId(deckId, ownerUserId);
    }

    @Override
    public Optional<Deck> findActiveDeckByOwnerId(UUID ownerUserId) {
        return deckRepository.findByOwnerIdAndActiveTrue(ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Deck> findActiveDeckMetadataByOwnerId(UUID ownerUserId) {
        return deckRepository.findActiveMetadataByOwnerId(ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public DeckValidationSummary getValidationSummary(UUID deckId) {
        DeckValidationSummaryProjection summary = deckRepository.findValidationSummary(
                deckId,
                Card.PLAYABLE_SET_CODES);
        List<String> duplicatedNames = deckRepository.findDuplicatedCardNames(
                deckId,
                CardCategory.BASIC_ENERGY,
                MAX_COPIES_BY_NAME);

        return new DeckValidationSummary(
                summary.getTotalCards(),
                summary.getCardsOutsideSet() > 0,
                summary.getBasicPokemonCards() > 0,
                duplicatedNames);
    }

    @Override
    public long countByOwnerId(UUID ownerUserId) {
        return deckRepository.countByOwnerId(ownerUserId);
    }

    @Override
    public Deck save(Deck deck) {
        return deckRepository.save(deck);
    }

    @Override
    public void delete(Deck deck) {
        deckRepository.delete(deck);
    }

    @Override
    public void flush() {
        deckRepository.flush();
    }
}
