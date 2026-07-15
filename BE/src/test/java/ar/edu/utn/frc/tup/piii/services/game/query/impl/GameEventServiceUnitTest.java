package ar.edu.utn.frc.tup.piii.services.game.query.impl;



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
import ar.edu.utn.frc.tup.piii.repositories.GameEventAccessRepository;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.mappers.GameEventMapper;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.GameEventServiceImpl;
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
class GameEventServiceUnitTest {

    @Mock
    private GameEventAccessRepository gameEventRepository;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameEventMapper gameEventMapper;

    @Test
    void shouldPersistPublicEventThroughPort() {
        GameEventService gameEventService = new GameEventServiceImpl(gameEventRepository, gameLookupService, gameEventMapper);
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        GameEvent savedEvent = new GameEvent();
        savedEvent.setGame(game);
        GameEventDto dto = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.GAME_STARTED, 3, false, null, Map.of("phase", "DRAW"));
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);
        when(gameEventRepository.save(org.mockito.ArgumentMatchers.any(GameEvent.class))).thenReturn(savedEvent);
        when(gameEventMapper.toDto(savedEvent)).thenReturn(dto);

        GameEventDto result = gameEventService.recordPublicEvent(gameId, GameEventType.GAME_STARTED, 3, Map.of("phase", "DRAW"));

        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
        verify(gameEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getGame()).isSameAs(game);
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(GameEventType.GAME_STARTED);
        assertThat(eventCaptor.getValue().getVersion()).isEqualTo(3);
        assertThat(eventCaptor.getValue().getPayload()).containsEntry("phase", "DRAW");
        assertThat(eventCaptor.getValue().getVisibleToUserId()).isNull();
        assertThat(result).isSameAs(dto);
    }

    @Test
    void shouldReturnVisibleEventsFromPortInOrder() {
        GameEventService gameEventService = new GameEventServiceImpl(gameEventRepository, gameLookupService, gameEventMapper);
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        GameEvent publicEvent = new GameEvent();
        publicEvent.setGame(game);
        publicEvent.setEventType(GameEventType.GAME_STARTED);
        publicEvent.setVersion(1);
        GameEvent privateEvent = new GameEvent();
        privateEvent.setGame(game);
        privateEvent.setEventType(GameEventType.STATE_SYNC);
        privateEvent.setVersion(2);
        privateEvent.setVisibleToUserId(viewerUserId);
        GameEventDto publicDto = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.GAME_STARTED, 1, false, null, Map.of());
        GameEventDto privateDto = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.STATE_SYNC, 2, true, null, Map.of("private", true));
        when(gameEventRepository.findVisibleEvents(gameId, viewerUserId)).thenReturn(List.of(publicEvent, privateEvent));
        when(gameEventMapper.toDto(publicEvent)).thenReturn(publicDto);
        when(gameEventMapper.toDto(privateEvent)).thenReturn(privateDto);

        List<GameEventDto> events = gameEventService.getVisibleEvents(gameId, viewerUserId);

        verify(gameLookupService).assertGameExists(gameId);
        assertThat(events).containsExactly(publicDto, privateDto);
    }
}
