package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class V20__persona_5_evolution_stack_turn_metadata extends BaseJavaMigration {

    private static final int BASE_STACK_ORDER = 0;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        execute(connection, "alter table pokemon_evolution_stack add column created_at_turn integer");
        execute(
                connection,
                """
                update pokemon_evolution_stack
                   set created_at_turn = (
                       select pokemon_in_play.entered_play_turn
                         from pokemon_in_play
                        where pokemon_in_play.id = pokemon_evolution_stack.pokemon_in_play_id
                   )
                 where created_at_turn is null
                """);
        execute(connection, "update pokemon_evolution_stack set created_at_turn = 0 where created_at_turn is null");
        insertMissingBaseStacks(connection);
        execute(connection, "alter table pokemon_evolution_stack alter column created_at_turn set not null");
        execute(
                connection,
                "alter table pokemon_evolution_stack add constraint ck_pokemon_evolution_stack_created_at_turn check (created_at_turn >= 0)");
        execute(
                connection,
                "alter table pokemon_evolution_stack add constraint ck_pokemon_evolution_stack_order_max check (stack_order <= 2)");
        execute(
                connection,
                "create unique index ux_pokemon_evolution_stack_card_instance on pokemon_evolution_stack (game_card_instance_id)");
        execute(
                connection,
                "create index ix_pokemon_evolution_stack_pokemon_order_desc on pokemon_evolution_stack (pokemon_in_play_id, stack_order desc)");
    }

    private void insertMissingBaseStacks(Connection connection) throws SQLException {
        List<BaseStackBackfill> missingStacks = findMissingBaseStacks(connection);
        if (missingStacks.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                """
                insert into pokemon_evolution_stack
                    (id, pokemon_in_play_id, game_card_instance_id, stack_order, created_at_turn, created_at)
                values (?, ?, ?, ?, ?, ?)
                """)) {
            for (BaseStackBackfill missingStack : missingStacks) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, missingStack.pokemonInPlayId());
                statement.setObject(3, missingStack.gameCardInstanceId());
                statement.setInt(4, BASE_STACK_ORDER);
                statement.setInt(5, missingStack.enteredPlayTurn());
                statement.setTimestamp(6, Timestamp.from(Instant.now()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<BaseStackBackfill> findMissingBaseStacks(Connection connection) throws SQLException {
        List<BaseStackBackfill> missingStacks = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                """
                select pokemon_in_play.id,
                       pokemon_in_play.active_card_instance_id,
                       pokemon_in_play.entered_play_turn
                  from pokemon_in_play
                 where pokemon_in_play.active_card_instance_id is not null
                   and not exists (
                       select 1
                         from pokemon_evolution_stack
                        where pokemon_evolution_stack.pokemon_in_play_id = pokemon_in_play.id
                          and pokemon_evolution_stack.stack_order = 0
                   )
                   and not exists (
                       select 1
                         from pokemon_evolution_stack
                        where pokemon_evolution_stack.game_card_instance_id = pokemon_in_play.active_card_instance_id
                   )
                """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                UUID pokemonInPlayId = resultSet.getObject(1, UUID.class);
                UUID gameCardInstanceId = resultSet.getObject(2, UUID.class);
                int enteredPlayTurn = resultSet.getInt(3);
                missingStacks.add(new BaseStackBackfill(pokemonInPlayId, gameCardInstanceId, enteredPlayTurn));
            }
        }

        return missingStacks;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record BaseStackBackfill(UUID pokemonInPlayId, UUID gameCardInstanceId, int enteredPlayTurn) {
    }
}
