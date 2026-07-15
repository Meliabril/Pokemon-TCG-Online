package ar.edu.utn.frc.tup.piii.controllers.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.ChangePasswordRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.DeleteUserRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserProfileRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserProfileResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.EmailAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.UsernameAlreadyExistsException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordChangeService;
import ar.edu.utn.frc.tup.piii.services.user.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceImpl userService;

    @MockBean
    private PasswordChangeService passwordChangeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldReturnCreatedWhenRegisterRequestIsValid() throws Exception {
        when(userService.register(any(RegisterRequestDto.class))).thenReturn(validResponse());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("melina@gmail.com"))
                .andExpect(jsonPath("$.username").value("meli123"))
                .andExpect(jsonPath("$.avatar").value("avatar-pikachu-01"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        when(userService.register(any(RegisterRequestDto.class)))
                .thenThrow(new EmailAlreadyExistsException("melina@gmail.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
        when(userService.register(any(RegisterRequestDto.class)))
                .thenThrow(new UsernameAlreadyExistsException("meli123"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordIsWeak() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "melina@gmail.com", "meli123", "password");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void shouldReturnCurrentUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getCurrentUserProfile(userId)).thenReturn(validProfileResponse());

        mockMvc.perform(get("/api/users/me/profile")
                        .principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meli123"))
                .andExpect(jsonPath("$.email").value("melina@gmail.com"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.avatar").value("avatar-pikachu-01"))
                .andExpect(jsonPath("$", not(hasKey("id"))))
                .andExpect(jsonPath("$", not(hasKey("role"))))
                .andExpect(jsonPath("$", not(hasKey("status"))))
                .andExpect(jsonPath("$", not(hasKey("createdAt"))))
                .andExpect(jsonPath("$", not(hasKey("updatedAt"))));
    }

    @Test
    void shouldUpdateCurrentUserProfileFromAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto(
                "newtrainer",
                "avatar-snorlax-01");
        when(userService.updateCurrentUserProfile(eq(userId), any(UpdateUserProfileRequestDto.class)))
                .thenReturn(new UserProfileResponseDto(
                        "newtrainer",
                        "melina@gmail.com",
                        true,
                        "avatar-snorlax-01"));

        mockMvc.perform(patch("/api/users/me/profile")
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newtrainer"))
                .andExpect(jsonPath("$.email").value("melina@gmail.com"))
                .andExpect(jsonPath("$.avatar").value("avatar-snorlax-01"))
                .andExpect(jsonPath("$", not(hasKey("id"))))
                .andExpect(jsonPath("$", not(hasKey("role"))))
                .andExpect(jsonPath("$", not(hasKey("status"))));
    }

    @Test
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "melina@gmail", "meli123", "Contrasena123!");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void shouldNotExposePasswordInRegisterResponse() throws Exception {
        when(userService.register(any(RegisterRequestDto.class))).thenReturn(validResponse());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("firstName"))))
                .andExpect(jsonPath("$", not(hasKey("lastName"))))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
    }

    @Test
    void shouldRequestPasswordChangeCodeForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/me/password-change/code")
                        .principal(authentication(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Codigo enviado al correo de la cuenta."));
    }

    @Test
    void shouldChangePasswordWithCodeForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request = new ChangePasswordRequestDto(
                UUID.randomUUID().toString(),
                "NuevaPass123!",
                "NuevaPass123!");

        mockMvc.perform(patch("/api/users/me/password")
                        .principal(authentication(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contrasena actualizada correctamente."));
    }

    @Test
    void shouldRejectPasswordChangeWhenVerificationTokenIsBlank() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequestDto request = new ChangePasswordRequestDto(
                "",
                "NuevaPass123!",
                "NuevaPass123!");

        mockMvc.perform(patch("/api/users/me/password")
                        .principal(authentication(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.verificationToken").exists());
    }

    @Test
    void shouldNotExposeGenericUserUpdateRoute() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOkWhenUserIsSoftDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.softDeleteUser(eq(userId), any(DeleteUserRequestDto.class))).thenReturn(deletedUserResponse(userId));

        mockMvc.perform(patch("/api/users/delete/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDeleteRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void shouldReturnNotFoundWhenSoftDeletingNonExistentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.softDeleteUser(eq(userId), any(DeleteUserRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + userId));

        mockMvc.perform(patch("/api/users/delete/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDeleteRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldNotExposePasswordHashAfterSoftDelete() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.softDeleteUser(eq(userId), any(DeleteUserRequestDto.class))).thenReturn(deletedUserResponse(userId));

        mockMvc.perform(patch("/api/users/delete/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDeleteRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("firstName"))))
                .andExpect(jsonPath("$", not(hasKey("lastName"))))
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
    }

    @Test
    void shouldNotExposeStatusChangeInCommonUserRoute() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.BLOCKED))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotExposeGetAllInCommonUserRoute() throws Exception {
        mockMvc.perform(get("/api/users/getAll"))
                .andExpect(status().isNotFound());
    }

    private RegisterRequestDto validRequest() {
        return new RegisterRequestDto(
                "melina@gmail.com",
                "meli123",
                "Contrasena123!",
                "avatar-pikachu-01");
    }

    private RegisterResponseDto validResponse() {
        return new RegisterResponseDto(
                UUID.randomUUID(),
                "melina@gmail.com",
                "meli123",
                "avatar-pikachu-01",
                UserRole.USER,
                UserStatus.PENDING_VERIFICATION,
                false,
                "Cuenta creada. Revisá tu email para verificarla.",
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T12:00:00Z"));
    }

    private UserResponseDto validUserResponse(UUID userId) {
        return new UserResponseDto(
                userId,
                "new@gmail.com",
                "newuser",
                "avatar-pikachu-01",
                UserRole.USER,
                UserStatus.ACTIVE,
                true,
                null,
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T13:00:00Z"));
    }

    private UserResponseDto deletedUserResponse(UUID userId) {
        return new UserResponseDto(
                userId,
                "melina@gmail.com",
                "meli123",
                "avatar-pikachu-01",
                UserRole.USER,
                UserStatus.DELETED,
                true,
                null,
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T13:00:00Z"));
    }

    private UserProfileResponseDto validProfileResponse() {
        return new UserProfileResponseDto(
                "meli123",
                "melina@gmail.com",
                true,
                "avatar-pikachu-01");
    }

    private DeleteUserRequestDto validDeleteRequest() {
        return new DeleteUserRequestDto("Contrasena123!");
    }

    private TestingAuthenticationToken authentication(UUID userId) {
        return new TestingAuthenticationToken(userId.toString(), null);
    }
}
