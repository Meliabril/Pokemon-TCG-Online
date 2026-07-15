package ar.edu.utn.frc.tup.piii.dtos.auth;

public record PasswordCodeVerificationResponseDto(
        String verificationToken,
        String message) {
}
