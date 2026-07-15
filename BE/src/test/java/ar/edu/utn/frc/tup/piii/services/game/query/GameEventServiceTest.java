package ar.edu.utn.frc.tup.piii.services.game.query;



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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameEventServiceTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameEventService gameEventService;

    @Autowired
    private GameRepository gameRepository;

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
    void shouldReturnPublicAndPrivateEventsVisibleToViewerOrderedByVersion() {
        UUID gameId = createGame();
        UUID viewerUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        gameEventService.recordPublicEvent(gameId, GameEventType.GAME_STARTED, 1, Map.of("phase", "DRAW"));
        gameEventService.recordPrivateEvent(gameId, GameEventType.STATE_SYNC, 2, Map.of("private", true), viewerUserId);
        gameEventService.recordPrivateEvent(gameId, GameEventType.STATE_SYNC, 3, Map.of("hidden", true), otherUserId);

        List<GameEventDto> events = gameEventService.getVisibleEvents(gameId, viewerUserId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventType()).isEqualTo(GameEventType.GAME_STARTED);
        assertThat(events.get(0).privateEvent()).isFalse();
        assertThat(events.get(1).eventType()).isEqualTo(GameEventType.STATE_SYNC);
        assertThat(events.get(1).privateEvent()).isTrue();
    }

    @Test
    void shouldThrowNotFoundWhenGameDoesNotExist() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameEventService.getVisibleEvents(UUID.randomUUID(), UUID.randomUUID());
            }
        })
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnOnlyPublicEventsWhenViewerIsNull() {
        UUID gameId = createGame();
        UUID privateViewerId = UUID.randomUUID();

        gameEventService.recordPublicEvent(gameId, GameEventType.GAME_STARTED, 1, Map.of("phase", "DRAW"));
        gameEventService.recordPrivateEvent(gameId, GameEventType.STATE_SYNC, 2, Map.of("private", true), privateViewerId);

        List<GameEventDto> events = gameEventService.getVisibleEvents(gameId, null);

        assertThat(events).hasSize(1);
        assertThat(events.getFirst().eventType()).isEqualTo(GameEventType.GAME_STARTED);
    }

    private UUID createGame() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(1);
        game.setStateVersion(0);
        return gameRepository.saveAndFlush(game).getId();
    }
}
