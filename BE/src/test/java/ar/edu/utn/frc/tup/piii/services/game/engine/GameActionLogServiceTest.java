package ar.edu.utn.frc.tup.piii.services.game.engine;



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
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameActionLogServiceTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameActionLogService gameActionLogService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameActionLogRepository gameActionLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_events");
        jdbcTemplate.update("delete from game_state_snapshots");
        jdbcTemplate.update("delete from game_action_logs");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPersistAcceptedActionAndReturnItInHistory() {
        UUID gameId = createGame();
        UUID actorUserId = UUID.randomUUID();

        insertUser(actorUserId, "log-user-one");

        gameActionLogService.recordAcceptedAction(
                gameId,
                actorUserId,
                GameActionType.DRAW_CARD,
                Map.of("step", 1),
                Map.of("drawn", 1),
                1,
                UUID.randomUUID());

        List<GameActionLogEntryDto> history = gameActionLogService.getHistory(gameId);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().actionType()).isEqualTo(GameActionType.DRAW_CARD);
        assertThat(history.getFirst().payload()).containsEntry("step", 1);
        assertThat(history.getFirst().result()).containsEntry("drawn", 1);
    }

    @Test
    void shouldRejectDuplicateVersionForSameGame() {
        UUID gameId = createGame();
        UUID actorUserId = UUID.randomUUID();

        insertUser(actorUserId, "log-user-two");

        gameActionLogService.recordAcceptedAction(
                gameId,
                actorUserId,
                GameActionType.DRAW_CARD,
                Map.of("step", 1),
                null,
                1,
                UUID.randomUUID());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameActionLogService.recordAcceptedAction(
                        gameId,
                        actorUserId,
                        GameActionType.END_TURN,
                        Map.of("step", 2),
                        null,
                        1,
                        UUID.randomUUID());
            }
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateClientActionIdForSameGameAndActor() {
        UUID gameId = createGame();
        UUID actorUserId = UUID.randomUUID();
        UUID clientActionId = UUID.randomUUID();

        insertUser(actorUserId, "log-user-three");

        gameActionLogService.recordAcceptedAction(
                gameId,
                actorUserId,
                GameActionType.DRAW_CARD,
                Map.of("step", 1),
                null,
                1,
                clientActionId);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameActionLogService.recordAcceptedAction(
                        gameId,
                        actorUserId,
                        GameActionType.END_TURN,
                        Map.of("step", 2),
                        null,
                        2,
                        clientActionId);
            }
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldThrowNotFoundWhenHistoryGameDoesNotExist() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameActionLogService.getHistory(UUID.randomUUID());
            }
        })
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UUID createGame() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        return gameRepository.saveAndFlush(game).getId();
    }

    private void insertUser(UUID userId, String username) {
        Instant now = Instant.parse("2026-05-17T20:00:00Z");
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                username + "@example.com",
                username,
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
    }
}
