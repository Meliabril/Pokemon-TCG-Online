package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameCardInstanceRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameCardInstanceRepository gameCardInstanceRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from attack_costs");
        jdbcTemplate.update("delete from attacks");
        jdbcTemplate.update("delete from card_weaknesses");
        jdbcTemplate.update("delete from card_resistances");
        jdbcTemplate.update("delete from cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldReturnZoneCardsOrderedByZonePosition() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID firstCardId = UUID.randomUUID();
        UUID secondCardId = UUID.randomUUID();

        insertUser(userId);
        insertCard(firstCardId);
        insertCard(secondCardId);
        Game game = insertGame(gameId);

        GameCardInstance second = new GameCardInstance();
        second.setGame(game);
        second.setOwnerUserId(userId);
        second.setCardId(secondCardId);
        second.setZone(CardZone.HAND);
        second.setZonePosition(2);

        GameCardInstance first = new GameCardInstance();
        first.setGame(game);
        first.setOwnerUserId(userId);
        first.setCardId(firstCardId);
        first.setZone(CardZone.HAND);
        first.setZonePosition(1);

        gameCardInstanceRepository.save(second);
        gameCardInstanceRepository.save(first);

        List<GameCardInstance> cards = gameCardInstanceRepository
                .findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, userId, CardZone.HAND);

        assertThat(cards)
                .hasSize(2)
                .extracting(GameCardInstance::getZonePosition)
                .containsExactly(1, 2);
    }

    @Test
    void shouldRejectDuplicateZonePositionForSameGameOwnerAndZone() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID firstCardId = UUID.randomUUID();
        UUID secondCardId = UUID.randomUUID();

        insertUser(userId);
        insertCard(firstCardId);
        insertCard(secondCardId);
        Game game = insertGame(gameId);

        GameCardInstance first = new GameCardInstance();
        first.setGame(game);
        first.setOwnerUserId(userId);
        first.setCardId(firstCardId);
        first.setZone(CardZone.HAND);
        first.setZonePosition(1);

        GameCardInstance duplicate = new GameCardInstance();
        duplicate.setGame(game);
        duplicate.setOwnerUserId(userId);
        duplicate.setCardId(secondCardId);
        duplicate.setZone(CardZone.HAND);
        duplicate.setZonePosition(1);

        gameCardInstanceRepository.saveAndFlush(first);

        assertThatThrownBy(() -> gameCardInstanceRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameZonePositionWhenZoneDiffers() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID handCardId = UUID.randomUUID();
        UUID deckCardId = UUID.randomUUID();

        insertUser(userId);
        insertCard(handCardId);
        insertCard(deckCardId);
        Game game = insertGame(gameId);

        GameCardInstance handCard = new GameCardInstance();
        handCard.setGame(game);
        handCard.setOwnerUserId(userId);
        handCard.setCardId(handCardId);
        handCard.setZone(CardZone.HAND);
        handCard.setZonePosition(1);

        GameCardInstance deckCard = new GameCardInstance();
        deckCard.setGame(game);
        deckCard.setOwnerUserId(userId);
        deckCard.setCardId(deckCardId);
        deckCard.setZone(CardZone.DECK);
        deckCard.setZonePosition(1);

        gameCardInstanceRepository.saveAndFlush(handCard);
        gameCardInstanceRepository.saveAndFlush(deckCard);

        List<GameCardInstance> handCards = gameCardInstanceRepository
                .findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, userId, CardZone.HAND);
        List<GameCardInstance> deckCards = gameCardInstanceRepository
                .findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, userId, CardZone.DECK);

        assertThat(handCards).hasSize(1);
        assertThat(deckCards).hasSize(1);
    }

    @Test
    void shouldDefaultFaceDownToFalseWhenNotProvided() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        insertUser(userId);
        insertCard(cardId);
        Game game = insertGame(gameId);

        GameCardInstance instance = new GameCardInstance();
        instance.setGame(game);
        instance.setOwnerUserId(userId);
        instance.setCardId(cardId);
        instance.setZone(CardZone.PRIZE);
        instance.setZonePosition(1);

        GameCardInstance saved = gameCardInstanceRepository.saveAndFlush(instance);

        assertThat(saved.getFaceDown()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldExposeSwappedZonesAfterSwapActiveWithBench() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID benchCardId = UUID.randomUUID();

        insertUser(userId);
        insertCard(activeCardId);
        insertCard(benchCardId);
        Game game = insertGame(gameId);

        GameCardInstance activeInstance = new GameCardInstance();
        activeInstance.setGame(game);
        activeInstance.setOwnerUserId(userId);
        activeInstance.setCardId(activeCardId);
        activeInstance.setZone(CardZone.ACTIVE);
        activeInstance.setZonePosition(0);

        GameCardInstance benchInstance = new GameCardInstance();
        benchInstance.setGame(game);
        benchInstance.setOwnerUserId(userId);
        benchInstance.setCardId(benchCardId);
        benchInstance.setZone(CardZone.BENCH);
        benchInstance.setZonePosition(1);

        gameCardInstanceRepository.saveAndFlush(benchInstance);
        GameCardInstance savedActive = gameCardInstanceRepository.saveAndFlush(activeInstance);

        int updated = gameCardInstanceRepository.swapActiveWithBench(
                activeInstance.getId(), benchInstance.getId(), 1);
        assertThat(updated).isEqualTo(2);

        List<GameCardInstance> activeCards = gameCardInstanceRepository
                .findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, userId, CardZone.ACTIVE);
        List<GameCardInstance> benchCards = gameCardInstanceRepository
                .findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, userId, CardZone.BENCH);

        assertThat(activeCards)
                .extracting(GameCardInstance::getId)
                .containsExactly(benchInstance.getId());
        assertThat(benchCards)
                .extracting(GameCardInstance::getId)
                .containsExactly(activeInstance.getId());
        assertThat(activeCards.getFirst().getZonePosition()).isZero();
        assertThat(benchCards.getFirst().getZonePosition()).isOne();
        assertThat(savedActive.getId()).isEqualTo(activeInstance.getId());
    }

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-05-17T18:00:00Z");
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

    private void insertCard(UUID cardId) {
        Instant now = Instant.parse("2026-05-17T18:00:00Z");
        jdbcTemplate.update(
                "insert into cards (id, external_id, set_code, set_name, number, name, supertype, category, raw_json, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                cardId,
                cardId.toString(),
                "xy1",
                "XY",
                "1",
                "Card " + cardId,
                "POKEMON",
                "BASIC_POKEMON",
                "{}",
                Timestamp.from(now),
                Timestamp.from(now));
    }

    private Game insertGame(UUID gameId) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        return gameRepository.saveAndFlush(game);
    }
}
