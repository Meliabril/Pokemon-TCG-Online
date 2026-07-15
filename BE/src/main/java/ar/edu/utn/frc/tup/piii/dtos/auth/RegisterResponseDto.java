package ar.edu.utn.frc.tup.piii.dtos.auth;

import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponseDto(
        UUID id,
        String email,
        String username,
        String avatar,
        UserRole role,
        UserStatus status,
        Boolean emailVerified,
        String message,
        Instant createdAt,
        Instant updatedAt) {

    public RegisterResponseDto(
            UUID id,
            String email,
            String username,
            UserRole role,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this(id, email, username, null, role, status, null, null, createdAt, updatedAt);
    }
}
