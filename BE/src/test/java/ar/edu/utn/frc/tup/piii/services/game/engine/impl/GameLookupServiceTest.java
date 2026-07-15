package ar.edu.utn.frc.tup.piii.services.game.engine.impl;



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
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenActionException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.GameLookupServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameLookupServiceTest {

    @Mock
    private GameDataService gameDataService;

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @InjectMocks
    private GameLookupServiceImpl gameLookupService;

    @Test
    void shouldReturnRequiredGameWhenItExists() {
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        when(gameDataService.getRequiredGame(gameId)).thenReturn(game);

        Game foundGame = gameLookupService.getRequiredGame(gameId);

        assertThat(foundGame).isSameAs(game);
    }

    @Test
    void shouldThrowNotFoundWhenRequiredGameDoesNotExist() {
        UUID gameId = UUID.randomUUID();
        when(gameDataService.getRequiredGame(gameId))
                .thenThrow(new ResourceNotFoundException("Game with id " + gameId + " was not found"));

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameLookupService.getRequiredGame(gameId);
            }
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    @Test
    void shouldThrowNotFoundWhenGameDoesNotExistForParticipantCheck() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Game with id " + gameId + " was not found"))
                .when(gameDataService).assertGameExists(gameId);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameLookupService.assertParticipant(gameId, userId);
            }
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    @Test
    void shouldThrowForbiddenWhenUserIsNotParticipant() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(gameParticipantStateService.existsByGameIdAndUserId(gameId, userId)).thenReturn(false);

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameLookupService.assertParticipant(gameId, userId);
            }
        })
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void shouldThrowForbiddenWhenParticipantUserIdIsNull() {
        UUID gameId = UUID.randomUUID();

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                gameLookupService.assertParticipant(gameId, null);
            }
        })
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void shouldAllowParticipantWhenMembershipExists() {
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(gameParticipantStateService.existsByGameIdAndUserId(gameId, userId)).thenReturn(true);

        gameLookupService.assertParticipant(gameId, userId);

        verify(gameParticipantStateService).existsByGameIdAndUserId(gameId, userId);
    }
}
