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
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogReadRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogWriteRepository;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionLogEntryDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.mappers.GameActionLogMapper;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameActionLogCommandServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.GameActionLogQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameActionLogServicesTest {

    @Mock
    private GameActionLogWriteRepository gameActionLogWriteRepository;

    @Mock
    private GameActionLogReadRepository gameActionLogReadRepository;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameActionLogMapper gameActionLogMapper;

    @Test
    void shouldPersistAcceptedActionThroughCommandPort() {
        GameActionLogCommandServiceImpl commandService = new GameActionLogCommandServiceImpl(gameActionLogWriteRepository, gameLookupService, gameActionLogMapper);
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID clientActionId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        GameActionLog savedLog = new GameActionLog();
        GameActionLogEntryDto dto = new GameActionLogEntryDto(UUID.randomUUID(), actorUserId, GameActionType.DRAW_CARD, Map.of("step", 1), Map.of("drawn", 1), 2, clientActionId, null);
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameActionLogWriteRepository.save(org.mockito.ArgumentMatchers.any(GameActionLog.class))).thenReturn(savedLog);
        when(gameActionLogMapper.toDto(savedLog)).thenReturn(dto);

        GameActionLogEntryDto result = commandService.recordAcceptedAction(gameId, actorUserId, GameActionType.DRAW_CARD, Map.of("step", 1), Map.of("drawn", 1), 2, clientActionId);

        ArgumentCaptor<GameActionLog> logCaptor = ArgumentCaptor.forClass(GameActionLog.class);
        verify(gameActionLogWriteRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getGame()).isSameAs(game);
        assertThat(logCaptor.getValue().getActionType()).isEqualTo(GameActionType.DRAW_CARD);
        assertThat(logCaptor.getValue().getPayload()).containsEntry("step", 1);
        assertThat(logCaptor.getValue().getResult()).containsEntry("drawn", 1);
        assertThat(result).isSameAs(dto);
    }

    @Test
    void shouldReadOrderedHistoryThroughQueryPort() {
        GameActionLogQueryServiceImpl queryService = new GameActionLogQueryServiceImpl(gameActionLogReadRepository, gameLookupService, gameActionLogMapper);
        UUID gameId = UUID.randomUUID();
        GameActionLog firstLog = new GameActionLog();
        firstLog.setVersion(1);
        GameActionLog secondLog = new GameActionLog();
        secondLog.setVersion(2);
        GameActionLogEntryDto firstDto = new GameActionLogEntryDto(UUID.randomUUID(), UUID.randomUUID(), GameActionType.DRAW_CARD, Map.of(), null, 1, UUID.randomUUID(), null);
        GameActionLogEntryDto secondDto = new GameActionLogEntryDto(UUID.randomUUID(), UUID.randomUUID(), GameActionType.END_TURN, Map.of(), null, 2, UUID.randomUUID(), null);
        when(gameActionLogReadRepository.findHistory(gameId)).thenReturn(List.of(firstLog, secondLog));
        when(gameActionLogMapper.toDto(firstLog)).thenReturn(firstDto);
        when(gameActionLogMapper.toDto(secondLog)).thenReturn(secondDto);

        List<GameActionLogEntryDto> history = queryService.getHistory(gameId);

        verify(gameLookupService).assertGameExists(gameId);
        assertThat(history).containsExactly(firstDto, secondDto);
    }
}
