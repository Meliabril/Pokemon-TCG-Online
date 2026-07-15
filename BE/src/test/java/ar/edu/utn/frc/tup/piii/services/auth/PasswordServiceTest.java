package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.services.auth.impl.PasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordServiceImpl(new BCryptPasswordEncoder());
    }

    @Test
    void hashShouldNotReturnPlainPassword() {
        String hash = passwordService.hash("Contrasena123!");

        assertThat(hash).isNotEqualTo("Contrasena123!");
    }

    @Test
    void hashShouldReturnBCryptHash() {
        String hash = passwordService.hash("Contrasena123!");

        assertThat(hash).startsWith("$2");
    }

    @Test
    void matchesShouldReturnTrueWhenPasswordMatchesHash() {
        String hash = passwordService.hash("Contrasena123!");

        assertThat(passwordService.matches("Contrasena123!", hash)).isTrue();
    }

    @Test
    void matchesShouldReturnFalseWhenPasswordDoesNotMatchHash() {
        String hash = passwordService.hash("Contrasena123!");

        assertThat(passwordService.matches("WrongPass123!", hash)).isFalse();
    }
}
