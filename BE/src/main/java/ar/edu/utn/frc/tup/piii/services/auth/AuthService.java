package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;

import java.util.UUID;

public interface AuthService {

    AuthSessionResult login(LoginRequestDto request);

    AuthSessionResult refresh(String refreshToken);

    void logout(String refreshToken);

    UserResponseDto getCurrentUser(UUID userId);
}
