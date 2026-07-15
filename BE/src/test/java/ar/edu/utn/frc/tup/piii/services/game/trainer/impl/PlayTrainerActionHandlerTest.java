package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;




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
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayTrainerActionHandlerTest {

    @Test
    void shouldDeclarePlayTrainerAsSupportedAction() {
        TrainerEffectService trainerEffectService = mock(TrainerEffectService.class);
        PlayTrainerActionHandler handler = new PlayTrainerActionHandler(trainerEffectService);

        assertThat(handler.supportedAction()).isEqualTo(GameActionType.PLAY_TRAINER);
    }

    @Test
    void shouldDelegateExecutionToTrainerEffectService() {
        TrainerEffectService trainerEffectService = mock(TrainerEffectService.class);
        PlayTrainerActionHandler handler = new PlayTrainerActionHandler(trainerEffectService);
        GameActionContext context = context();
        GameActionExecutionResult expectedResult = new GameActionExecutionResult(context.currentState(), List.of());
        when(trainerEffectService.executeTrainer(context)).thenReturn(expectedResult);

        GameActionExecutionResult result = handler.execute(context);

        assertThat(result).isEqualTo(expectedResult);
        verify(trainerEffectService).executeTrainer(context);
    }

    private GameActionContext context() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                4,
                actorUserId,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                4,
                Map.<String, Object>of("cardId", UUID.randomUUID().toString()));
        return new GameActionContext(gameId, actorUserId, request, state);
    }
}
