package ar.edu.utn.frc.tup.piii.dtos.common;

import java.time.Instant;
import java.util.Map;

public record ErrorApi(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors) {
}
