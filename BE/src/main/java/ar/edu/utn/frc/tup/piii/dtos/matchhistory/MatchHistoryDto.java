package ar.edu.utn.frc.tup.piii.dtos.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.enums.MatchResult;

import java.time.Instant;
import java.util.UUID;

public record MatchHistoryDto(
        UUID matchId,
        MatchResult result,
        String opponentName,
        Instant date,
        int turnsPlayed) {
}
