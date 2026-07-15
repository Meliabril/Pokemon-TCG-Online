package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.controllers.matchhistory.MatchHistoryController;
import ar.edu.utn.frc.tup.piii.dtos.common.PageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchHistoryFilter;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchResult;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.PlayerStatsDto;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.matchhistory.MatchHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchHistoryService matchHistoryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldGetAuthenticatedPlayerStats() throws Exception {
        UUID userId = UUID.randomUUID();
        when(matchHistoryService.getPlayerStats(userId))
                .thenReturn(new PlayerStatsDto(4, 3, 1, 75.0, 2));

        mockMvc.perform(get("/api/matches/stats").principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMatches").value(4))
                .andExpect(jsonPath("$.wins").value(3))
                .andExpect(jsonPath("$.losses").value(1))
                .andExpect(jsonPath("$.winRate").value(75.0))
                .andExpect(jsonPath("$.currentStreak").value(2));
    }

    @Test
    void shouldGetAuthenticatedPlayerHistoryWithFilterAndPageable() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        Instant finishedAt = Instant.parse("2026-06-01T10:15:30Z");
        MatchHistoryDto match = new MatchHistoryDto(matchId, MatchResult.VICTORIA, "Misty", finishedAt, 9);
        PageResponseDto<MatchHistoryDto> response =
                new PageResponseDto<>(List.of(match), 1, 5, 11, 3, false, false);
        when(matchHistoryService.getMatchHistory(
                eq(userId),
                eq(MatchHistoryFilter.WINS),
                org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/matches/history")
                        .param("filter", "WINS")
                        .param("page", "1")
                        .param("size", "5")
                        .principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].matchId").value(matchId.toString()))
                .andExpect(jsonPath("$.items[0].result").value("VICTORIA"))
                .andExpect(jsonPath("$.items[0].opponentName").value("Misty"))
                .andExpect(jsonPath("$.items[0].turnsPlayed").value(9))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(matchHistoryService).getMatchHistory(
                eq(userId),
                eq(MatchHistoryFilter.WINS),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    private TestingAuthenticationToken authentication(UUID userId) {
        return new TestingAuthenticationToken(userId.toString(), null);
    }
}
