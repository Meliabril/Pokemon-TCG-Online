package ar.edu.utn.frc.tup.piii.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface MatchHistoryProjection {

    UUID getMatchId();

    UUID getWinnerPlayerId();

    String getOpponentName();

    Instant getDate();

    Integer getTurnsPlayed();
}
