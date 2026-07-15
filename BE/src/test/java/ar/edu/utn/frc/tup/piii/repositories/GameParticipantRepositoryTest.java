package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameParticipantRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameParticipantRepository gameParticipantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldRejectDuplicatePlayerOrderForSameGame() {
        UUID userOneId = UUID.randomUUID();
        UUID userTwoId = UUID.randomUUID();
        UUID deckOneId = UUID.randomUUID();
        UUID deckTwoId = UUID.randomUUID();

        insertUser(userOneId);
        insertUser(userTwoId);
        insertDeck(deckOneId, userOneId);
        insertDeck(deckTwoId, userTwoId);

        Game game = createGame();

        GameParticipant first = newParticipant(game, userOneId, deckOneId, 1);
        GameParticipant duplicateOrder = newParticipant(game, userTwoId, deckTwoId, 1);

        gameParticipantRepository.saveAndFlush(first);

        assertThatThrownBy(() -> gameParticipantRepository.saveAndFlush(duplicateOrder))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDuplicateUserForSameGame() {
        UUID userId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();

        insertUser(userId);
        insertDeck(deckId, userId);

        Game game = createGame();

        GameParticipant first = newParticipant(game, userId, deckId, 1);
        GameParticipant duplicateUser = newParticipant(game, userId, deckId, 2);

        gameParticipantRepository.saveAndFlush(first);

        assertThatThrownBy(() -> gameParticipantRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Game createGame() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        return gameRepository.saveAndFlush(game);
    }

    private GameParticipant newParticipant(Game game, UUID userId, UUID deckId, int playerOrder) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setDeckId(deckId);
        participant.setPlayerOrder(playerOrder);
        return participant;
    }

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-05-17T19:00:00Z");
        jdbcTemplate.update(
                "insert into users (id, email, username, password_hash, role, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                userId,
                userId + "@example.com",
                "user" + userId.toString().substring(0, 8),
                "hash",
                "USER",
                "ACTIVE",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private void insertDeck(UUID deckId, UUID ownerUserId) {
        Instant now = Instant.parse("2026-05-17T19:00:00Z");
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
}
