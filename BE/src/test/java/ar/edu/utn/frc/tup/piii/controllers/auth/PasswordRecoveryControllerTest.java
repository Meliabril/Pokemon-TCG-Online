package ar.edu.utn.frc.tup.piii.controllers.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.ForgotPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResetPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordRecoveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldReturnGenericMessageWhenForgotPasswordIsRequested() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequestDto(
                                "melina@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Si el email pertenece a una cuenta activa, se enviara un codigo de recuperacion."))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))))
                .andExpect(jsonPath("$", not(hasKey("accessToken"))))
                .andExpect(jsonPath("$", not(hasKey("refreshToken"))));

        verify(passwordRecoveryService).requestPasswordReset(any(ForgotPasswordRequestDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenForgotEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequestDto(
                                "melina"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void shouldReturnSuccessMessageWhenPasswordIsReset() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequestDto(
                                "melina@gmail.com",
                                "839214",
                                "NuevaPass123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Contrasena actualizada correctamente. Inicia sesion nuevamente."))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))))
                .andExpect(jsonPath("$", not(hasKey("accessToken"))))
                .andExpect(jsonPath("$", not(hasKey("refreshToken"))));

        verify(passwordRecoveryService).resetPassword(any(ResetPasswordRequestDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenResetCodeFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequestDto(
                                "melina@gmail.com",
                                "123",
                                "NuevaPass123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    void shouldReturnBadRequestWhenResetPasswordIsWeak() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequestDto(
                                "melina@gmail.com",
                                "839214",
                                "weak"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.newPassword").exists());
    }

    @Test
    void shouldSupportForgotPasswordAlias() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequestDto(
                                "melina@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Si el email pertenece a una cuenta activa, se enviara un codigo de recuperacion."));
    }

    @Test
    void shouldSupportResetPasswordAlias() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequestDto(
                                "melina@gmail.com",
                                "839214",
                                "NuevaPass123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Contrasena actualizada correctamente. Inicia sesion nuevamente."));
    }
}
