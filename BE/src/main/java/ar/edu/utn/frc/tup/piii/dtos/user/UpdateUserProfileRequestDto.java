package ar.edu.utn.frc.tup.piii.dtos.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequestDto(
        @Pattern(regexp = "^\\S.*$", message = "Username must not be blank")
        String username,

        @Size(max = 1024, message = "Avatar must have at most 1024 characters")
        String avatar) {
}
