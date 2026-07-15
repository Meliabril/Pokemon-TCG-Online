package ar.edu.utn.frc.tup.piii.dtos.user;

import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
        UUID id,
        String email,
        String username,
        String avatar,
        UserRole role,
        UserStatus status,
        Boolean emailVerified,
        String matchmakingCode,
        Instant createdAt,
        Instant updatedAt) {

    public UserResponseDto(
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
