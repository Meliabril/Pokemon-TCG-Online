package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequestDto;
import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.EmailNotVerifiedException;
import ar.edu.utn.frc.tup.piii.exceptions.InactiveUserException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.impl.AuthServiceImpl;
import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordServiceImpl;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import ar.edu.utn.frc.tup.piii.services.user.impl.UserServiceImpl;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthServiceImpl authService;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
        UserService userService = new UserServiceImpl(userRepository, passwordService, emailVerificationService, new UserMapper(), validator);
        authService = new AuthServiceImpl(
                userService,
                passwordService,
                jwtService,
                refreshTokenService,
                new UserMapper(),
                validator);
    }

    @Test
    void shouldLoginWithEmail() {
        User user = existingUser(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("melina@gmail.com")).thenReturn(Optional.of(user));
        mockTokenGeneration(user);

        AuthSessionResult response = authService.login(new LoginRequestDto("melina@gmail.com", "Contrasena123!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("melina@gmail.com");
        assertThat(response.user().avatar()).isEqualTo("avatar-pikachu-01");
    }

    @Test
    void shouldLoginWithUsername() {
        User user = existingUser(UserStatus.ACTIVE);
        when(userRepository.findByUsernameIgnoreCase("meli123")).thenReturn(Optional.of(user));
        mockTokenGeneration(user);

        AuthSessionResult response = authService.login(new LoginRequestDto("meli123", "Contrasena123!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().username()).isEqualTo("meli123");
    }

    @Test
    void shouldRejectUnknownEmailWithoutGeneratingTokens() {
        when(userRepository.findByEmailIgnoreCase("noexiste@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("noexiste@gmail.com", "Contrasena123!")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldRejectUnknownUsernameWithoutGeneratingTokens() {
        when(userRepository.findByUsernameIgnoreCase("usuarioNoExiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("usuarioNoExiste", "Contrasena123!")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldRejectInvalidLoginRequest() {
        assertThatThrownBy(() -> authService.login(new LoginRequestDto("", "")))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Email or username is required")
                .hasMessageContaining("Password is required");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldRejectWrongPasswordWithoutGeneratingTokens() {
        User user = existingUser(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("melina@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("melina@gmail.com", "WrongPass123!")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldRejectBlockedUser() {
        User user = existingUser(UserStatus.BLOCKED);
        when(userRepository.findByEmailIgnoreCase("melina@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("melina@gmail.com", "Contrasena123!")))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("Blocked users cannot login");
    }

    @Test
    void shouldRejectDeletedUser() {
        User user = existingUser(UserStatus.DELETED);
        when(userRepository.findByUsernameIgnoreCase("meli123")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("meli123", "Contrasena123!")))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("Deleted users cannot login");
    }

    @Test
    void shouldRejectUnverifiedUser() {
        User user = existingUser(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        when(userRepository.findByEmailIgnoreCase("melina@gmail.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("melina@gmail.com", "Contrasena123!")))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("La cuenta todavia no fue verificada");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void shouldNotExposePasswordHashInLoginResponse() {
        User user = existingUser(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("melina@gmail.com")).thenReturn(Optional.of(user));
        mockTokenGeneration(user);

        AuthSessionResult response = authService.login(new LoginRequestDto("melina@gmail.com", "Contrasena123!"));

        assertThat(response.user()).hasNoNullFieldsOrProperties();
        assertThat(response.user().getClass().getDeclaredFields())
                .extracting("name")
                .doesNotContain("firstName", "lastName", "password", "passwordHash");
    }

    @Test
    void refreshShouldRotateTokenAndReturnNewTokens() {
        User user = existingUser(UserStatus.ACTIVE);
        RefreshToken rotatedRefreshToken = refreshToken(user);
        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("new-refresh-token", rotatedRefreshToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthSessionResult response = authService.refresh("refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    void refreshShouldRejectBlockedUser() {
        User user = existingUser(UserStatus.BLOCKED);
        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("new-refresh-token", refreshToken(user)));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("Blocked users cannot login");
    }

    @Test
    void refreshShouldRejectDeletedUser() {
        User user = existingUser(UserStatus.DELETED);
        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("new-refresh-token", refreshToken(user)));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("Deleted users cannot login");
    }

    @Test
    void logoutShouldRevokeRefreshToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }

    @Test
    void logoutShouldIgnoreMissingRefreshToken() {
        authService.logout("");
        verify(refreshTokenService, never()).revokeRefreshToken(any());
    }

    @Test
    void getCurrentUserShouldReturnUser() {
        User user = existingUser(UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThat(authService.getCurrentUser(user.getId()))
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(user.getId());
                    assertThat(response.email()).isEqualTo("melina@gmail.com");
                    assertThat(response.username()).isEqualTo("meli123");
                });
    }

    @Test
    void getCurrentUserShouldRejectUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);
    }

    @Test
    void getCurrentUserShouldRejectBlockedUser() {
        User user = existingUser(UserStatus.BLOCKED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.getCurrentUser(user.getId()))
                .isInstanceOf(InactiveUserException.class)
                .hasMessageContaining("Blocked users cannot login");
    }

    private void mockTokenGeneration(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash("hashed-refresh-token");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));

        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshTokenService.RefreshTokenResult("refresh-token", refreshToken));
    }

    private RefreshToken refreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash("hashed-refresh-token");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        return refreshToken;
    }

    private User existingUser(UserStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("melina@gmail.com");
        user.setUsername("meli123");
        user.setAvatar("avatar-pikachu-01");
        user.setPasswordHash(passwordService.hash("Contrasena123!"));
        user.setRole(UserRole.USER);
        user.setStatus(status);
        user.setEmailVerified(status == UserStatus.ACTIVE);
        user.setCreatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        return user;
    }
}
