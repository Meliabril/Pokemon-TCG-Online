package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.controllers.card.AdminCardController;
import ar.edu.utn.frc.tup.piii.dtos.card.CardImportResultDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.card.CardImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardImportService cardImportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldImportXy1Cards() throws Exception {
        when(cardImportService.importXy1())
                .thenReturn(new CardImportResultDto(Card.XY1_SET_CODE, 146, true, "XY1 import completed"));

        mockMvc.perform(post("/api/admin/cards/import/xy1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setCode").value(Card.XY1_SET_CODE))
                .andExpect(jsonPath("$.importedCards").value(146))
                .andExpect(jsonPath("$.complete").value(true));
    }
}
