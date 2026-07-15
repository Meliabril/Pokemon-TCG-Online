package ar.edu.utn.frc.tup.piii.services.auth.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordServiceImpl service;

    @Test
    void hash_delegatesToEncoder() {
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        assertThat(service.hash("plain")).isEqualTo("hashed");
    }

    @Test
    void matches_returnsTrue_whenEncoderMatches() {
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);
        assertThat(service.matches("plain", "hashed")).isTrue();
    }

    @Test
    void matches_returnsFalse_whenEncoderDoesNotMatch() {
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        assertThat(service.matches("wrong", "hashed")).isFalse();
    }
}
