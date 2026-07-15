package ar.edu.utn.frc.tup.piii.services.user;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.DeleteUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserProfileRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserProfileResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.User;

import java.util.List;
import java.util.UUID;
public interface UserService {
    RegisterResponseDto register(RegisterRequestDto request);

    RegisterResponseDto createAdmin(RegisterRequestDto request);

    UserResponseDto updateUser(UUID id, UpdateUserRequestDto request);

    UserProfileResponseDto getCurrentUserProfile(UUID userId);

    UserProfileResponseDto updateCurrentUserProfile(UUID userId, UpdateUserProfileRequestDto request);

    UserResponseDto softDeleteUser(UUID id, DeleteUserRequestDto request);

    UserResponseDto changeUserStatus(UUID id, UpdateUserStatusRequestDto request);

    UserResponseDto getUserById(UUID id);

    List<UserResponseDto> getAllUsers();

    User getUserEntityById(UUID id);

    User getUserEntityByIdentifier(String identifier);
}
