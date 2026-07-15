package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must include a valid domain")
        String email) {
}
