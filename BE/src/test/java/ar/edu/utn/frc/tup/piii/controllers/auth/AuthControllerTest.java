package ar.edu.utn.frc.tup.piii.controllers.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResendVerificationCodeRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifyAccountRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.security.RefreshTokenCookieService;
import ar.edu.utn.frc.tup.piii.services.auth.AuthService;
import ar.edu.utn.frc.tup.piii.services.auth.AuthSessionResult;
import ar.edu.utn.frc.tup.piii.services.auth.EmailVerificationService;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RefreshTokenCookieService.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldReturnOkWhenLoginIsValid() throws Exception {
        when(authService.login(any(LoginRequestDto.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(
                                "melina@gmail.com",
                                "Contrasena123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("melina@gmail.com"))
                .andExpect(jsonPath("$.user.avatar").value("avatar-pikachu-01"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
                .andExpect(jsonPath("$", not(hasKey("refreshToken"))));
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(
                                "melina@gmail.com",
                                "WrongPass123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldReturnBadRequestWhenLoginIdentifierIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("", "Contrasena123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.identifier").exists());
    }

    @Test
    void shouldNotExposePasswordInLoginResponse() throws Exception {
        when(authService.login(any(LoginRequestDto.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(
                                "melina@gmail.com",
                                "Contrasena123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user", not(hasKey("firstName"))))
                .andExpect(jsonPath("$.user", not(hasKey("lastName"))))
                .andExpect(jsonPath("$.user", not(hasKey("password"))))
                .andExpect(jsonPath("$.user", not(hasKey("passwordHash"))));
    }

    @Test
    void shouldReturnOkWhenRefreshTokenIsValid() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(new AuthSessionResult(
                "new-access-token",
                "new-refresh-token",
                "Bearer",
                900L,
                userResponse(UUID.randomUUID())));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=new-refresh-token")))
                .andExpect(jsonPath("$", not(hasKey("refreshToken"))));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshTokenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldReturnAuthResponseWhenVerificationCodeIsValid() throws Exception {
        when(emailVerificationService.verifyAccount("melina@gmail.com", "839214")).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyAccountRequestDto(
                                "melina@gmail.com",
                                "839214"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("melina@gmail.com"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
                .andExpect(jsonPath("$", not(hasKey("refreshToken"))));
    }

    @Test
    void shouldReturnBadRequestWhenVerificationCodeFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/verify-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyAccountRequestDto(
                                "melina@gmail.com",
                                "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.code").exists());
    }

    @Test
    void shouldReturnMessageWhenVerificationCodeIsResent() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationCodeRequestDto(
                                "melina@gmail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Código reenviado correctamente."));

        verify(emailVerificationService).resendVerificationCode("melina@gmail.com");
    }

    @Test
    void shouldReturnBadRequestWhenResendEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationCodeRequestDto(
                                "melina"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void shouldReturnLogoutSuccessful() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/auth/logout")
                        .principal(new UsernamePasswordAuthenticationToken(userId.toString(), null))
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(authService).logout("refresh-token");
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.getCurrentUser(userId)).thenReturn(userResponse(userId));

        mockMvc.perform(get("/api/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken(userId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("melina@gmail.com"));
    }

    private AuthSessionResult authResponse() {
        return new AuthSessionResult(
                "access-token",
                "refresh-token",
                "Bearer",
                900L,
                userResponse(UUID.randomUUID()));
    }

    private UserResponseDto userResponse(UUID userId) {
        return new UserResponseDto(
                userId,
                "melina@gmail.com",
                "meli123",
                "avatar-pikachu-01",
                UserRole.USER,
                UserStatus.ACTIVE,
                true,
                null,
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T13:00:00Z"));
    }
}
