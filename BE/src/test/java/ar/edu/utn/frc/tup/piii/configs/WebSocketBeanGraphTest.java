package ar.edu.utn.frc.tup.piii.configs;



import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.controllers.websocket.GamePresenceChannelInterceptor;
import ar.edu.utn.frc.tup.piii.controllers.websocket.GamePresenceDisconnectListener;
import ar.edu.utn.frc.tup.piii.controllers.websocket.GameWebSocketPresenceRegistry;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.security.StompJwtChannelInterceptor;
import ar.edu.utn.frc.tup.piii.services.auth.JwtService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameRealtimeEventService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameService;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceAutoStartListener;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceNotificationListener;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.GameRealtimeEventServiceImpl;
import ar.edu.utn.frc.tup.piii.services.websocket.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.services.websocket.WebSocketGameEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class WebSocketBeanGraphTest {

    @Test
    void shouldBuildWebSocketBeanGraphWithoutCircularDependencies() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(WebSocketConfig.class, TestBeans.class);

            assertThatCode(context::refresh).doesNotThrowAnyException();
            assertThat(context.getBean(WebSocketConfig.class)).isNotNull();
            assertThat(context.getBean(GamePresenceChannelInterceptor.class)).isNotNull();
            assertThat(context.getBean(GameRealtimeEventService.class)).isNotNull();
            assertThat(context.getBean(WebSocketGameEventPublisher.class)).isNotNull();
        }
    }

    @Configuration
    static class TestBeans {

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        GameParticipantRepository gameParticipantRepository() {
            return mock(GameParticipantRepository.class);
        }

        @Bean
        GameRepository gameRepository() {
            return mock(GameRepository.class);
        }

        @Bean
        GameService gameService() {
            return mock(GameService.class);
        }

        @Bean
        GameEventService gameEventService() {
            return mock(GameEventService.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry() {
            return new GameWebSocketPresenceRegistry();
        }

        @Bean
        StompJwtChannelInterceptor stompJwtChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
            return new StompJwtChannelInterceptor(jwtService, userRepository);
        }

        @Bean
        GamePresenceService gamePresenceService(
                GameParticipantRepository gameParticipantRepository,
                GameRepository gameRepository,
                org.springframework.context.ApplicationEventPublisher applicationEventPublisher,
                GameWebSocketPresenceRegistry registry) {
            return new GamePresenceServiceImpl(gameParticipantRepository, gameRepository, applicationEventPublisher, registry);
        }

        @Bean
        GamePresenceChannelInterceptor gamePresenceChannelInterceptor(
                GameWebSocketPresenceRegistry registry,
                GamePresenceService gamePresenceService) {
            return new GamePresenceChannelInterceptor(registry, gamePresenceService);
        }

        @Bean
        GamePresenceDisconnectListener gamePresenceDisconnectListener(
                GameWebSocketPresenceRegistry registry,
                GamePresenceService gamePresenceService) {
            return new GamePresenceDisconnectListener(registry, gamePresenceService);
        }

        @Bean
        GameEventPublisher gameEventPublisher(org.springframework.messaging.simp.SimpMessagingTemplate template) {
            return new WebSocketGameEventPublisher(template);
        }

        @Bean
        GameRealtimeEventService gameRealtimeEventService(
                GameEventService gameEventService,
                GameEventPublisher gameEventPublisher,
                ObjectMapper objectMapper) {
            return new GameRealtimeEventServiceImpl(gameEventService, gameEventPublisher, objectMapper);
        }

        @Bean
        GamePresenceNotificationListener gamePresenceNotificationListener(GameRealtimeEventService gameRealtimeEventService) {
            return new GamePresenceNotificationListener(gameRealtimeEventService);
        }

        @Bean
        GamePresenceAutoStartListener gamePresenceAutoStartListener(
                GameParticipantRepository gameParticipantRepository,
                GameRepository gameRepository,
                GameService gameService) {
            return new GamePresenceAutoStartListener(gameParticipantRepository, gameRepository, gameService);
        }
    }
}
