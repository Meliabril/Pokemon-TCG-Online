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
import ar.edu.utn.frc.tup.piii.dtos.game.GameDetailDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.GameDetailMapper;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.GameQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceImplTest {

    @Mock
    private GameDataService gameDataService;

    @Mock
    private GameDetailMapper gameDetailMapper;

    @InjectMocks
    private GameQueryServiceImpl gameQueryService;

    @Test
    void shouldReturnMappedGameDetailWhenGameExists() {
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        GameDetailDto dto = new GameDetailDto(gameId, null, null, 0, 0, null, null, null, null, null, null, null, null, null, List.of());
        when(gameDataService.getRequiredGameDetail(gameId)).thenReturn(game);
        when(gameDetailMapper.toDto(game)).thenReturn(dto);

        GameDetailDto result = gameQueryService.getById(gameId);

        assertThat(result).isSameAs(dto);
        verify(gameDetailMapper).toDto(game);
    }

    @Test
    void shouldThrowNotFoundWhenGameDetailDoesNotExist() {
        UUID gameId = UUID.randomUUID();
        when(gameDataService.getRequiredGameDetail(gameId))
                .thenThrow(new ResourceNotFoundException("Game with id " + gameId + " was not found"));

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameQueryService.getById(gameId);
            }
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }
}
