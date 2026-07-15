package ar.edu.utn.frc.tup.piii.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface MatchHistoryResultProjection {

    UUID getMatchId();

    UUID getWinnerPlayerId();

    Instant getDate();
}
