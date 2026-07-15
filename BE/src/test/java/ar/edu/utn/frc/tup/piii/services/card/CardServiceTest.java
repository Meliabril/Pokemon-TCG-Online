package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.edu.utn.frc.tup.piii.services.card.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardImportService cardImportService;

    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
                cardRepository,
                cardImportService,
                new CardMapper(CardTranslationService.empty(), mock(AbilityCatalogService.class), new ObjectMapper()));
    }

    @Test
    void shouldListOnlyXy1Cards() {
        Card card = card("Pikachu");
        when(cardRepository.findBySetCodeOrderByNumberAsc(Card.XY1_SET_CODE)).thenReturn(List.of(card));

        List<CardResponseDto> response = cardService.getCards(Card.XY1_SET_CODE);

        assertThat(response).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(card.getId());
            assertThat(dto.setCode()).isEqualTo(Card.XY1_SET_CODE);
        });
    }

    @Test
    void shouldListOnlyCustomProfessorCards() {
        Card card = card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE);
        when(cardRepository.findBySetCodeOrderByNumberAsc(Card.CUSTOM_PROFESSORS_SET_CODE)).thenReturn(List.of(card));

        List<CardResponseDto> response = cardService.getCards(Card.CUSTOM_PROFESSORS_SET_CODE);

        assertThat(response).singleElement().satisfies(dto -> {
            assertThat(dto.name()).isEqualTo("AngularQuin");
            assertThat(dto.setCode()).isEqualTo(Card.CUSTOM_PROFESSORS_SET_CODE);
        });
    }

    @Test
    void shouldListPlayableCatalogWhenSetCodeIsMissing() {
        Card xy1Card = card("Pikachu");
        Card customCard = card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE);
        when(cardRepository.findBySetCodeInOrderBySetCodeAscNumberAsc(Card.PLAYABLE_SET_CODES))
                .thenReturn(List.of(customCard, xy1Card));

        List<CardResponseDto> response = cardService.getCards(null);

        assertThat(response).extracting(CardResponseDto::setCode)
                .containsExactly(Card.CUSTOM_PROFESSORS_SET_CODE, Card.XY1_SET_CODE);
    }

    @Test
    void shouldRejectListingOtherSets() {
        assertThatThrownBy(() -> cardService.getCards("base1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Only playable setCodes xy1 and custom-professors are available");
    }

    @Test
    void shouldSearchXy1CardsByName() {
        Card card = card("Pikachu");
        when(cardRepository.findBySetCodeAndNameContainingIgnoreCaseOrderByNameAsc(Card.XY1_SET_CODE, "pika"))
                .thenReturn(List.of(card));

        List<CardResponseDto> response = cardService.searchCards(Card.XY1_SET_CODE, "pika");

        assertThat(response).singleElement().extracting(CardResponseDto::name).isEqualTo("Pikachu");
    }

    @Test
    void shouldSearchPlayableCatalogByNameWhenSetCodeIsMissing() {
        Card card = card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE);
        when(cardRepository.findBySetCodeInAndNameContainingIgnoreCaseOrderByNameAsc(Card.PLAYABLE_SET_CODES, "ang"))
                .thenReturn(List.of(card));

        List<CardResponseDto> response = cardService.searchCards(null, "ang");

        assertThat(response).singleElement().satisfies(dto -> {
            assertThat(dto.name()).isEqualTo("AngularQuin");
            assertThat(dto.setCode()).isEqualTo(Card.CUSTOM_PROFESSORS_SET_CODE);
        });
    }

    @Test
    void shouldSearchCustomProfessorCardsByName() {
        Card card = card("AngularQuin", Card.CUSTOM_PROFESSORS_SET_CODE);
        when(cardRepository.findBySetCodeAndNameContainingIgnoreCaseOrderByNameAsc(
                Card.CUSTOM_PROFESSORS_SET_CODE,
                "AngularQuin"))
                .thenReturn(List.of(card));

        List<CardResponseDto> response = cardService.searchCards(Card.CUSTOM_PROFESSORS_SET_CODE, "AngularQuin");

        assertThat(response).singleElement().satisfies(dto -> {
            assertThat(dto.name()).isEqualTo("AngularQuin");
            assertThat(dto.setCode()).isEqualTo(Card.CUSTOM_PROFESSORS_SET_CODE);
        });
    }

    @Test
    void shouldRejectSearchingOtherSets() {
        assertThatThrownBy(() -> cardService.searchCards("base1", "Pikachu"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Only playable setCodes xy1 and custom-professors are available");
    }

    @Test
    void shouldGetCardByIdWithDetails() {
        Card card = card("Pikachu");
        when(cardRepository.findWithDetailsById(card.getId())).thenReturn(Optional.of(card));

        CardResponseDto response = cardService.getCard(card.getId());

        assertThat(response.id()).isEqualTo(card.getId());
        assertThat(response.name()).isEqualTo("Pikachu");
    }

    @Test
    void shouldGetCardEntityByIdWithDetails() {
        Card card = card("Pikachu");
        when(cardRepository.findWithDetailsById(card.getId())).thenReturn(Optional.of(card));

        Card response = cardService.getCardEntityById(card.getId());

        assertThat(response).isSameAs(card);
    }

    @Test
    void shouldLoadCardEntitiesInBulkAndPreserveRequestedOrder() {
        Card first = card("Pikachu");
        Card second = card("Raichu");
        when(cardRepository.findAllById(List.of(second.getId(), first.getId())))
                .thenReturn(List.of(first, second));

        List<Card> response = cardService.getCardEntitiesByIds(List.of(second.getId(), first.getId()));

        assertThat(response).containsExactly(second, first);
        verify(cardRepository).findAllById(List.of(second.getId(), first.getId()));
    }

    @Test
    void shouldThrowNotFoundWhenBulkCardDoesNotExist() {
        Card card = card("Pikachu");
        UUID missingCardId = UUID.randomUUID();
        when(cardRepository.findAllById(List.of(card.getId(), missingCardId))).thenReturn(List.of(card));

        assertThatThrownBy(() -> cardService.getCardEntitiesByIds(List.of(card.getId(), missingCardId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missingCardId.toString());
    }

    @Test
    void shouldLoadDeckBuildingCardsWithoutDetailedGraphQuery() {
        Card card = card("Pikachu");
        when(cardRepository.findDeckBuildingCardsBySetCodeOrderByNumberAsc(Card.XY1_SET_CODE))
                .thenReturn(List.of(card));

        List<Card> response = cardService.getCardEntities(Card.XY1_SET_CODE);

        assertThat(response).containsExactly(card);
        verify(cardRepository).findDeckBuildingCardsBySetCodeOrderByNumberAsc(Card.XY1_SET_CODE);
    }

    @Test
    void shouldThrowNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findWithDetailsById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCard(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(cardId.toString());
    }

    @Test
    void shouldThrowNotFoundWhenCardEntityDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findWithDetailsById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardEntityById(cardId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(cardId.toString());
    }

    @Test
    void shouldDelegateXy1ImportStatus() {
        when(cardImportService.getXy1Status()).thenReturn(new CardImportStatusDto(Card.XY1_SET_CODE, 146, 146, true));

        CardImportStatusDto status = cardService.getXy1ImportStatus();

        assertThat(status.complete()).isTrue();
        verify(cardImportService).getXy1Status();
    }

    private Card card(String name) {
        return card(name, Card.XY1_SET_CODE);
    }

    private Card card(String name, String setCode) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(setCode + "-1");
        card.setSetCode(setCode);
        card.setSetName(setCode);
        card.setNumber("1");
        card.setName(name);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setRawJson("{}");
        return card;
    }
}
