package ar.edu.utn.frc.tup.piii.services.matchhistory;

import ar.edu.utn.frc.tup.piii.dtos.common.PageResponseDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchHistoryFilter;
import ar.edu.utn.frc.tup.piii.dtos.enums.MatchResult;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.MatchHistoryDto;
import ar.edu.utn.frc.tup.piii.dtos.matchhistory.PlayerStatsDto;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryProjection;
import ar.edu.utn.frc.tup.piii.repositories.projections.MatchHistoryResultProjection;
import ar.edu.utn.frc.tup.piii.services.matchhistory.impl.MatchHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchHistoryServiceImplTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private MatchHistoryServiceImpl matchHistoryService;

    @Test
    void shouldReturnEmptyStatsWhenPlayerHasNoFinishedMatches() {
        UUID userId = UUID.randomUUID();
        when(gameRepository.findFinishedMatchResultsByParticipantUserId(userId, GameStatus.FINISHED))
                .thenReturn(List.of());

        PlayerStatsDto stats = matchHistoryService.getPlayerStats(userId);

        assertThat(stats.totalMatches()).isZero();
        assertThat(stats.wins()).isZero();
        assertThat(stats.losses()).isZero();
        assertThat(stats.winRate()).isZero();
        assertThat(stats.currentStreak()).isZero();
    }

    @Test
    void shouldCalculateStatsAndCurrentWinStreakFromMostRecentMatches() {
        UUID userId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        when(gameRepository.findFinishedMatchResultsByParticipantUserId(userId, GameStatus.FINISHED))
                .thenReturn(List.of(
                        result(userId),
                        result(userId),
                        result(opponentId),
                        result(userId)
                ));

        PlayerStatsDto stats = matchHistoryService.getPlayerStats(userId);

        assertThat(stats.totalMatches()).isEqualTo(4);
        assertThat(stats.wins()).isEqualTo(3);
        assertThat(stats.losses()).isEqualTo(1);
        assertThat(stats.winRate()).isEqualTo(75.0);
        assertThat(stats.currentStreak()).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroCurrentStreakWhenLatestMatchWasLost() {
        UUID userId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        when(gameRepository.findFinishedMatchResultsByParticipantUserId(userId, GameStatus.FINISHED))
                .thenReturn(List.of(
                        result(opponentId),
                        result(userId),
                        result(userId)
                ));

        PlayerStatsDto stats = matchHistoryService.getPlayerStats(userId);

        assertThat(stats.currentStreak()).isZero();
        assertThat(stats.wins()).isEqualTo(2);
        assertThat(stats.losses()).isEqualTo(1);
        assertThat(stats.winRate()).isEqualTo(66.67);
    }

    @Test
    void shouldMapPaginatedHistoryAndApplyWinsFilter() {
        UUID userId = UUID.randomUUID();
        Instant finishedAt = Instant.parse("2026-06-01T10:15:30Z");
        Pageable pageable = PageRequest.of(1, 5);
        MatchHistoryProjection match = history(UUID.randomUUID(), userId, "Ash", finishedAt, 8);
        when(gameRepository.findMatchHistoryByParticipantUserId(
                eq(userId),
                eq(GameStatus.FINISHED),
                eq(true),
                eq(false),
                eq(null),
                eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(match), pageable, 11));

        PageResponseDto<MatchHistoryDto> history =
                matchHistoryService.getMatchHistory(userId, MatchHistoryFilter.WINS, pageable);

        assertThat(history.items()).hasSize(1);
        assertThat(history.page()).isEqualTo(1);
        assertThat(history.size()).isEqualTo(5);
        assertThat(history.totalItems()).isEqualTo(11);
        assertThat(history.items().getFirst().result()).isEqualTo(MatchResult.VICTORIA);
        assertThat(history.items().getFirst().opponentName()).isEqualTo("Ash");
        assertThat(history.items().getFirst().date()).isEqualTo(finishedAt);
        assertThat(history.items().getFirst().turnsPlayed()).isEqualTo(8);
    }

    @Test
    void shouldApplyLastSevenDaysFilterWithDateCutoff() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(gameRepository.findMatchHistoryByParticipantUserId(
                eq(userId),
                eq(GameStatus.FINISHED),
                eq(true),
                eq(true),
                org.mockito.ArgumentMatchers.any(Instant.class),
                eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        matchHistoryService.getMatchHistory(userId, MatchHistoryFilter.LAST_7_DAYS, pageable);

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(gameRepository).findMatchHistoryByParticipantUserId(
                eq(userId),
                eq(GameStatus.FINISHED),
                eq(true),
                eq(true),
                cutoffCaptor.capture(),
                eq(pageable));
        assertThat(cutoffCaptor.getValue()).isBeforeOrEqualTo(Instant.now());
    }

    private MatchHistoryResultProjection result(UUID winnerPlayerId) {
        return new TestMatchHistoryResultProjection(UUID.randomUUID(), winnerPlayerId, Instant.now());
    }

    private MatchHistoryProjection history(
            UUID matchId,
            UUID winnerPlayerId,
            String opponentName,
            Instant date,
            int turnsPlayed) {
        return new TestMatchHistoryProjection(matchId, winnerPlayerId, opponentName, date, turnsPlayed);
    }

    private record TestMatchHistoryResultProjection(
            UUID matchId,
            UUID winnerPlayerId,
            Instant date) implements MatchHistoryResultProjection {

        @Override
        public UUID getMatchId() {
            return matchId;
        }

        @Override
        public UUID getWinnerPlayerId() {
            return winnerPlayerId;
        }

        @Override
        public Instant getDate() {
            return date;
        }
    }

    private record TestMatchHistoryProjection(
            UUID matchId,
            UUID winnerPlayerId,
            String opponentName,
            Instant date,
            Integer turnsPlayed) implements MatchHistoryProjection {

        @Override
        public UUID getMatchId() {
            return matchId;
        }

        @Override
        public UUID getWinnerPlayerId() {
            return winnerPlayerId;
        }

        @Override
        public String getOpponentName() {
            return opponentName;
        }

        @Override
        public Instant getDate() {
            return date;
        }

        @Override
        public Integer getTurnsPlayed() {
            return turnsPlayed;
        }
    }
}
