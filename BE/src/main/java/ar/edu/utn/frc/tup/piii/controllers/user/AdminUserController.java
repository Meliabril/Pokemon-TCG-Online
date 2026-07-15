package ar.edu.utn.frc.tup.piii.controllers.user;

import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UpdateUserStatusRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.user.UserResponseDto;
import ar.edu.utn.frc.tup.piii.services.user.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;
@CrossOrigin(originPatterns = {
        "http://localhost:*",
        "http://127.0.0.1:*"
})@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin user", description = "Admin-only user management operations")
@RequiredArgsConstructor

public class AdminUserController {

    private final AdminUserService adminUserService;


    @PostMapping({"", "/create"})
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto createAdmin(@Valid @RequestBody RegisterRequestDto request) {
        return adminUserService.createAdmin(request);
    }

    @PatchMapping("/{id}/status")
    public UserResponseDto changeUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequestDto request) {
        return adminUserService.changeUserStatus(id, request);
    }

    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable UUID id) {
        return adminUserService.getUserById(id);
    }

    @GetMapping({"", "/getAll"})
    public List<UserResponseDto> getAllUsers() {
        return adminUserService.getAllUsers();
    }
}
