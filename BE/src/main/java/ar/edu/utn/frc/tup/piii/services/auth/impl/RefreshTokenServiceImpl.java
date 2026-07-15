package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.exceptions.RefreshTokenRevokedException;
import ar.edu.utn.frc.tup.piii.exceptions.TokenExpiredException;
import ar.edu.utn.frc.tup.piii.repositories.RefreshTokenRepository;
import ar.edu.utn.frc.tup.piii.services.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.jwt.refresh-token-expiration-seconds:604800}")
    private Long expirationSeconds;

    @Override
    @Transactional
    public RefreshTokenResult createRefreshToken(User user) {
        String rawToken = generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(expirationSeconds));

        return new RefreshTokenResult(rawToken, refreshTokenRepository.save(refreshToken));
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new RefreshTokenRevokedException("Refresh token was revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public RefreshTokenResult rotateRefreshToken(String rawToken) {
        RefreshToken currentToken = validateRefreshToken(rawToken);
        RefreshTokenResult nextToken = createRefreshToken(currentToken.getUser());

        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenId(nextToken.refreshToken().getId());
        refreshTokenRepository.save(currentToken);

        return nextToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        RefreshToken refreshToken = validateRefreshToken(rawToken);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllUserRefreshTokens(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return BASE64_URL_ENCODER.encodeToString(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new InvalidTokenException("Could not hash refresh token");
        }
    }
}
