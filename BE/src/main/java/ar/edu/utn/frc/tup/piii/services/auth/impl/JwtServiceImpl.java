package ar.edu.utn.frc.tup.piii.services.auth.impl;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.exceptions.TokenExpiredException;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    @Value("${app.jwt.secret:change-this-secret-for-production-change-this-secret}")
    private String secret;
    @Value("${app.jwt.access-token-expiration-seconds:900}")
    private Long expirationSeconds;

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("status", user.getStatus().name());
        claims.put("jti", UUID.randomUUID().toString());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiration.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedClaims = encodeJson(claims);
        String unsignedToken = encodedHeader + "." + encodedClaims;
        return unsignedToken + "." + sign(unsignedToken);
    }

    @Override
    public boolean validateToken(String token) {
        Map<String, Object> claims = claims(token);
        if (extractInstant(claims, "exp").isBefore(Instant.now())) {
            throw new TokenExpiredException("Token expired");
        }
        return true;
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString((String) claims(token).get("sub"));
    }

    @Override
    public String extractRole(String token) {
        return (String) claims(token).get("role");
    }

    @Override
    public UUID extractJti(String token) {
        return UUID.fromString((String) claims(token).get("jti"));
    }

    @Override
    public Instant extractExpiration(String token) {
        return extractInstant(claims(token), "exp");
    }

    @Override
    public Long getAccessTokenExpirationSeconds() {
        return expirationSeconds;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new InvalidTokenException("Could not create token");
        }
    }

    private Map<String, Object> claims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("Invalid token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new InvalidTokenException("Invalid token signature");
        }

        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new InvalidTokenException("Invalid token payload");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new InvalidTokenException("Could not sign token");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigestHolder.equals(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private Instant extractInstant(Map<String, Object> claims, String key) {
        Number epochSeconds = (Number) claims.get(key);
        return Instant.ofEpochSecond(epochSeconds.longValue());
    }

    private static final class MessageDigestHolder {
        private MessageDigestHolder() {
        }

        static boolean equals(byte[] left, byte[] right) {
            return java.security.MessageDigest.isEqual(left, right);
        }
    }
}
