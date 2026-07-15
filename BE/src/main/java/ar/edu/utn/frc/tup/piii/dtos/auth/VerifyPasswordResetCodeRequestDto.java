package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPasswordResetCodeRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        String email,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Code must have 6 digits")
        String code) {
}
