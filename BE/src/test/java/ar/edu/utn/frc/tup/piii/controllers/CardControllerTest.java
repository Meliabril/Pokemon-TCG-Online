package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.controllers.card.CardController;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportStatusDto;
import ar.edu.utn.frc.tup.piii.dtos.card.CardResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldListCards() throws Exception {
        when(cardService.getCards(null)).thenReturn(List.of(cardResponse()));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value(Card.XY1_SET_CODE))
                .andExpect(jsonPath("$[0].name").value("Pikachu"));
    }

    @Test
    void shouldListCustomProfessorCardsBySetCode() throws Exception {
        when(cardService.getCards(Card.CUSTOM_PROFESSORS_SET_CODE))
                .thenReturn(List.of(cardResponse(Card.CUSTOM_PROFESSORS_SET_CODE, "AngularQuin")));

        mockMvc.perform(get("/api/cards").param("setCode", Card.CUSTOM_PROFESSORS_SET_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value(Card.CUSTOM_PROFESSORS_SET_CODE))
                .andExpect(jsonPath("$[0].name").value("AngularQuin"));
    }

    @Test
    void shouldListXy1CardsBySetCode() throws Exception {
        when(cardService.getCards(Card.XY1_SET_CODE)).thenReturn(List.of(cardResponse()));

        mockMvc.perform(get("/api/cards").param("setCode", Card.XY1_SET_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value(Card.XY1_SET_CODE))
                .andExpect(jsonPath("$[0].name").value("Pikachu"));
    }

    @Test
    void shouldSearchCardsByName() throws Exception {
        when(cardService.searchCards(null, "Pikachu")).thenReturn(List.of(cardResponse()));

        mockMvc.perform(get("/api/cards/search").param("name", "Pikachu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pikachu"));

        verify(cardService).searchCards(null, "Pikachu");
    }

    @Test
    void shouldSearchXy1CardsByName() throws Exception {
        when(cardService.searchCards(Card.XY1_SET_CODE, "Pikachu")).thenReturn(List.of(cardResponse()));

        mockMvc.perform(get("/api/cards/search")
                        .param("setCode", Card.XY1_SET_CODE)
                        .param("name", "Pikachu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value(Card.XY1_SET_CODE))
                .andExpect(jsonPath("$[0].name").value("Pikachu"));

        verify(cardService).searchCards(Card.XY1_SET_CODE, "Pikachu");
    }

    @Test
    void shouldSearchCustomProfessorCardsByName() throws Exception {
        when(cardService.searchCards(Card.CUSTOM_PROFESSORS_SET_CODE, "AngularQuin"))
                .thenReturn(List.of(cardResponse(Card.CUSTOM_PROFESSORS_SET_CODE, "AngularQuin")));

        mockMvc.perform(get("/api/cards/search")
                        .param("setCode", Card.CUSTOM_PROFESSORS_SET_CODE)
                        .param("name", "AngularQuin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value(Card.CUSTOM_PROFESSORS_SET_CODE))
                .andExpect(jsonPath("$[0].name").value("AngularQuin"));

        verify(cardService).searchCards(Card.CUSTOM_PROFESSORS_SET_CODE, "AngularQuin");
    }

    @Test
    void shouldGetCardById() throws Exception {
        CardResponseDto response = cardResponse();
        when(cardService.getCard(response.id())).thenReturn(response);

        mockMvc.perform(get("/api/cards/{id}", response.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()));
    }

    @Test
    void shouldReturnImportStatus() throws Exception {
        when(cardService.getXy1ImportStatus()).thenReturn(new CardImportStatusDto(Card.XY1_SET_CODE, 146, 146, true));

        mockMvc.perform(get("/api/cards/import-status/xy1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCards").value(146))
                .andExpect(jsonPath("$.complete").value(true));
    }

    private CardResponseDto cardResponse() {
        return cardResponse(Card.XY1_SET_CODE, "Pikachu");
    }

    private CardResponseDto cardResponse(String setCode, String name) {
        return new CardResponseDto(
                UUID.randomUUID(),
                setCode + "-42",
                setCode,
                "XY",
                "42",
                name,
                CardSupertype.POKEMON,
                CardCategory.BASIC_POKEMON,
                "Basic",
                null,
                60,
                "Lightning",
                1,
                "small.png",
                "large.png",
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null);
    }
}
