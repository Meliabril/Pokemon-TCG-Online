package ar.edu.utn.frc.tup.piii.dtos.user;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Verification token is required")
        String verificationToken,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {
}
