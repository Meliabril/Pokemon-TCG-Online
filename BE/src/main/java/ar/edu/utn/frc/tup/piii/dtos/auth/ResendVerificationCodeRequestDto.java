package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationCodeRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        String email) {
}
