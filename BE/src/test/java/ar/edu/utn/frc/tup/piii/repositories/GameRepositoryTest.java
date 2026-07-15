package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.configs.LocalTestingDeckSeeder;
import ar.edu.utn.frc.tup.piii.support.PostgreSqlDockerTestBase;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.entities.Game;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class GameRepositoryTest extends PostgreSqlDockerTestBase {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LocalTestingDeckSeeder localTestingDeckSeeder;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from game_participants");
        jdbcTemplate.update("delete from game_card_instances");
        jdbcTemplate.update("delete from games");
        jdbcTemplate.update("delete from deck_cards");
        jdbcTemplate.update("delete from decks");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldApplyDefaultValuesWhenSavingMinimalGame() {
        Game game = new Game();

        Game saved = gameRepository.saveAndFlush(game);

        assertThat(saved.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(saved.getTurnNumber()).isEqualTo(0);
        assertThat(saved.getStateVersion()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldRejectNegativeTurnNumber() {
        Game game = new Game();
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnNumber(-1);
        game.setStateVersion(0);

        assertThatThrownBy(() -> gameRepository.saveAndFlush(game))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
