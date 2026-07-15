package ar.edu.utn.frc.tup.piii.controllers.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.user.impl.AdminUserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
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

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserServiceImpl adminUserService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldReturnCreatedWhenAdminRequestIsValid() throws Exception {
        when(adminUserService.createAdmin(any(RegisterRequestDto.class))).thenReturn(adminResponse());

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@gmail.com"))
                .andExpect(jsonPath("$.username").value("admin123"))
                .andExpect(jsonPath("$.avatar").value("avatar-admin-01"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldNotExposePasswordHashWhenAdminIsCreated() throws Exception {
        when(adminUserService.createAdmin(any(RegisterRequestDto.class))).thenReturn(adminResponse());

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
    }

    @Test
    void shouldReturnOkWhenStatusIsChangedToActive() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.changeUserStatus(eq(userId), any(UpdateUserStatusRequestDto.class)))
                .thenReturn(statusUserResponse(userId, UserStatus.ACTIVE));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnOkWhenStatusIsChangedToBlocked() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.changeUserStatus(eq(userId), any(UpdateUserStatusRequestDto.class)))
                .thenReturn(statusUserResponse(userId, UserStatus.BLOCKED));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.BLOCKED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void shouldReturnOkWhenStatusIsChangedToDeleted() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.changeUserStatus(eq(userId), any(UpdateUserStatusRequestDto.class)))
                .thenReturn(statusUserResponse(userId, UserStatus.DELETED));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.DELETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    void shouldReturnNotFoundWhenChangingStatusOfNonExistentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.changeUserStatus(eq(userId), any(UpdateUserStatusRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + userId));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.BLOCKED))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.status").exists());
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void shouldReturnUserWithoutPasswordHashAfterStatusChange() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.changeUserStatus(eq(userId), any(UpdateUserStatusRequestDto.class)))
                .thenReturn(statusUserResponse(userId, UserStatus.BLOCKED));

        mockMvc.perform(patch("/api/admin/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserStatusRequestDto(UserStatus.BLOCKED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("password"))))
                .andExpect(jsonPath("$", not(hasKey("passwordHash"))));
    }

    @Test
    void shouldReturnOkWhenAdminGetsUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.getUserById(userId)).thenReturn(statusUserResponse(userId, UserStatus.ACTIVE));

        mockMvc.perform(get("/api/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("melina@gmail.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnOkWhenAdminGetsAllUsers() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminUserService.getAllUsers()).thenReturn(List.of(statusUserResponse(userId, UserStatus.ACTIVE)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(userId.toString()))
                .andExpect(jsonPath("$[0].email").value("melina@gmail.com"));
    }

    private RegisterRequestDto adminRequest() {
        return new RegisterRequestDto(
                "admin@gmail.com",
                "admin123",
                "Contrasena123!",
                "avatar-admin-01");
    }

    private RegisterResponseDto adminResponse() {
        return new RegisterResponseDto(
                UUID.randomUUID(),
                "admin@gmail.com",
                "admin123",
                "avatar-admin-01",
                UserRole.ADMIN,
                UserStatus.ACTIVE,
                true,
                null,
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T12:00:00Z"));
    }

    private UserResponseDto statusUserResponse(UUID userId, UserStatus status) {
        return new UserResponseDto(
                userId,
                "melina@gmail.com",
                "meli123",
                "avatar-pikachu-01",
                UserRole.USER,
                status,
                true,
                null,
                Instant.parse("2026-05-06T12:00:00Z"),
                Instant.parse("2026-05-06T13:00:00Z"));
    }
}
