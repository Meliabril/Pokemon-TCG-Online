package ar.edu.utn.frc.tup.piii.dtos.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordWithCodeRequestDto(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Code must have 6 digits")
        String code,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmPassword) {
}
