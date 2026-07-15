package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must include a valid domain")
        String email,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Code must have exactly 6 numeric digits")
        String code,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$",
                message = "Password must have at least 8 characters, one uppercase letter, one number and one special character"
        )
        String newPassword) {
}
