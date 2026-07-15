package ar.edu.utn.frc.tup.piii.controllers.auth;

import ar.edu.utn.frc.tup.piii.dtos.auth.ForgotPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.GenericMessageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.PasswordCodeVerificationResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.ResetPasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifiedPasswordResetRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.VerifyPasswordResetCodeRequestDto;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordRecoveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(originPatterns = {
        "http://localhost:*",
        "http://127.0.0.1:*"
})
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Password Recovery", description = "Endpoints for password recovery")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private static final String FORGOT_MESSAGE =
            "Si el email pertenece a una cuenta activa, se enviara un codigo de recuperacion.";
    private static final String RESET_MESSAGE =
            "Contrasena actualizada correctamente. Inicia sesion nuevamente.";

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping({"/password/forgot", "/forgot-password"})
    public GenericMessageResponseDto forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwordRecoveryService.requestPasswordReset(request);
        return new GenericMessageResponseDto(FORGOT_MESSAGE);
    }

    @PostMapping({"/password/reset", "/reset-password"})
    public GenericMessageResponseDto resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwordRecoveryService.resetPassword(request);
        return new GenericMessageResponseDto(RESET_MESSAGE);
    }

    @PostMapping({"/password/verify-code", "/verify-password-code"})
    public PasswordCodeVerificationResponseDto verifyPasswordResetCode(
            @Valid @RequestBody VerifyPasswordResetCodeRequestDto request) {
        return passwordRecoveryService.verifyPasswordResetCode(request);
    }

    @PostMapping({"/password/reset/verified", "/reset-password/verified"})
    public GenericMessageResponseDto resetVerifiedPassword(
            @Valid @RequestBody VerifiedPasswordResetRequestDto request) {
        passwordRecoveryService.resetVerifiedPassword(request);
        return new GenericMessageResponseDto(RESET_MESSAGE);
    }
}
