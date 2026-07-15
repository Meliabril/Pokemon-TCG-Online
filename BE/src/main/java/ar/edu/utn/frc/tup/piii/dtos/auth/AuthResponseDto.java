package ar.edu.utn.frc.tup.piii.dtos.auth;

import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;

public record AuthResponseDto(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserResponseDto user) {
}
