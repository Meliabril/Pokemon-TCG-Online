package ar.edu.utn.frc.tup.piii.dtos.websocket;

import java.time.Instant;
import java.util.Map;

public record WebSocketErrorDto(
        String error,
        String message,
        Instant timestamp,
        Map<String, Object> details) {

    public WebSocketErrorDto {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
