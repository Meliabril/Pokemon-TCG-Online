package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.controllers.card.CardController;
import ar.edu.utn.frc.tup.piii.controllers.auth.PasswordRecoveryController;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordRecoveryService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest({CardController.class, PasswordRecoveryController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldAllowCorsPreflightFromAngularLocalhost() throws Exception {
        mockMvc.perform(options("/api/cards")
                        .param("setCode", "xy1")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void shouldAllowCorsPreflightFromLanAddress() throws Exception {
        mockMvc.perform(options("/api/cards")
                        .param("setCode", "xy1")
                        .header(HttpHeaders.ORIGIN, "http://192.168.1.25:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://192.168.1.25:4200"));
    }

    @Test
    void shouldAllowSwaggerUiEntryPointWithoutJwt() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void shouldRequireAuthenticationForCardsRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/cards").param("setCode", "xy1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowForgotPasswordWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowResetPasswordWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com","code":"839214","newPassword":"NuevaPass123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPasswordCodeVerificationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/password/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com","code":"839214"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowVerifiedPasswordResetWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset/verified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationToken":"dce52d77-b9a5-4937-aa87-93ac79d10809","newPassword":"NuevaPass123!","confirmPassword":"NuevaPass123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowForgotPasswordAliasWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowResetPasswordAliasWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com","code":"839214","newPassword":"NuevaPass123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPasswordCodeVerificationAliasWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/verify-password-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"melina@gmail.com","code":"839214"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowVerifiedPasswordResetAliasWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password/verified")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"verificationToken":"dce52d77-b9a5-4937-aa87-93ac79d10809","newPassword":"NuevaPass123!","confirmPassword":"NuevaPass123!"}
                                """))
                .andExpect(status().isOk());
    }
}
