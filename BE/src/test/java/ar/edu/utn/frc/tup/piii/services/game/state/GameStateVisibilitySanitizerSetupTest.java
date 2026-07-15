package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardActionHintsDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePlayerBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleZoneDto;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateVisibilitySanitizer;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateVisibilitySanitizerSetupTest {

    @Test
    void shouldPreservePublicOpponentSetupOccupancyWithoutRevealingCardIdentity() {
        UUID gameId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        UUID opponentActiveCardId = UUID.randomUUID();
        List<UUID> opponentBenchCardIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        GameStateDto baseState = GameStateTestFactory.state(
                gameId,
                GameStatus.SETUP,
                TurnPhase.MAIN,
                0,
                1,
                null,
                List.of(viewerId, opponentId),
                List.of(),
                Instant.parse("2026-06-22T22:00:00Z"));

        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>(baseState.players());
        players.put(opponentId, players.get(opponentId).toBuilder()
                .initialActiveCardInstanceId(opponentActiveCardId)
                .initialBenchCardInstanceIds(opponentBenchCardIds)
                .build());

        VisibleBoardDto visibleBoard = new VisibleBoardDto(
                null,
                List.of(
                        visiblePlayer(viewerId, true, false, 0),
                        visiblePlayer(opponentId, false, true, 2)),
                null,
                VisibleBoardActionHintsDto.empty());
        GameStateDto state = baseState.toBuilder()
                .players(players)
                .board(baseState.board().toBuilder().view(visibleBoard).build())
                .build();

        GameStateDto sanitized = new GameStateVisibilitySanitizer().sanitizeForViewer(state, viewerId);
        VisiblePlayerBoardDto opponent = sanitized.board().view().players().stream()
                .filter(player -> player.playerId().equals(opponentId))
                .findFirst()
                .orElseThrow();

        assertThat(opponent.setupActiveOccupied()).isTrue();
        assertThat(opponent.setupBenchOccupiedCount()).isEqualTo(2);
        assertThat(opponent.setupSelectionSubmitted()).isFalse();
        assertThat(opponent.activePokemon()).isNull();
        assertThat(opponent.benchPokemon()).isEmpty();
        assertThat(opponent.hand().cards()).isEmpty();
        assertThat(sanitized.players().get(opponentId).initialActiveCardInstanceId()).isNull();
        assertThat(sanitized.players().get(opponentId).initialBenchCardInstanceIds()).isEmpty();
    }

    private VisiblePlayerBoardDto visiblePlayer(
            UUID playerId,
            boolean local,
            boolean activeOccupied,
            int benchOccupiedCount) {
        return new VisiblePlayerBoardDto(
                playerId,
                local ? 0 : 1,
                true,
                local,
                0,
                false,
                false,
                true,
                0,
                false,
                activeOccupied,
                benchOccupiedCount,
                zone(CardZone.HAND, 7),
                zone(CardZone.DECK, 47),
                zone(CardZone.PRIZE, 6),
                zone(CardZone.DISCARD, 0),
                null,
                List.of());
    }

    private VisibleZoneDto zone(CardZone zone, int count) {
        return new VisibleZoneDto(zone, count, List.of());
    }
}
