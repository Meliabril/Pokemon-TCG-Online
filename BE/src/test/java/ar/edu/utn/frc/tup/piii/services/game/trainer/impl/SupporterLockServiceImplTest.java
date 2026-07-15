package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupporterLockServiceImplTest {

    @Mock
    private GameParticipantStateService gameParticipantStateService;

    @Test
    void shouldLockSupportersForParticipantTurn() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameParticipant participant = participant(playerId, null);
        SupporterLockServiceImpl service = new SupporterLockServiceImpl(gameParticipantStateService);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)).thenReturn(Optional.of(participant));

        service.lockSupporters(gameId, playerId, 4);

        assertThat(participant.getSupporterLockedTurn()).isEqualTo(4);
        verify(gameParticipantStateService).save(participant);
    }

    @Test
    void shouldReportLockedOnlyOnMatchingTurn() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameParticipant participant = participant(playerId, 4);
        SupporterLockServiceImpl service = new SupporterLockServiceImpl(gameParticipantStateService);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)).thenReturn(Optional.of(participant));

        assertThat(service.isSupporterLocked(gameId, playerId, 4)).isTrue();
    }

    @Test
    void shouldExpireStaleLockWhenCheckedAfterLockedTurn() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameParticipant participant = participant(playerId, 4);
        SupporterLockServiceImpl service = new SupporterLockServiceImpl(gameParticipantStateService);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)).thenReturn(Optional.of(participant));

        assertThat(service.isSupporterLocked(gameId, playerId, 5)).isFalse();

        assertThat(participant.getSupporterLockedTurn()).isNull();
        verify(gameParticipantStateService).save(participant);
    }

    @Test
    void shouldExpireLockAtEndOfLockedTurn() {
        GameParticipant participant = participant(UUID.randomUUID(), 4);
        SupporterLockServiceImpl service = new SupporterLockServiceImpl(gameParticipantStateService);

        service.expireLock(participant, 4);

        assertThat(participant.getSupporterLockedTurn()).isNull();
        verify(gameParticipantStateService).save(participant);
    }

    @Test
    void shouldRejectMissingParticipantWhenLocking() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        SupporterLockServiceImpl service = new SupporterLockServiceImpl(gameParticipantStateService);
        when(gameParticipantStateService.findByGameIdAndUserId(gameId, playerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lockSupporters(gameId, playerId, 4))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Player participant was not found for supporter lock");
        verify(gameParticipantStateService, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private GameParticipant participant(UUID playerId, Integer supporterLockedTurn) {
        GameParticipant participant = new GameParticipant();
        participant.setUserId(playerId);
        participant.setSupporterLockedTurn(supporterLockedTurn);
        return participant;
    }
}
