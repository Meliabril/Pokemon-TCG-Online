package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must have a valid format")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must include a valid domain")
        String email,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$",
                message = "Password must have at least 8 characters, one uppercase letter, one number and one special character"
        )
        String password,

        @Size(max = 1024, message = "Avatar must have at most 1024 characters")
        String avatar) {

    public RegisterRequestDto(String email, String username, String password) {
        this(email, username, password, null);
    }
}
