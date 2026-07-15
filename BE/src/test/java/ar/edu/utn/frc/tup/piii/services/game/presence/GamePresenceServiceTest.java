package ar.edu.utn.frc.tup.piii.services.game.presence;

import ar.edu.utn.frc.tup.piii.controllers.websocket.GameWebSocketPresenceRegistry;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamePresenceServiceTest {

    @Mock
    private GameParticipantRepository gameParticipantRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry;

    @Test
    void shouldPublishPresenceChangedAndConnectedEventsWhenParticipantConnects() {
        UUID gameId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant connectingParticipant = participant(game, secondUserId, 2, false);
        GamePresenceService service = buildService();

        when(gameParticipantRepository.findByGame_IdAndUserId(gameId, secondUserId))
                .thenReturn(Optional.of(connectingParticipant));
        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

        service.markConnected(gameId, secondUserId);

        assertThat(connectingParticipant.getConnected()).isTrue();
        assertThat(connectingParticipant.getLastSeenAt()).isNotNull();
        verify(gameParticipantRepository).save(connectingParticipant);
        verify(applicationEventPublisher).publishEvent(argThat(presenceChangedEvent(
                gameId,
                secondUserId,
                true,
                Integer.valueOf(0),
                true,
                null)));
        verify(applicationEventPublisher).publishEvent(new GameParticipantConnectedEvent(gameId, secondUserId));
    }

    @Test
    void shouldNotPublishConnectedEventWhenParticipantWasAlreadyConnected() {
        UUID gameId = UUID.randomUUID();
        UUID connectingUserId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant connectingParticipant = participant(game, connectingUserId, 1, true);
        GamePresenceService service = buildService();

        when(gameParticipantRepository.findByGame_IdAndUserId(gameId, connectingUserId))
                .thenReturn(Optional.of(connectingParticipant));
        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

        service.markConnected(gameId, connectingUserId);

        assertThat(connectingParticipant.getConnected()).isTrue();
        assertThat(connectingParticipant.getLastSeenAt()).isNotNull();
        verify(applicationEventPublisher).publishEvent(argThat(presenceChangedEvent(
                gameId,
                connectingUserId,
                true,
                null,
                false,
                null)));
        verify(applicationEventPublisher, never()).publishEvent(new GameParticipantConnectedEvent(gameId, connectingUserId));
    }

    @Test
    void shouldMarkParticipantDisconnected() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant participant = participant(game, userId, 1, true);
        participant.setLastSeenAt(Instant.parse("2026-05-30T12:00:00Z"));
        GamePresenceService service = buildService();

        when(gameParticipantRepository.findByGame_IdAndUserId(gameId, userId)).thenReturn(Optional.of(participant));
        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

        service.markDisconnected(gameId, userId);

        assertThat(participant.getConnected()).isFalse();
        verify(gameParticipantRepository).save(participant);
        verify(applicationEventPublisher).publishEvent(argThat(presenceChangedEvent(
                gameId,
                userId,
                false,
                null,
                false,
                Instant.parse("2026-05-30T12:00:00Z"))));
    }

    @Test
    void shouldMarkParticipantDisconnectedWhenExplicitLeaveRemovesTrackedGameSession() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant participant = participant(game, userId, 1, true);
        participant.setLastSeenAt(Instant.parse("2026-05-30T12:00:00Z"));
        GamePresenceService service = buildService();

        when(gameWebSocketPresenceRegistry.unregister("session-1", gameId, userId)).thenReturn(true);
        when(gameParticipantRepository.findByGame_IdAndUserId(gameId, userId)).thenReturn(Optional.of(participant));
        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

        service.leaveGame("session-1", gameId, userId);

        assertThat(participant.getConnected()).isFalse();
        verify(gameParticipantRepository).save(participant);
        verify(applicationEventPublisher).publishEvent(argThat(presenceChangedEvent(
                gameId,
                userId,
                false,
                null,
                false,
                Instant.parse("2026-05-30T12:00:00Z"))));
    }

    @Test
    void shouldIgnoreExplicitLeaveWhenSessionWasNotTrackingThatGame() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GamePresenceService service = buildService();

        when(gameWebSocketPresenceRegistry.unregister("session-1", gameId, userId)).thenReturn(false);

        service.leaveGame("session-1", gameId, userId);

        verify(gameParticipantRepository, never()).findByGame_IdAndUserId(gameId, userId);
        verify(applicationEventPublisher, never()).publishEvent(argThat((Object event) -> event instanceof GameParticipantPresenceChangedEvent));
    }

    @Test
    void shouldStartTransactionWhenExplicitLeaveDisconnectsParticipant() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TransactionalLeaveGameTestConfiguration.class)) {
            UUID gameId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Game game = waitingGame(gameId);
            GameParticipant participant = participant(game, userId, 1, true);
            participant.setLastSeenAt(Instant.parse("2026-05-30T12:00:00Z"));

            GamePresenceService service = context.getBean(GamePresenceService.class);
            TestTransactionManager transactionManager = context.getBean(TestTransactionManager.class);
            GameWebSocketPresenceRegistry registry = context.getBean(GameWebSocketPresenceRegistry.class);
            GameParticipantRepository participantRepository = context.getBean(GameParticipantRepository.class);
            GameRepository repository = context.getBean(GameRepository.class);

            when(registry.unregister("session-1", gameId, userId)).thenReturn(true);
            when(participantRepository.findByGame_IdAndUserId(gameId, userId)).thenReturn(Optional.of(participant));
            when(repository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));

            service.leaveGame("session-1", gameId, userId);

            assertThat(AopUtils.isAopProxy(service)).isTrue();
            assertThat(transactionManager.getBeginCount()).isEqualTo(1);
            assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        }
    }

    @Test
    void shouldRejectPresenceUpdatesWhenParticipantDoesNotExist() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GamePresenceService service = buildService();

        when(gameParticipantRepository.findByGame_IdAndUserId(gameId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markConnected(gameId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    private GamePresenceService buildService() {
        return new GamePresenceServiceImpl(
                gameParticipantRepository,
                gameRepository,
                applicationEventPublisher,
                gameWebSocketPresenceRegistry);
    }

    private ArgumentMatcher<Object> presenceChangedEvent(
            UUID expectedGameId,
            UUID expectedUserId,
            boolean expectedConnected,
            Integer expectedStateVersion,
            boolean requireLastSeenAt,
            Instant expectedLastSeenAt) {
        return event -> {
            if (!(event instanceof GameParticipantPresenceChangedEvent changedEvent)) {
                return false;
            }
            if (!expectedGameId.equals(changedEvent.gameId())) {
                return false;
            }
            if (!expectedUserId.equals(changedEvent.userId())) {
                return false;
            }
            if (changedEvent.connected() != expectedConnected) {
                return false;
            }
            if (expectedStateVersion != null && changedEvent.stateVersion() != expectedStateVersion.intValue()) {
                return false;
            }
            if (requireLastSeenAt && changedEvent.lastSeenAt() == null) {
                return false;
            }
            return expectedLastSeenAt == null || expectedLastSeenAt.equals(changedEvent.lastSeenAt());
        };
    }

    private Game waitingGame(UUID gameId) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        return game;
    }

    private GameParticipant participant(Game game, UUID userId, int playerOrder, boolean connected) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setDeckId(UUID.randomUUID());
        participant.setPlayerOrder(playerOrder);
        participant.setConnected(connected);
        return participant;
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAspectJAutoProxy(proxyTargetClass = false)
    static class TransactionalLeaveGameTestConfiguration {

        @Bean
        TestTransactionManager transactionManager() {
            return new TestTransactionManager();
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
        ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }

        @Bean
        GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry() {
            return mock(GameWebSocketPresenceRegistry.class);
        }

        @Bean
        GamePresenceService gamePresenceService(
                GameParticipantRepository gameParticipantRepository,
                GameRepository gameRepository,
                ApplicationEventPublisher applicationEventPublisher,
                GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry) {
            return new GamePresenceServiceImpl(
                    gameParticipantRepository,
                    gameRepository,
                    applicationEventPublisher,
                    gameWebSocketPresenceRegistry);
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {

        private int beginCount;
        private int commitCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        int getBeginCount() {
            return beginCount;
        }

        int getCommitCount() {
            return commitCount;
        }
    }
}
