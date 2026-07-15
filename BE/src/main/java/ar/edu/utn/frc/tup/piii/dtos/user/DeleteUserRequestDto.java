package ar.edu.utn.frc.tup.piii.dtos.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword) {
}
