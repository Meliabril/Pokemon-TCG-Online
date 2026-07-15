package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.controllers.websocket.GamePresenceChannelInterceptor;
import ar.edu.utn.frc.tup.piii.security.StompJwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOriginPatterns;
    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
    private final GamePresenceChannelInterceptor gamePresenceChannelInterceptor;

    public WebSocketConfig(
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            GamePresenceChannelInterceptor gamePresenceChannelInterceptor,
            @Value("${app.websocket.allowed-origin-patterns:http://localhost:4200,http://127.0.0.1:4200,http://192.168.*.*:4200,http://10.*.*.*:4200,http://172.*.*.*:4200}")
            String allowedOriginPatterns) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.gamePresenceChannelInterceptor = gamePresenceChannelInterceptor;
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor, gamePresenceChannelInterceptor);
    }
}
