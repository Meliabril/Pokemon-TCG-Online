package ar.edu.utn.frc.tup.piii.services.game.state;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameStateSnapshotRepository;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PersistedGameSnapshotServiceTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameSnapshotService gameSnapshotService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameStateSnapshotRepository gameStateSnapshotRepository;

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
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldPersistSnapshotAndRecoverLatestState() {
        UUID gameId = createGame();

        GameStateDto versionOne = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.DRAW,
                1,
                1,
                null,
                List.of(GameActionType.DRAW_CARD),
                Instant.parse("2026-05-17T21:00:00Z"));

        GameStateDto versionTwo = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                2,
                null,
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-17T21:01:00Z"));

        gameSnapshotService.saveSnapshot(gameId, 1, versionOne, null);
        gameSnapshotService.saveSnapshot(gameId, 2, versionTwo, null);

        Optional<GameStateDto> latest = gameSnapshotService.findLatestVisibleState(gameId, null);

        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().stateVersion()).isEqualTo(2);
        assertThat(latest.orElseThrow().turn().currentPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void shouldGenerateChecksumWhenSavingSnapshot() {
        UUID gameId = createGame();

        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.DRAW,
                1,
                1,
                null,
                List.of(GameActionType.DRAW_CARD),
                Instant.parse("2026-05-17T21:00:00Z"));

        gameSnapshotService.saveSnapshot(gameId, 1, state, null);

        GameStateSnapshot saved = gameStateSnapshotRepository.findFirstByGame_IdOrderByVersionDesc(gameId)
                .orElseThrow();

        assertThat(saved.getChecksum()).isNotBlank();
        assertThat(saved.getChecksum()).hasSize(64);
    }

    @Test
    void shouldPreserveRicherVisibleStateFieldsWhenReloadingSnapshot() {
        UUID gameId = createGame();
        UUID playerOne = UUID.randomUUID();
        UUID playerTwo = UUID.randomUUID();
        UUID pokemonInPlayId = UUID.randomUUID();
        UUID processedActionId = UUID.randomUUID();
        UUID targetCardId = UUID.randomUUID();

        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                4,
                4,
                playerOne,
                List.of(playerOne, playerTwo),
                true,
                true,
                false,
                java.util.Map.of(playerOne, 2, playerTwo, 1),
                java.util.Map.of(playerOne, List.of(SpecialConditionType.BURNED), playerTwo, List.of()),
                4,
                playerOne,
                java.util.Map.of(pokemonInPlayId, 3),
                java.util.Map.of(playerOne, List.of(targetCardId)),
                java.util.Map.of(playerOne, java.util.Set.of(UUID.randomUUID())),
                java.util.Set.of(processedActionId),
                java.util.Map.of(targetCardId, "ACTIVE", pokemonInPlayId, "ACTIVE"),
                java.util.Map.of(targetCardId, playerOne, pokemonInPlayId, playerOne),
                List.of(GameActionType.END_TURN, GameActionType.DECLARE_ATTACK),
                Instant.parse("2026-05-24T22:00:00Z"));

        gameSnapshotService.saveSnapshot(gameId, 4, state, null);

        GameStateDto loaded = gameSnapshotService.findLatestVisibleState(gameId, null).orElseThrow();

        assertThat(loaded.turn().energyAttachedThisTurn()).isTrue();
        assertThat(loaded.turn().supporterPlayedThisTurn()).isTrue();
        assertThat(GameStateTestFactory.benchCountByPlayer(loaded)).containsEntry(playerOne, 2).containsEntry(playerTwo, 1);
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(loaded).get(playerOne)).containsExactly(SpecialConditionType.BURNED);
        assertThat(loaded.turn().playerWhoWentFirstId()).isEqualTo(playerOne);
        assertThat(loaded.board().enteredPlayTurnByPokemonInPlayId()).containsEntry(pokemonInPlayId, 3);
        assertThat(GameStateTestFactory.cardsInHandByPlayer(loaded).get(playerOne)).containsExactly(targetCardId);
        assertThat(loaded.actions().processedClientActionIds()).containsExactly(processedActionId);
        assertThat(loaded.board().zoneByCardReferenceId()).containsEntry(targetCardId, CardZone.ACTIVE).containsEntry(pokemonInPlayId, CardZone.ACTIVE);
        assertThat(loaded.board().ownerByCardReferenceId()).containsEntry(targetCardId, playerOne).containsEntry(pokemonInPlayId, playerOne);
        assertThat(loaded.actions().availableActions()).containsExactly(GameActionType.END_TURN, GameActionType.DECLARE_ATTACK);
    }

    @Test
    void shouldReturnEmptyWhenSnapshotDoesNotExist() {
        Optional<GameStateDto> latest = gameSnapshotService.findLatestVisibleState(UUID.randomUUID(), null);

        assertThat(latest).isEmpty();
    }

    @Test
    void shouldReturnLatestVisibleStateForParticipantViewer() {
        UUID gameId = createGame();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:00:00Z");

        insertUser(userId, "snapshotviewer", now);
        insertDeck(deckId, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                2,
                userId,
                List.of(userId),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-17T21:01:00Z"));

        gameSnapshotService.saveSnapshot(gameId, 2, state, null);

        Optional<GameStateDto> latest = gameSnapshotService.findLatestVisibleState(gameId, userId);

        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().turn().activePlayerId()).isEqualTo(userId);
    }

    @Test
    void shouldRejectVisibleStateLookupForNonParticipantViewer() {
        UUID gameId = createGame();
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-17T21:00:00Z");

        insertUser(userId, "snapshotowner", now);
        insertDeck(deckId, userId, now);
        insertParticipant(UUID.randomUUID(), gameId, userId, deckId, now);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameSnapshotService.findLatestVisibleState(gameId, UUID.randomUUID());
            }
        })
                .isInstanceOf(ForbiddenActionException.class);
    }

    private UUID createGame() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        return gameRepository.saveAndFlush(game).getId();
    }

    private void insertUser(UUID userId, String username, Instant now) {
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

    private void insertDeck(UUID deckId, UUID ownerUserId, Instant now) {
        jdbcTemplate.update(
                "insert into decks (id, owner_user_id, name, format, is_valid, is_active, validation_errors, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                deckId,
                ownerUserId,
                "Deck " + deckId,
                "XY1_UNLIMITED",
                true,
                true,
                null,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertParticipant(UUID participantId, UUID gameId, UUID userId, UUID deckId, Instant now) {
        jdbcTemplate.update(
                "insert into game_participants (id, game_id, user_id, deck_id, player_order, is_connected, last_seen_at, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                participantId,
                gameId,
                userId,
                deckId,
                1,
                true,
                Timestamp.from(now),
                Timestamp.from(now));
    }
}
