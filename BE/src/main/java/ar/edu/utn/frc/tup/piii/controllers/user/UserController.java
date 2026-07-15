package ar.edu.utn.frc.tup.piii.controllers.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.ChangePasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.DeleteUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserProfileRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.VerifyPasswordChangeCodeRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserProfileResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordChangeService;
import ar.edu.utn.frc.tup.piii.services.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
@CrossOrigin(originPatterns = {
        "http://localhost:*",
        "http://127.0.0.1:*"
})@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints for user registration, updates, and soft deletion")
@RequiredArgsConstructor

public class UserController {

    private static final String PASSWORD_CHANGE_CODE_SENT_MESSAGE = "Codigo enviado al correo de la cuenta.";
    private static final String PASSWORD_CHANGED_MESSAGE = "Contrasena actualizada correctamente.";

    private final UserService userService;
    private final PasswordChangeService passwordChangeService;

    @Description("Create user")
    @PostMapping({"", "/create"})
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return userService.register(request);
    }

    @Description("Get current user profile")
    @GetMapping("/me/profile")
    public UserProfileResponseDto getCurrentUserProfile(Authentication authentication) {
        return userService.getCurrentUserProfile(userId(authentication));
    }

    @Description("Update current user profile")
    @PatchMapping("/me/profile")
    public UserProfileResponseDto updateCurrentUserProfile(
            Authentication authentication,
            @RequestBody UpdateUserProfileRequestDto request) {
        return userService.updateCurrentUserProfile(userId(authentication), request);
    }

    @Description("Request password change code for current user")
    @PostMapping("/me/password-change/code")
    public GenericMessageResponseDto requestPasswordChangeCode(Authentication authentication) {
        passwordChangeService.requestPasswordChangeCode(userId(authentication));
        return new GenericMessageResponseDto(PASSWORD_CHANGE_CODE_SENT_MESSAGE);
    }

    @Description("Verify password change code for current user")
    @PostMapping("/me/password-change/verify-code")
    public PasswordCodeVerificationResponseDto verifyPasswordChangeCode(
            Authentication authentication,
            @Valid @RequestBody VerifyPasswordChangeCodeRequestDto request) {
        return passwordChangeService.verifyPasswordChangeCode(userId(authentication), request);
    }

    @Description("Change current user password with email code")
    @PatchMapping("/me/password")
    public GenericMessageResponseDto changeCurrentUserPassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        passwordChangeService.changeCurrentUserPassword(userId(authentication), request);
        return new GenericMessageResponseDto(PASSWORD_CHANGED_MESSAGE);
    }

    @Description("Delete user with id and password")
    @PatchMapping("/delete/{id}")
    public UserResponseDto softDeleteUser(
            @PathVariable UUID id,
            @Valid @RequestBody DeleteUserRequestDto request) {
        return userService.softDeleteUser(id, request);
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
