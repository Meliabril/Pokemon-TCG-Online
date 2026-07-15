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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateServiceTest {

    @Mock
    private GameStateQueryService gameStateQueryService;

    @Mock
    private GameStateRestorer gameStateRestorer;

    @Test
    void shouldDelegateVisibleStateBuildingToQueryService() {
        GameStateService gameStateService = new GameStateServiceImpl(gameStateQueryService, gameStateRestorer);
        Game game = new Game();
        game.setId(UUID.randomUUID());
        GameStateDto expectedState = GameStateTestFactory.state(
                game.getId(),
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                4,
                7,
                UUID.randomUUID(),
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-24T01:30:00Z"));
        when(gameStateQueryService.buildVisibleState(game)).thenReturn(expectedState);

        GameStateDto visibleState = gameStateService.buildVisibleState(game);

        assertThat(visibleState).isSameAs(expectedState);
        verify(gameStateQueryService).buildVisibleState(game);
    }

    @Test
    void shouldDelegateSnapshotRestorationToRestorer() {
        GameStateService gameStateService = new GameStateServiceImpl(gameStateQueryService, gameStateRestorer);
        Game game = new Game();
        GameStateDto snapshot = GameStateTestFactory.state(
                UUID.randomUUID(),
                GameStatus.PAUSED,
                TurnPhase.ATTACK,
                9,
                11,
                UUID.randomUUID(),
                List.of(GameActionType.RETREAT),
                Instant.parse("2026-05-24T01:35:00Z"));

        gameStateService.restoreFromSnapshot(game, snapshot);

        verify(gameStateRestorer).restoreFromSnapshot(game, snapshot);
    }
}
