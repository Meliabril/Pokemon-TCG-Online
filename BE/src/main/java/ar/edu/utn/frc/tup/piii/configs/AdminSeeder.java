package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private static final List<SeedAdmin> SEEDED_ADMINS = List.of(
            new SeedAdmin("admin@gmail.com", "admin", "Admin123!"),
            new SeedAdmin("admin2@gmail.com", "admin2", "Admin123!"));

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AdminSeeder(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        for (SeedAdmin seedAdmin : SEEDED_ADMINS) {
            if (userRepository.findByEmailIgnoreCase(seedAdmin.email()).isPresent()) {
                continue;
            }

            User admin = new User();
            admin.setEmail(seedAdmin.email());
            admin.setUsername(seedAdmin.username());
            admin.setPasswordHash(passwordService.hash(seedAdmin.password()));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);

            userRepository.save(admin);
        }
    }

    private record SeedAdmin(String email, String username, String password) {
    }
}
