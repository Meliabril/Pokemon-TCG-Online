package ar.edu.utn.frc.tup.piii.dtos.user;

public record UserProfileResponseDto(
        String username,
        String email,
        Boolean emailVerified,
        String avatar) {
}

