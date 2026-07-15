package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserProfileResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDto toUserResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getId().toString().substring(0, 6).toUpperCase(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public UserProfileResponseDto toUserProfileResponseDto(User user) {
        return new UserProfileResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerified(),
                user.getAvatar());
    }

    public RegisterResponseDto toRegisterResponseDto(User user) {
        return toRegisterResponseDto(user, null);
    }

    public RegisterResponseDto toRegisterResponseDto(User user, String message) {
        return new RegisterResponseDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerified(),
                message,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }


}
