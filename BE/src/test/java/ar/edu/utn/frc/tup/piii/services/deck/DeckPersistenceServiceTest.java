package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.projections.DeckValidationSummaryProjection;
import ar.edu.utn.frc.tup.piii.services.deck.impl.DeckPersistenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckPersistenceServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckValidationSummaryProjection validationSummaryProjection;

    private DeckPersistenceServiceImpl deckPersistenceService;

    @BeforeEach
    void setUp() {
        deckPersistenceService = new DeckPersistenceServiceImpl(deckRepository);
    }

    @Test
    void shouldSkipCollectionQueryWhenOwnerHasNoDecks() {
        UUID ownerId = UUID.randomUUID();
        when(deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(ownerId)).thenReturn(List.of());

        List<Deck> result = deckPersistenceService.findAllByOwnerIdOrderByCreatedAtAsc(ownerId);

        assertThat(result).isEmpty();
        verify(deckRepository, never()).findAllWithCardsByIdIn(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldLoadCollectionsInBulkAndPreserveOriginalOrder() {
        UUID ownerId = UUID.randomUUID();
        Deck first = deck();
        Deck second = deck();
        Deck firstWithCards = deck(first.getId());
        Deck secondWithCards = deck(second.getId());
        when(deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(ownerId)).thenReturn(List.of(first, second));
        when(deckRepository.findAllWithCardsByIdIn(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(secondWithCards, firstWithCards));

        List<Deck> result = deckPersistenceService.findAllByOwnerIdOrderByCreatedAtAsc(ownerId);

        assertThat(result).containsExactly(firstWithCards, secondWithCards);
    }

    @Test
    void shouldLoadOnlyMetadataWithoutCollectionQuery() {
        UUID ownerId = UUID.randomUUID();
        Deck deck = deck();
        when(deckRepository.findAllByOwnerIdOrderByCreatedAtAsc(ownerId)).thenReturn(List.of(deck));

        List<Deck> result = deckPersistenceService.findAllMetadataByOwnerIdOrderByCreatedAtAsc(ownerId);

        assertThat(result).containsExactly(deck);
        verify(deckRepository, never()).findAllWithCardsByIdIn(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldBuildValidationSummaryWithTwoAggregateQueries() {
        UUID deckId = UUID.randomUUID();
        when(deckRepository.findValidationSummary(deckId, Card.PLAYABLE_SET_CODES))
                .thenReturn(validationSummaryProjection);
        when(validationSummaryProjection.getTotalCards()).thenReturn(60L);
        when(validationSummaryProjection.getCardsOutsideSet()).thenReturn(0L);
        when(validationSummaryProjection.getBasicPokemonCards()).thenReturn(1L);
        when(deckRepository.findDuplicatedCardNames(deckId, CardCategory.BASIC_ENERGY, 4))
                .thenReturn(List.of("Pikachu"));

        DeckValidationSummary summary = deckPersistenceService.getValidationSummary(deckId);

        assertThat(summary.totalCards()).isEqualTo(60);
        assertThat(summary.containsCardsOutsideSet()).isFalse();
        assertThat(summary.containsBasicPokemon()).isTrue();
        assertThat(summary.duplicatedNames()).containsExactly("Pikachu");
        verify(deckRepository).findValidationSummary(deckId, Card.PLAYABLE_SET_CODES);
        verify(deckRepository).findDuplicatedCardNames(deckId, CardCategory.BASIC_ENERGY, 4);
    }

    private Deck deck() {
        return deck(UUID.randomUUID());
    }

    private Deck deck(UUID id) {
        Deck deck = new Deck();
        deck.setId(id);
        return deck;
    }
}
