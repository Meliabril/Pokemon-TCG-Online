package ar.edu.utn.frc.tup.piii.services.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;

import java.util.List;
import java.util.UUID;

public interface AdminUserService {

    RegisterResponseDto createAdmin(RegisterRequestDto request);

    UserResponseDto changeUserStatus(UUID id, UpdateUserStatusRequestDto request);

    UserResponseDto getUserById(UUID id);

    List<UserResponseDto> getAllUsers();
}
