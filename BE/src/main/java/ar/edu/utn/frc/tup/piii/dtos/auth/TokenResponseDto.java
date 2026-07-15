package ar.edu.utn.frc.tup.piii.dtos.auth;

public record TokenResponseDto(
        String accessToken,
        String tokenType,
        Long expiresIn) {
}
