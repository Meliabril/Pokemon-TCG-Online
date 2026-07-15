package ar.edu.utn.frc.tup.piii.dtos.websocket;

import java.util.UUID;

public record CardHoverChangedVisualEventDto(
        String type,
        UUID gameId,
        UUID playerId,
        String zone,
        String owner,
        Integer visualIndex,
        UUID cardInstanceId,
        Boolean hovered,
        Boolean occupied) {
}
