package ar.edu.utn.frc.tup.piii.controllers.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.LogoutResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResendVerificationCodeRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.TokenResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifyAccountRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.security.RefreshTokenCookieService;
import ar.edu.utn.frc.tup.piii.services.auth.AuthService;
import ar.edu.utn.frc.tup.piii.services.auth.AuthSessionResult;
import ar.edu.utn.frc.tup.piii.services.auth.EmailVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints for user authentication and session management")
@RequiredArgsConstructor

public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/login")
    public AuthResponseDto login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        AuthSessionResult session = authService.login(request);
        refreshTokenCookieService.addRefreshTokenCookie(response, session.refreshToken());
        return toAuthResponse(session);
    }

    @PostMapping("/refresh")
    public TokenResponseDto refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthSessionResult session = authService.refresh(requiredRefreshToken(request));
        refreshTokenCookieService.addRefreshTokenCookie(response, session.refreshToken());
        return toTokenResponse(session);
    }

    @PostMapping("/verify-account")
    public AuthResponseDto verifyAccount(
            @Valid @RequestBody VerifyAccountRequestDto request,
            HttpServletResponse response) {
        AuthSessionResult session = emailVerificationService.verifyAccount(request.email(), request.code());
        refreshTokenCookieService.addRefreshTokenCookie(response, session.refreshToken());
        return toAuthResponse(session);
    }

    @PostMapping("/resend-verification-code")
    public GenericMessageResponseDto resendVerificationCode(
            @Valid @RequestBody ResendVerificationCodeRequestDto request) {
        emailVerificationService.resendVerificationCode(request.email());
        return new GenericMessageResponseDto("Código reenviado correctamente.");
    }

    @PostMapping("/logout")
    public LogoutResponseDto logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(refreshTokenCookieService.resolveRefreshToken(request));
        refreshTokenCookieService.clearRefreshTokenCookie(response);
        return new LogoutResponseDto("Logout successful");
    }

    @GetMapping("/me")
    public UserResponseDto me(Authentication authentication) {
        return authService.getCurrentUser(UUID.fromString(authentication.getName()));
    }

    private String requiredRefreshToken(HttpServletRequest request) {
        String refreshToken = refreshTokenCookieService.resolveRefreshToken(request);
        if (refreshToken == null) {
            throw new InvalidTokenException("Refresh token is required");
        }

        return refreshToken;
    }

    private AuthResponseDto toAuthResponse(AuthSessionResult session) {
        return new AuthResponseDto(
                session.accessToken(),
                session.tokenType(),
                session.expiresIn(),
                session.user());
    }

    private TokenResponseDto toTokenResponse(AuthSessionResult session) {
        return new TokenResponseDto(
                session.accessToken(),
                session.tokenType(),
                session.expiresIn());
    }
}
