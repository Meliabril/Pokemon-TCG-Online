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
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameEventRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameStateSnapshotRepository;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GamePauseResumeServiceTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GamePauseUseCase gamePauseService;

    @Autowired
    private GameResumeUseCase gameResumeService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameStateSnapshotRepository gameStateSnapshotRepository;

    @Autowired
    private GameEventRepository gameEventRepository;

    @Autowired
    private GameSnapshotService gameSnapshotService;

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
    }

    @Test
    void shouldPauseGameAndStoreResumableSnapshot() {
        Game game = createGame(GameStatus.ACTIVE, TurnPhase.ATTACK, 5, 2);

        GameStateDto paused = gamePauseService.pause(game.getId(), "manual pause");
        Game savedGame = gameRepository.findById(game.getId()).orElseThrow();
        GameStateSnapshot snapshot = gameStateSnapshotRepository.findFirstByGame_IdOrderByVersionDesc(game.getId())
                .orElseThrow();
        GameEvent pausedEvent = gameEventRepository.findVisibleEvents(game.getId(), null).getFirst();

        assertThat(paused.status()).isEqualTo(GameStatus.PAUSED);
        assertThat(savedGame.getPauseReason()).isEqualTo("manual pause");
        assertThat(savedGame.getPausedAt()).isNotNull();
        assertThat(savedGame.getStateVersion()).isEqualTo(3);
        assertThat(snapshot.getVersion()).isEqualTo(2);
        assertThat(snapshot.getStateJson()).containsEntry("status", "ACTIVE");
        assertThat(pausedEvent.getEventType()).isEqualTo(GameEventType.GAME_PAUSED);
        assertThat(pausedEvent.getPayload()).containsEntry("reason", "manual pause");
    }

    @Test
    void shouldResumeGameFromLatestSnapshotAndClearPauseFields() {
        Game game = createGame(GameStatus.ACTIVE, TurnPhase.MAIN, 8, 4);

        gamePauseService.pause(game.getId(), "temporary disconnect");
        GameStateDto resumed = gameResumeService.resume(game.getId());
        Game savedGame = gameRepository.findById(game.getId()).orElseThrow();
        GameStateSnapshot latestSnapshot = gameStateSnapshotRepository.findFirstByGame_IdOrderByVersionDesc(game.getId())
                .orElseThrow();
        List<GameEvent> events = gameEventRepository.findVisibleEvents(game.getId(), null);

        assertThat(resumed.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(resumed.turn().currentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(resumed.turn().turnNumber()).isEqualTo(8);
        assertThat(savedGame.getPauseReason()).isNull();
        assertThat(savedGame.getPausedAt()).isNull();
        assertThat(savedGame.getStateVersion()).isEqualTo(6);
        assertThat(latestSnapshot.getVersion()).isEqualTo(6);
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getEventType()).isEqualTo(GameEventType.GAME_RESUMED);
        assertThat(events.get(1).getPayload()).containsEntry("resumedFromVersion", 4);
    }

    @Test
    void shouldRejectResumeWhenGameIsNotPaused() {
        Game game = createGame(GameStatus.ACTIVE, TurnPhase.DRAW, 1, 0);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameResumeService.resume(game.getId());
            }
        })
                .isInstanceOf(InvalidGameActionException.class);
    }

    @Test
    void shouldRejectResumeWhenPausedGameHasNoSnapshot() {
        Game game = createGame(GameStatus.PAUSED, TurnPhase.DRAW, 1, 0);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameResumeService.resume(game.getId());
            }
        })
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldPreserveRicherSnapshotFieldsAcrossPauseAndResume() {
        Game game = createGame(GameStatus.ACTIVE, TurnPhase.MAIN, 6, 3);
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        java.time.Instant now = java.time.Instant.parse("2026-05-24T22:30:00Z");
        insertUser(playerOne, "pauseplayerone", now);
        insertUser(playerTwo, "pauseplayertwo", now);
        GameStateDto visibleState = GameStateTestFactory.state(
                game.getId(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                6,
                3,
                playerOne,
                List.of(playerOne, playerTwo),
                true,
                false,
                true,
                java.util.Map.of(playerOne, 1, playerTwo, 0),
                java.util.Map.of(playerOne, List.of(SpecialConditionType.POISONED), playerTwo, List.of()),
                6,
                playerTwo,
                java.util.Map.of(pokemonInPlayId, 5),
                List.of(GameActionType.END_TURN),
                now);
        gameSnapshotService.saveSnapshot(game.getId(), 3, visibleState, null);

        GameStateDto paused = gamePauseService.pause(game.getId(), "rich state pause");
        GameStateDto resumed = gameResumeService.resume(game.getId());

        assertThat(paused.turn().energyAttachedThisTurn()).isTrue();
        assertThat(paused.turn().retreatedThisTurn()).isTrue();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(paused).get(playerOne)).containsExactly(SpecialConditionType.POISONED);
        assertThat(paused.turn().playerWhoWentFirstId()).isEqualTo(playerTwo);
        assertThat(paused.board().enteredPlayTurnByPokemonInPlayId()).containsEntry(pokemonInPlayId, 5);

        assertThat(resumed.turn().energyAttachedThisTurn()).isTrue();
        assertThat(resumed.turn().retreatedThisTurn()).isTrue();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(resumed).get(playerOne)).containsExactly(SpecialConditionType.POISONED);
        assertThat(resumed.turn().playerWhoWentFirstId()).isEqualTo(playerTwo);
        assertThat(resumed.board().enteredPlayTurnByPokemonInPlayId()).containsEntry(pokemonInPlayId, 5);
        assertThat(resumed.actions().availableActions()).containsExactly(GameActionType.END_TURN);
    }

    private Game createGame(GameStatus status, TurnPhase phase, int turnNumber, int stateVersion) {
        Game game = new Game();
        game.setStatus(status);
        game.setCurrentPhase(phase);
        game.setTurnNumber(turnNumber);
        game.setStateVersion(stateVersion);
        return gameRepository.saveAndFlush(game);
    }

    private void insertUser(UUID userId, String username, java.time.Instant now) {
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                username + "@example.com",
                username,
                "hash",
                "USER",
                "ACTIVE",
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now));
    }
}
