package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;

public record AuthSessionResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponseDto user) {
}
