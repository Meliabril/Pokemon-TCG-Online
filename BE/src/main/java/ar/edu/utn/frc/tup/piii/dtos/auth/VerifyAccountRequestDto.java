package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyAccountRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must have exactly 6 digits")
        String code) {
}
