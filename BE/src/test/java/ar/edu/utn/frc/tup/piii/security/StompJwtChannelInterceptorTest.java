package ar.edu.utn.frc.tup.piii.security;

import ar.edu.utn.frc.tup.piii.entities.User;
import ar.edu.utn.frc.tup.piii.entities.UserRole;
import ar.edu.utn.frc.tup.piii.entities.UserStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidTokenException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StompJwtChannelInterceptorTest {

    private JwtService jwtService;
    private UserRepository userRepository;
    private StompJwtChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userRepository = mock(UserRepository.class);
        interceptor = new StompJwtChannelInterceptor(jwtService, userRepository);
        channel = mock(MessageChannel.class);
    }

    @Test
    void shouldAuthenticateConnectFrameWithValidBearerToken() {
        UUID userId = UUID.randomUUID();
        Message<byte[]> message = connectMessage("Bearer valid-token");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser(userId)));

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isInstanceOf(StompPrincipal.class);
        assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
    }

    @Test
    void shouldIgnoreFramesThatAreNotConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void shouldRejectConnectFrameWithoutAuthorizationHeader() {
        Message<byte[]> message = connectMessage(null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authorization header");
    }

    @Test
    void shouldRejectConnectFrameWithInvalidToken() {
        Message<byte[]> message = connectMessage("Bearer invalid-token");

        when(jwtService.validateToken("invalid-token"))
                .thenThrow(new InvalidTokenException("Invalid token"));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("WebSocket authentication failed");
    }

    @Test
    void shouldRejectConnectFrameForInactiveUser() {
        UUID userId = UUID.randomUUID();
        Message<byte[]> message = connectMessage("Bearer valid-token");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser(userId)));

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User is not active");
    }

    private Message<byte[]> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorizationHeader != null) {
            accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private User activeUser(UUID userId) {
        return user(userId, UserStatus.ACTIVE);
    }

    private User inactiveUser(UUID userId) {
        return user(userId, UserStatus.BLOCKED);
    }

    private User user(UUID userId, UserStatus status) {
        User user = new User();
        user.setId(userId);
        user.setEmail("trainer@example.com");
        user.setUsername("trainer");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setStatus(status);
        user.setCreatedAt(Instant.parse("2026-05-20T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-20T10:00:00Z"));
        return user;
    }
}
