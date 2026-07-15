package ar.edu.utn.frc.tup.piii.services.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.configs.Xy1CardSeeder;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.services.deck.DeckValidationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:custom_professors_json_h2;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
class CustomProfessorCardsJsonH2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private DeckValidationService deckValidationService;

    @MockBean
    private Xy1CardSeeder xy1CardSeeder;

    @Test
    void shouldExposeJsonSeededCustomProfessorCardsThroughCatalogEndpoints() throws Exception {
        Card energy = saveXy1EnergyCard();

        List<Map<String, Object>> catalog = getCards("/api/cards");
        assertThat(catalog).extracting(card -> card.get("setCode"))
                .contains(Card.XY1_SET_CODE, Card.CUSTOM_PROFESSORS_SET_CODE);
        assertThat(catalog).extracting(card -> card.get("name"))
                .contains("Water Energy", "AngularQuin");

        List<Map<String, Object>> customCards = getCards("/api/cards?setCode=custom-professors");
        assertThat(customCards).hasSize(5);
        assertThat(customCards).extracting(card -> card.get("name")).containsExactly(
                "AngularQuin",
                "RaMaven",
                "BelgraNode",
                "SantorMento",
                "HernullPointer");

        assertThat(customCards)
                .extracting(card -> card.get("imageSmallUrl"))
                .allSatisfy(url -> assertThat((String) url).endsWith("INGLES.png"));
        assertThat(customCards)
                .flatExtracting(this::attacks)
                .hasSize(10)
                .allSatisfy(attack -> {
                    assertThat(attack.get("effectText")).isNull();
                    assertThat((String) attack.get("displayText")).isNotBlank();
                    assertThat((String) attack.get("displayTextEn")).isNotBlank();
                });
        assertThat(attacks(cardByName(customCards, "AngularQuin")))
                .first()
                .satisfies(attack -> {
                    assertThat(attack.get("displayText"))
                            .isEqualTo("AngularQuin descarga una explicación veloz potenciada por apuntes, diapos y NotebookLM.");
                    assertThat(attack.get("displayTextEn"))
                            .isEqualTo("AngularQuín unleashes a swift explanation powered by notes, slides, and NotebookLM.");
                });
        List<Map<String, Object>> searchResults = getCards("/api/cards/search?name=AngularQuin");
        assertThat(searchResults).singleElement().satisfies(card -> {
            assertThat(card.get("setCode")).isEqualTo(Card.CUSTOM_PROFESSORS_SET_CODE);
            assertThat(card.get("name")).isEqualTo("AngularQuin");
        });

        Card angularQuin = cardRepository.findByExternalId("custom-professors-pr-001").orElseThrow();
        DeckValidationResult validationResult = deckValidationService.validate(deckWithCustomProfessor(angularQuin, energy));

        assertThat(validationResult.valid()).isTrue();
        assertThat(validationResult.errors()).isEmpty();
    }

    private Map<String, Object> cardByName(List<Map<String, Object>> cards, String name) {
        return cards.stream()
                .filter(card -> name.equals(card.get("name")))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> attacks(Map<String, Object> card) {
        return (List<Map<String, Object>>) card.get("attacks");
    }

    private List<Map<String, Object>> getCards(String url) throws Exception {
        String response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(response, new TypeReference<>() {
        });
    }

    private Card saveXy1EnergyCard() {
        return cardRepository.findByExternalId("xy1-local-water-energy").orElseGet(() -> {
            Card card = new Card();
            card.setExternalId("xy1-local-water-energy");
            card.setSetCode(Card.XY1_SET_CODE);
            card.setSetName("XY");
            card.setSource(Card.SOURCE_POKEMONTCG_IO);
            card.setNumber("999");
            card.setName("Water Energy");
            card.setSupertype(CardSupertype.ENERGY);
            card.setCategory(CardCategory.BASIC_ENERGY);
            card.setSubtype("Basic");
            card.setRawJson("{}");
            return cardRepository.save(card);
        });
    }

    private Deck deckWithCustomProfessor(Card customProfessorCard, Card energyCard) {
        Deck deck = new Deck();
        deck.setId(UUID.randomUUID());
        deck.addCard(deckCard(customProfessorCard, 4));
        deck.addCard(deckCard(energyCard, 56));
        return deck;
    }

    private DeckCard deckCard(Card card, int quantity) {
        DeckCard deckCard = new DeckCard();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }
}
