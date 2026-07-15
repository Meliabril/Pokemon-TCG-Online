package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.websocket.allowed-origin-patterns=http://localhost:4200,http://192.168.*.*:4200")
class WebSocketConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebSocketConfig webSocketConfig;

    @Test
    void shouldRegisterWebSocketConfigBean() {
        assertThat(applicationContext.getBean(WebSocketConfig.class)).isNotNull();
    }

    @Test
    void shouldEnableWebSocketMessageBroker() {
        assertThat(AnnotationUtils.findAnnotation(WebSocketConfig.class, EnableWebSocketMessageBroker.class))
                .isNotNull();
    }

    @Test
    void shouldRegisterWebSocketEndpointWithLanOriginPatterns() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registration).setAllowedOriginPatterns(
                "http://localhost:4200",
                "http://192.168.*.*:4200");
    }
}
