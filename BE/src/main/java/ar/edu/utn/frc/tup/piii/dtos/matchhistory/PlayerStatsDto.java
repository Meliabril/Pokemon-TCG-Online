package ar.edu.utn.frc.tup.piii.dtos.matchhistory;

public record PlayerStatsDto(
        int totalMatches,
        int wins,
        int losses,
        double winRate,
        int currentStreak) {
}
