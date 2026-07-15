package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.controllers.matchmaking.MatchmakingController;
import ar.edu.utn.frc.tup.piii.dtos.websocket.MatchmakingQueueStatusDto;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.matchmaking.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchmakingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchmakingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchmakingService matchmakingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldJoinQueueForAuthenticatedUserWithoutDeckBody() throws Exception {
        UUID userId = UUID.randomUUID();
        when(matchmakingService.joinQueue(userId)).thenReturn(new MatchmakingQueueStatusDto(
                true,
                Instant.parse("2026-05-20T10:00:00Z"),
                1,
                null,
                null));

        mockMvc.perform(post("/api/matchmaking/queue").principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(true))
                .andExpect(jsonPath("$.queueSize").value(1));

        verify(matchmakingService).joinQueue(userId);
    }

    @Test
    void shouldLeaveQueueForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/matchmaking/queue").principal(authentication(userId)))
                .andExpect(status().isNoContent());

        verify(matchmakingService).leaveQueue(userId);
    }

    @Test
    void shouldReturnQueueStatusForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        when(matchmakingService.getMyQueueStatus(userId)).thenReturn(new MatchmakingQueueStatusDto(
                false,
                null,
                0,
                opponentUserId,
                gameId));

        mockMvc.perform(get("/api/matchmaking/queue/me").principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(false))
                .andExpect(jsonPath("$.queueSize").value(0))
                .andExpect(jsonPath("$.matchedUserId").value(opponentUserId.toString()))
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    @Test
    void shouldReturnMatchedGameDataWhenJoinQueueResolvesMatchImmediately() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        when(matchmakingService.joinQueue(userId)).thenReturn(new MatchmakingQueueStatusDto(
                false,
                null,
                0,
                opponentUserId,
                gameId));

        mockMvc.perform(post("/api/matchmaking/queue").principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queued").value(false))
                .andExpect(jsonPath("$.matchedUserId").value(opponentUserId.toString()))
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }

    private TestingAuthenticationToken authentication(UUID userId) {
        return new TestingAuthenticationToken(userId.toString(), null);
    }
}
