package ar.edu.utn.frc.tup.piii.services.user.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.services.user.AdminUserService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserService userService;

    @Override
    @Transactional
    public RegisterResponseDto createAdmin(RegisterRequestDto request) {
        return userService.createAdmin(request);
    }

    @Override
    @Transactional
    public UserResponseDto changeUserStatus(UUID id, UpdateUserStatusRequestDto request) {
        return userService.changeUserStatus(id, request);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID id) {
        return userService.getUserById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }
}
