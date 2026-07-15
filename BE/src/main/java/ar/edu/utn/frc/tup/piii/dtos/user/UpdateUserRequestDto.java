package ar.edu.utn.frc.tup.piii.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(
        @Email(message = "Email must have a valid format")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must include a valid domain")
        String email,

        @Pattern(regexp = "^\\S.*$", message = "Username must not be blank")
        String username,

        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^\\p{L}\\d]).{8,}$",
                message = "Password must have at least 8 characters, one uppercase letter, one number and one special character"
        )
        String password,

        @Size(max = 1024, message = "Avatar must have at most 1024 characters")
        String avatar) {

    public UpdateUserRequestDto(String email, String username, String password) {
        this(email, username, password, null);
    }
}
