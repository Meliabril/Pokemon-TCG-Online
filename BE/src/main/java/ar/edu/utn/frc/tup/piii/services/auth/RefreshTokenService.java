package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.entities.RefreshToken;
import ar.edu.utn.frc.tup.piii.entities.User;

import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenResult createRefreshToken(User user);

    RefreshToken validateRefreshToken(String rawToken);

    RefreshTokenResult rotateRefreshToken(String rawToken);

    void revokeRefreshToken(String rawToken);

    void revokeAllUserRefreshTokens(UUID userId);

    record RefreshTokenResult(String rawToken, RefreshToken refreshToken) {
    }
}
