package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.exceptions.CardImportException;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.card.impl.CardImportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardImportServiceTest {

    @Mock
    private PokemonTcgApiService pokemonTcgApiService;

    @Mock
    private CardRepository cardRepository;

    private CardImportServiceImpl cardImportService;

    @BeforeEach
    void setUp() {
        cardImportService = new CardImportServiceImpl(
                pokemonTcgApiService,
                new Xy1CardImportValidator(),
                cardRepository,
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    @Test
    void shouldImportXy1CardsWithAttacksCostsWeaknessesAndResistances() {
        when(pokemonTcgApiService.fetchXy1Cards()).thenReturn(validPayloads());
        when(cardRepository.findByExternalId(any())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardRepository.countBySetCode(Card.XY1_SET_CODE)).thenReturn(146L);

        CardImportResultDto result = cardImportService.importXy1();

        assertThat(result.complete()).isTrue();
        assertThat(result.importedCards()).isEqualTo(146);
        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository, org.mockito.Mockito.times(146)).save(cardCaptor.capture());
        Card detailedCard = cardCaptor.getAllValues().get(0);
        assertThat(detailedCard.getExternalId()).isEqualTo("xy1-1");
        assertThat(detailedCard.getSetCode()).isEqualTo(Card.XY1_SET_CODE);
        assertThat(detailedCard.getAttacks()).singleElement().satisfies(attack -> {
            assertThat(attack.getCard()).isSameAs(detailedCard);
            assertThat(attack.getCosts()).singleElement().satisfies(cost -> {
                assertThat(cost.getAttack()).isSameAs(attack);
                assertThat(cost.getEnergyType()).isEqualTo("Lightning");
                assertThat(cost.getQuantity()).isEqualTo(2);
            });
        });
        assertThat(detailedCard.getWeaknesses()).singleElement().satisfies(weakness -> {
            assertThat(weakness.getCard()).isSameAs(detailedCard);
            assertThat(weakness.getEnergyType()).isEqualTo("Fighting");
        });
        assertThat(detailedCard.getResistances()).singleElement().satisfies(resistance -> {
            assertThat(resistance.getCard()).isSameAs(detailedCard);
            assertThat(resistance.getEnergyType()).isEqualTo("Metal");
        });
    }

    @Test
    void shouldRejectImportWhenFetchedCardsAreNotComplete() {
        when(pokemonTcgApiService.fetchXy1Cards()).thenReturn(validPayloads().subList(0, 145));

        assertThatThrownBy(() -> cardImportService.importXy1())
                .isInstanceOf(CardImportException.class)
                .hasMessageContaining("exactly 146 cards");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void shouldReturnXy1ImportStatus() {
        when(cardRepository.countBySetCode(Card.XY1_SET_CODE)).thenReturn(146L);

        CardImportStatusDto status = cardImportService.getXy1Status();

        assertThat(status.setCode()).isEqualTo(Card.XY1_SET_CODE);
        assertThat(status.importedCards()).isEqualTo(146);
        assertThat(status.expectedCards()).isEqualTo(146);
        assertThat(status.complete()).isTrue();
    }

    private List<PokemonTcgCardPayload> validPayloads() {
        List<PokemonTcgCardPayload> payloads = new ArrayList<>();
        payloads.add(detailedPayload());
        for (int index = 2; index <= 146; index++) {
            payloads.add(simplePayload(index));
        }
        return payloads;
    }

    private PokemonTcgCardPayload detailedPayload() {
        Map<String, Integer> costs = new LinkedHashMap<>();
        costs.put("Lightning", 2);
        return new PokemonTcgCardPayload(
                "xy1-1",
                Card.XY1_SET_CODE,
                "XY",
                "1",
                "Pikachu",
                CardSupertype.POKEMON,
                CardCategory.BASIC_POKEMON,
                "Basic",
                null,
                60,
                "Lightning",
                1,
                "small.png",
                "large.png",
                "{}",
                List.of(new PokemonTcgCardPayload.AttackPayload("Spark", "30", 30, "Deal damage.", 0, costs)),
                List.of(new PokemonTcgCardPayload.CardRelationPayload("Fighting", "x2")),
                List.of(new PokemonTcgCardPayload.CardRelationPayload("Metal", "-20")));
    }

    private PokemonTcgCardPayload simplePayload(int index) {
        return new PokemonTcgCardPayload(
                "xy1-" + index,
                Card.XY1_SET_CODE,
                "XY",
                String.valueOf(index),
                "Card " + index,
                CardSupertype.POKEMON,
                CardCategory.BASIC_POKEMON,
                "Basic",
                null,
                60,
                "Lightning",
                1,
                null,
                null,
                "{}",
                List.of(),
                List.of(),
                List.of());
    }
}
