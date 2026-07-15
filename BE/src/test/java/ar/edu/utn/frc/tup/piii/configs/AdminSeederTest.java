package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    private AdminSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new AdminSeeder(userRepository, passwordService);
    }

    @Test
    void shouldSeedBothPermanentAdminsWhenMissing() {
        when(userRepository.findByEmailIgnoreCase("admin@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("admin2@gmail.com")).thenReturn(Optional.empty());
        when(passwordService.hash(anyString())).thenAnswer(invocation -> "hash-" + invocation.getArgument(0, String.class));

        seeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        List<User> seededUsers = captor.getAllValues();

        assertThat(seededUsers)
                .extracting(User::getEmail)
                .containsExactly("admin@gmail.com", "admin2@gmail.com");
        assertThat(seededUsers)
                .extracting(User::getUsername)
                .containsExactly("admin", "admin2");
        assertThat(seededUsers)
                .extracting(User::getRole)
                .containsOnly(UserRole.ADMIN);
        assertThat(seededUsers)
                .extracting(User::getStatus)
                .containsOnly(UserStatus.ACTIVE);
        assertThat(seededUsers)
                .extracting(User::getEmailVerified)
                .containsOnly(true);
    }

    @Test
    void shouldOnlySeedMissingAdminAccounts() {
        User existingAdmin = new User();
        existingAdmin.setEmail("admin@gmail.com");
        when(userRepository.findByEmailIgnoreCase("admin@gmail.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmailIgnoreCase("admin2@gmail.com")).thenReturn(Optional.empty());
        when(passwordService.hash("Admin123!")).thenReturn("hash-Admin123!");

        seeder.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin2@gmail.com");
        assertThat(captor.getValue().getUsername()).isEqualTo("admin2");
    }
}
