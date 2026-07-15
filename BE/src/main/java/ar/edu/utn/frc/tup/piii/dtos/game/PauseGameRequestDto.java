package ar.edu.utn.frc.tup.piii.dtos.game;

import jakarta.validation.constraints.Size;

public record PauseGameRequestDto(
        @Size(max = 120) String reason) {
}
