package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameDetailDto(
        UUID gameId,
        GameStatus status,
        TurnPhase currentPhase,
        int turnNumber,
        int stateVersion,
        UUID activePlayerId,
        Instant turnStartedAt,
        UUID winnerPlayerId,
        String pauseReason,
        Instant startedAt,
        Instant pausedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<GameParticipantDto> participants) {

    public GameDetailDto {
        participants = participants == null ? List.of() : List.copyOf(participants);
    }
}
