package ar.edu.utn.frc.tup.piii.dtos.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank(message = "Refresh token is required")
        String refreshToken) {
}
