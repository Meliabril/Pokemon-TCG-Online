package ar.edu.utn.frc.tup.piii.dtos.user;

import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDto(
        @NotNull(message = "Status is required")
        UserStatus status) {
}
