package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Client request to perform a game action.")
public record GameActionRequestDto(
        @NotNull
        @Schema(description = "Target game identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID gameId,
        @NotNull
        @Schema(description = "Unique client-side id for idempotency.", example = "6fa459ea-ee8a-3ca4-894e-db77e160355e")
        UUID clientActionId,
        @NotNull
        @Schema(description = "Action type to execute.")
        GameActionType actionType,
        @NotNull
        @PositiveOrZero
        @Schema(description = "Expected current game state version for optimistic concurrency.", example = "0")
        Integer expectedStateVersion,
        @Schema(description = "Flexible action-specific payload.")
        Map<String, Object> payload) {

    public GameActionRequestDto {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }


    public Map<String, Object> parameters() {
        return this.payload;
    }
}
