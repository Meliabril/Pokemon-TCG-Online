package ar.edu.utn.frc.tup.piii.dtos.game;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Standard response after processing a game action.")
public record GameActionResponseDto(
        @Schema(description = "Indicates whether the action was accepted and applied.", example = "true")
        boolean success,
        @NotNull
        @Schema(description = "Human-readable result message.", example = "Action processed successfully")
        String message,
        @NotNull
        @PositiveOrZero
        @Schema(description = "State version after applying the action.", example = "1")
        Integer newStateVersion,
        @Schema(description = "Optional action-specific response data.")
        Object data) {
}

