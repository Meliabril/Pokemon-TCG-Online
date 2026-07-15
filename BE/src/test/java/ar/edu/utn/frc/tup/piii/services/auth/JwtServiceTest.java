package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.exceptions.TokenExpiredException;
import ar.edu.utn.frc.tup.piii.services.auth.impl.JwtServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-enough-length-for-hmac-signature";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = jwtServiceWithExpiration(900L);
        user = validUser();
    }

    @Test
    void generateAccessTokenShouldContainUserIdAsSubject() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("sub")).isEqualTo(user.getId().toString());
    }

    @Test
    void generateAccessTokenShouldContainEmail() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("email")).isEqualTo(user.getEmail());
    }

    @Test
    void generateAccessTokenShouldContainUsername() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("username")).isEqualTo(user.getUsername());
    }

    @Test
    void generateAccessTokenShouldContainRole() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("role")).isEqualTo(UserRole.ADMIN.name());
    }

    @Test
    void generateAccessTokenShouldContainStatus() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("status")).isEqualTo(UserStatus.ACTIVE.name());
    }

    @Test
    void generateAccessTokenShouldContainJti() {
        String token = jwtService.generateAccessToken(user);

        assertThat(claims(token).get("jti"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();
    }

    @Test
    void validateTokenShouldAcceptValidToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateTokenShouldRejectInvalidToken() {
        String token = jwtService.generateAccessToken(user);
        String invalidToken = token.substring(0, token.length() - 3) + "bad";

        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid token signature");
    }

    @Test
    void validateTokenShouldRejectExpiredToken() {
        JwtService expiredJwtService = jwtServiceWithExpiration(-1L);
        String token = expiredJwtService.generateAccessToken(user);

        assertThatThrownBy(() -> expiredJwtService.validateToken(token))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void extractUserIdShouldReturnSubject() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
    }

    @Test
    void extractRoleShouldReturnRoleClaim() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractRole(token)).isEqualTo(UserRole.ADMIN.name());
    }

    private Map<String, Object> claims(String token) {
        try {
            String payload = token.split("\\.")[1];
            byte[] decodedPayload = Base64.getUrlDecoder().decode(payload);
            return objectMapper.readValue(decodedPayload, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new AssertionError("Could not decode JWT payload", exception);
        }
    }

    private JwtService jwtServiceWithExpiration(Long expirationSeconds) {
        JwtServiceImpl service = new JwtServiceImpl(objectMapper);
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "expirationSeconds", expirationSeconds);
        return service;
    }

    private User validUser() {
        User validUser = new User();
        validUser.setId(UUID.randomUUID());
        validUser.setEmail("admin@gmail.com");
        validUser.setUsername("admin");
        validUser.setPasswordHash("$2a$10$hash");
        validUser.setRole(UserRole.ADMIN);
        validUser.setStatus(UserStatus.ACTIVE);
        validUser.setCreatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        validUser.setUpdatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        return validUser;
    }
}
