package ar.edu.utn.frc.tup.piii.services.matchhistory.impl;

import ar.edu.utn.frc.tup.piii.dtos.common.PageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchHistoryFilter;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchResult;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.PlayerStatsDto;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryProjection;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryResultProjection;
import ar.edu.utn.frc.tup.piii.services.matchhistory.MatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchHistoryServiceImpl implements MatchHistoryService {

    private static final int WIN_RATE_SCALE = 2;
    private static final int PERCENTAGE_MULTIPLIER = 100;
    private static final int LAST_7_DAYS = 7;
    private static final int LAST_30_DAYS = 30;

    private final GameRepository gameRepository;

    @Override
    @Transactional(readOnly = true)
    public PlayerStatsDto getPlayerStats(UUID userId) {
        List<MatchHistoryResultProjection> matches =
                gameRepository.findFinishedMatchResultsByParticipantUserId(userId, GameStatus.FINISHED);

        int totalMatches = matches.size();
        int wins = 0;
        int currentStreak = 0;
        boolean streakStillActive = true;

        for (MatchHistoryResultProjection match : matches) {
            boolean matchWasWon = userId.equals(match.getWinnerPlayerId());

            if (matchWasWon) {
                wins++;

                if (streakStillActive) {
                    currentStreak++;
                }
            } else {
                streakStillActive = false;
            }
        }

        int losses = totalMatches - wins;
        double winRate = calculateWinRate(wins, totalMatches);

        return new PlayerStatsDto(totalMatches, wins, losses, winRate, currentStreak);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<MatchHistoryDto> getMatchHistory(
            UUID userId,
            MatchHistoryFilter filter,
            Pageable pageable) {
        MatchHistoryFilter resolvedFilter = filter == null ? MatchHistoryFilter.ALL : filter;
        boolean includeWins = resolvedFilter != MatchHistoryFilter.LOSSES;
        boolean includeLosses = resolvedFilter != MatchHistoryFilter.WINS;
        Instant finishedFrom = resolveFinishedFrom(resolvedFilter);

        Page<MatchHistoryDto> history = gameRepository.findMatchHistoryByParticipantUserId(
                        userId,
                        GameStatus.FINISHED,
                        includeWins,
                        includeLosses,
                        finishedFrom,
                        pageable)
                .map(match -> toDto(match, userId));

        return PageResponseDto.from(history);
    }

    private MatchHistoryDto toDto(MatchHistoryProjection match, UUID userId) {
        return new MatchHistoryDto(
                match.getMatchId(),
                resolveResult(match.getWinnerPlayerId(), userId),
                match.getOpponentName(),
                match.getDate(),
                match.getTurnsPlayed());
    }

    private MatchResult resolveResult(UUID winnerPlayerId, UUID userId) {
        if (userId.equals(winnerPlayerId)) {
            return MatchResult.VICTORIA;
        }

        return MatchResult.DERROTA;
    }

    private double calculateWinRate(int wins, int totalMatches) {
        if (totalMatches == 0) {
            return 0.0;
        }

        BigDecimal rate = BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(PERCENTAGE_MULTIPLIER))
                .divide(BigDecimal.valueOf(totalMatches), WIN_RATE_SCALE, RoundingMode.HALF_UP);

        return rate.doubleValue();
    }

    private Instant resolveFinishedFrom(MatchHistoryFilter filter) {
        if (filter == MatchHistoryFilter.LAST_7_DAYS) {
            return Instant.now().minus(Duration.ofDays(LAST_7_DAYS));
        }

        if (filter == MatchHistoryFilter.LAST_30_DAYS) {
            return Instant.now().minus(Duration.ofDays(LAST_30_DAYS));
        }

        return null;
    }
}
