package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.controllers.deck.DeckController;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckActivationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResponseDto;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidDeckException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.deck.DeckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeckController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeckService deckService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldListAuthenticatedUserDecks() throws Exception {
        UUID userId = UUID.randomUUID();
        when(deckService.getMyDecks(userId)).thenReturn(List.of(deckResponse(userId, true)));

        mockMvc.perform(get("/api/me/decks").principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerUserId").value(userId.toString()))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldCreateDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        when(deckService.createDeck(eq(userId), any())).thenReturn(deckResponse(userId, true));

        mockMvc.perform(post("/api/me/decks")
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mazo XY1",
                                  "cards": [
                                    {"cardId": "%s", "quantity": 4}
                                  ]
                                }
                                """.formatted(cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mazo XY1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldActivateDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        when(deckService.activateDeck(userId, deckId)).thenReturn(activationResponse(deckId));

        mockMvc.perform(put("/api/me/decks/{deckId}/activate", deckId).principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(deckId.toString()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.cards").doesNotExist());
    }

    @Test
    void shouldExplainWhyDeckCannotBeActivated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        String validationMessage =
                "Deck must contain exactly 60 cards; Deck must contain at least 1 Basic Pokemon";
        when(deckService.activateDeck(userId, deckId)).thenThrow(new InvalidDeckException(validationMessage));

        mockMvc.perform(put("/api/me/decks/{deckId}/activate", deckId).principal(authentication(userId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("INVALID_DECK"))
                .andExpect(jsonPath("$.message").value(validationMessage))
                .andExpect(jsonPath("$.validationErrors").isEmpty());
    }

    @Test
    void shouldRandomizeDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        when(deckService.randomizeDeck(userId, deckId)).thenReturn(deckResponse(userId, true));

        mockMvc.perform(put("/api/me/decks/{deckId}/randomize", deckId).principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldAddCardToSpecificDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        when(deckService.addCard(eq(userId), eq(deckId), any(DeckCardRequestDto.class))).thenReturn(deckResponse(userId, false));

        mockMvc.perform(post("/api/me/decks/{deckId}/cards", deckId)
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeckCardRequestDto(cardId, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUserId").value(userId.toString()));
    }

    @Test
    void shouldDeleteDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        doNothing().when(deckService).deleteDeck(userId, deckId);

        mockMvc.perform(delete("/api/me/decks/{deckId}", deckId).principal(authentication(userId)))
                .andExpect(status().isOk());

        verify(deckService).deleteDeck(userId, deckId);
    }

    @Test
    void shouldValidateSpecificDeck() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        when(deckService.validateMyDeck(userId, deckId)).thenReturn(new DeckValidationResponseDto(deckId, true, List.of()));

        mockMvc.perform(get("/api/me/decks/{deckId}/validation", deckId).principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value(deckId.toString()))
                .andExpect(jsonPath("$.valid").value(true));
    }

    private TestingAuthenticationToken authentication(UUID userId) {
        return new TestingAuthenticationToken(userId.toString(), null);
    }

    private DeckResponseDto deckResponse(UUID userId, boolean active) {
        return new DeckResponseDto(
                UUID.randomUUID(),
                userId,
                "Mazo XY1",
                Deck.XY1_UNLIMITED_FORMAT,
                active,
                true,
                List.of(),
                List.of(),
                Instant.parse("2026-05-18T12:00:00Z"),
                Instant.parse("2026-05-18T12:00:00Z"));
    }

    private DeckActivationResponseDto activationResponse(UUID deckId) {
        return new DeckActivationResponseDto(deckId, true, true, List.of());
    }
}
