package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.exceptions.RefreshTokenRevokedException;
import ar.edu.utn.frc.tup.piii.exceptions.TokenExpiredException;
import ar.edu.utn.frc.tup.piii.repositories.RefreshTokenRepository;
import ar.edu.utn.frc.tup.piii.services.auth.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(refreshTokenRepository);
        ReflectionTestUtils.setField(service, "expirationSeconds", 604800L);
        refreshTokenService = service;
        user = validUser();
    }

    @Test
    void createRefreshTokenShouldReturnRawTokenAndPersistHash() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> savedToken(invocation.getArgument(0)));

        RefreshTokenService.RefreshTokenResult result = refreshTokenService.createRefreshToken(user);

        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.refreshToken().getTokenHash()).isNotEqualTo(result.rawToken());
        assertThat(result.refreshToken().getTokenHash()).isEqualTo(hash(result.rawToken()));
        assertThat(result.refreshToken().getUser()).isEqualTo(user);
        assertThat(result.refreshToken().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void validateRefreshTokenShouldReturnTokenWhenValid() {
        RefreshToken token = validRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hash("refresh-token"))).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validateRefreshToken("refresh-token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    void validateRefreshTokenShouldRejectUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(hash("unknown-token"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("unknown-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void validateRefreshTokenShouldRejectExpiredToken() {
        RefreshToken token = validRefreshToken("refresh-token");
        token.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(hash("refresh-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("refresh-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Refresh token expired");
    }

    @Test
    void validateRefreshTokenShouldRejectRevokedToken() {
        RefreshToken token = validRefreshToken("refresh-token");
        token.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByTokenHash(hash("refresh-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateRefreshToken("refresh-token"))
                .isInstanceOf(RefreshTokenRevokedException.class)
                .hasMessageContaining("Refresh token was revoked");
    }

    @Test
    void rotateRefreshTokenShouldRevokePreviousTokenAndCreateReplacement() {
        RefreshToken currentToken = validRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hash("refresh-token"))).thenReturn(Optional.of(currentToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> savedToken(invocation.getArgument(0)));

        RefreshTokenService.RefreshTokenResult result = refreshTokenService.rotateRefreshToken("refresh-token");

        assertThat(result.rawToken()).isNotBlank();
        assertThat(currentToken.getRevokedAt()).isNotNull();
        assertThat(currentToken.getReplacedByTokenId()).isEqualTo(result.refreshToken().getId());
        verify(refreshTokenRepository).save(currentToken);
    }

    @Test
    void revokeRefreshTokenShouldSetRevokedAt() {
        RefreshToken token = validRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hash("refresh-token"))).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeRefreshToken("refresh-token");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void revokeAllUserRefreshTokensShouldRevokeAllActiveTokens() {
        RefreshToken firstToken = validRefreshToken("first-token");
        RefreshToken secondToken = validRefreshToken("second-token");
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
                .thenReturn(List.of(firstToken, secondToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.revokeAllUserRefreshTokens(user.getId());

        assertThat(firstToken.getRevokedAt()).isNotNull();
        assertThat(secondToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(firstToken);
        verify(refreshTokenRepository).save(secondToken);
    }

    private RefreshToken savedToken(RefreshToken refreshToken) {
        if (refreshToken.getId() == null) {
            refreshToken.setId(UUID.randomUUID());
        }
        if (refreshToken.getCreatedAt() == null) {
            refreshToken.setCreatedAt(Instant.now());
        }
        return refreshToken;
    }

    private RefreshToken validRefreshToken(String rawToken) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setCreatedAt(Instant.now());
        return refreshToken;
    }

    private User validUser() {
        User validUser = new User();
        validUser.setId(UUID.randomUUID());
        validUser.setEmail("melina@gmail.com");
        validUser.setUsername("meli123");
        validUser.setPasswordHash("$2a$10$hash");
        validUser.setRole(UserRole.USER);
        validUser.setStatus(UserStatus.ACTIVE);
        validUser.setCreatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        validUser.setUpdatedAt(Instant.parse("2026-05-06T12:00:00Z"));
        return validUser;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError("Could not hash token", exception);
        }
    }
}
