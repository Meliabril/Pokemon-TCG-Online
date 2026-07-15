package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.User;

import java.time.Instant;
import java.util.UUID;

public interface JwtService {

    String generateAccessToken(User user);

    boolean validateToken(String token);

    UUID extractUserId(String token);

    String extractRole(String token);

    UUID extractJti(String token);

    Instant extractExpiration(String token);

    Long getAccessTokenExpirationSeconds();
}
