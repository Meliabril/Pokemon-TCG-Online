package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifiedPasswordResetRequestDto(
        @NotBlank(message = "Verification token is required")
        String verificationToken,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {
}
