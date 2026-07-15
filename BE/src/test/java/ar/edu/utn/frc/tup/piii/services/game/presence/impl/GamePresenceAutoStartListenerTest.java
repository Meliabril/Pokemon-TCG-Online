package ar.edu.utn.frc.tup.piii.services.game.presence.impl;



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
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.GamePresenceAutoStartListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GamePresenceAutoStartListenerTest {

    @Mock
    private GameParticipantRepository gameParticipantRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameService gameService;

    @Test
    void shouldAutoStartWaitingGameWhenBothPlayersAreOnline() {
        UUID gameId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant firstParticipant = participant(game, firstUserId, 1, true);
        GameParticipant secondParticipant = participant(game, secondUserId, 2, true);
        GamePresenceAutoStartListener listener = new GamePresenceAutoStartListener(
                gameParticipantRepository,
                gameRepository,
                gameService);

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantRepository.findByGame_IdOrderByPlayerOrderAsc(gameId))
                .thenReturn(List.of(firstParticipant, secondParticipant));

        listener.onParticipantConnected(new GameParticipantConnectedEvent(gameId, secondUserId));

        ArgumentCaptor<GameActionRequestDto> requestCaptor = ArgumentCaptor.forClass(GameActionRequestDto.class);
        verify(gameService).executeAction(org.mockito.ArgumentMatchers.eq(gameId), org.mockito.ArgumentMatchers.eq(firstUserId), requestCaptor.capture());
        assertThat(requestCaptor.getValue().actionType()).isEqualTo(GameActionType.START_GAME);
        assertThat(requestCaptor.getValue().expectedStateVersion()).isZero();
    }

    @Test
    void shouldNotAutoStartWhenOpponentIsStillOffline() {
        UUID gameId = UUID.randomUUID();
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        Game game = waitingGame(gameId);
        GameParticipant firstParticipant = participant(game, firstUserId, 1, true);
        GameParticipant secondParticipant = participant(game, secondUserId, 2, false);
        GamePresenceAutoStartListener listener = new GamePresenceAutoStartListener(
                gameParticipantRepository,
                gameRepository,
                gameService);

        when(gameRepository.findDetailByIdForUpdate(gameId)).thenReturn(Optional.of(game));
        when(gameParticipantRepository.findByGame_IdOrderByPlayerOrderAsc(gameId))
                .thenReturn(List.of(firstParticipant, secondParticipant));

        listener.onParticipantConnected(new GameParticipantConnectedEvent(gameId, secondUserId));

        verify(gameService, never()).executeAction(any(), any(), any());
    }

    private Game waitingGame(UUID gameId) {
        Game game = new Game();
        game.setId(gameId);
        game.setStatus(GameStatus.WAITING);
        game.setTurnNumber(0);
        game.setStateVersion(0);
        return game;
    }

    private GameParticipant participant(Game game, UUID userId, int playerOrder, boolean connected) {
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUserId(userId);
        participant.setDeckId(UUID.randomUUID());
        participant.setPlayerOrder(playerOrder);
        participant.setConnected(connected);
        return participant;
    }
}
