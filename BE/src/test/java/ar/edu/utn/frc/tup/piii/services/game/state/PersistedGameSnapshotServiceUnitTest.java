package ar.edu.utn.frc.tup.piii.services.game.state;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameSnapshotStore;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameStateVisibilitySanitizer;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.GameSnapshotServiceImpl;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistedGameSnapshotServiceUnitTest {

    @Mock
    private GameSnapshotStore gameSnapshotStore;

    @Mock
    private GameLookupService gameLookupService;

    private final GameStateVisibilitySanitizer gameStateVisibilitySanitizer = new GameStateVisibilitySanitizer();

    @Test
    void shouldSaveSnapshotThroughPorts() {
        GameSnapshotServiceImpl service = service();
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        game.setId(gameId);
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                4,
                UUID.randomUUID(),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-23T20:00:00Z"));
        when(gameLookupService.getRequiredGame(gameId)).thenReturn(game);

        service.saveSnapshot(gameId, 4, state, null);

        ArgumentCaptor<GameStateSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GameStateSnapshot.class);
        verify(gameSnapshotStore).save(snapshotCaptor.capture());
        verify(gameLookupService).getRequiredGame(gameId);
        assertThat(snapshotCaptor.getValue().getGame()).isSameAs(game);
        assertThat(snapshotCaptor.getValue().getVersion()).isEqualTo(4);
        assertThat(snapshotCaptor.getValue().getStateJson()).containsEntry("stateVersion", 4);
        assertThat(snapshotCaptor.getValue().getChecksum()).hasSize(64);
    }

    @Test
    void shouldUpdateLatestSnapshotWithoutCreatingAnotherVersion() {
        GameSnapshotServiceImpl service = service();
        UUID gameId = UUID.randomUUID();
        GameStateSnapshot persistedSnapshot = new GameStateSnapshot();
        persistedSnapshot.setVersion(4);
        persistedSnapshot.setStateJson(Map.of("stateVersion", 4));
        persistedSnapshot.setChecksum("old-checksum");
        GameStateDto updatedState = GameStateTestFactory.state(
                gameId,
                GameStatus.SETUP,
                TurnPhase.MAIN,
                0,
                4,
                null,
                List.of(),
                Instant.parse("2026-05-23T20:00:00Z"));
        when(gameSnapshotStore.findLatestByGameId(gameId)).thenReturn(Optional.of(persistedSnapshot));

        service.updateLatestSnapshot(gameId, updatedState);

        verify(gameSnapshotStore).save(persistedSnapshot);
        assertThat(persistedSnapshot.getVersion()).isEqualTo(4);
        assertThat(persistedSnapshot.getStateJson()).containsEntry("status", "SETUP");
        assertThat(persistedSnapshot.getChecksum()).hasSize(64).isNotEqualTo("old-checksum");
    }

    @Test
    void shouldFailSnapshotSaveWhenGameDoesNotExist() {
        GameSnapshotServiceImpl service = service();
        UUID gameId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                4,
                UUID.randomUUID(),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-23T20:00:00Z"));
        when(gameLookupService.getRequiredGame(gameId))
                .thenThrow(new ResourceNotFoundException("Game with id " + gameId + " was not found"));

        assertThatThrownBy(() -> service.saveSnapshot(gameId, 4, state, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(gameId.toString());
    }

    @Test
    void shouldReturnLatestVisibleStateFromPort() {
        GameSnapshotServiceImpl service = service();
        ObjectMapper objectMapper = objectMapper();
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID viewerHandCardId = UUID.randomUUID();
        UUID opponentHandCardId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                7,
                viewerUserId,
                List.of(viewerUserId, opponentUserId),
                false,
                false,
                false,
                Map.of(),
                Map.of(),
                2,
                null,
                Map.of(),
                Map.of(
                        viewerUserId, List.of(viewerHandCardId),
                        opponentUserId, List.of(opponentHandCardId)),
                Map.of(),
                Set.of(),
                Map.of(
                        viewerHandCardId, "HAND",
                        opponentHandCardId, "HAND"),
                Map.of(
                        viewerHandCardId, viewerUserId,
                        opponentHandCardId, opponentUserId),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-23T20:01:00Z"));
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setStateJson(objectMapper.convertValue(state, Map.class));
        when(gameSnapshotStore.findLatestByGameId(gameId)).thenReturn(Optional.of(snapshot));

        Optional<GameStateDto> latest = service.findLatestVisibleState(gameId, viewerUserId);

        verify(gameLookupService).assertParticipant(gameId, viewerUserId);
        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().stateVersion()).isEqualTo(7);
        assertThat(latest.orElseThrow().turn().activePlayerId()).isEqualTo(viewerUserId);
        assertThat(GameStateTestFactory.cardsInHandByPlayer(latest.orElseThrow()))
                .containsEntry(viewerUserId, List.of(viewerHandCardId))
                .containsEntry(opponentUserId, List.of());
        assertThat(GameStateTestFactory.cardsInHandInstanceIdsByPlayer(latest.orElseThrow()))
                .containsEntry(viewerUserId, List.of())
                .containsEntry(opponentUserId, List.of());
        assertThat(latest.orElseThrow().board().zoneByCardReferenceId())
                .containsEntry(viewerHandCardId, CardZone.HAND)
                .doesNotContainKey(opponentHandCardId);
        assertThat(latest.orElseThrow().board().ownerByCardReferenceId())
                .containsEntry(viewerHandCardId, viewerUserId)
                .doesNotContainKey(opponentHandCardId);
    }

    @Test
    void shouldReturnLatestVisibleStateWithoutParticipantCheckForAnonymousViewer() {
        GameSnapshotServiceImpl service = service();
        ObjectMapper objectMapper = objectMapper();
        UUID gameId = UUID.randomUUID();
        UUID viewerUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID opponentHandCardId = UUID.randomUUID();
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                2,
                7,
                viewerUserId,
                List.of(viewerUserId, opponentUserId),
                false,
                false,
                false,
                Map.of(),
                Map.of(),
                2,
                null,
                Map.of(),
                Map.of(opponentUserId, List.of(opponentHandCardId)),
                Map.of(),
                Set.of(),
                Map.of(opponentHandCardId, "HAND"),
                Map.of(opponentHandCardId, opponentUserId),
                List.of(GameActionType.END_TURN),
                Instant.parse("2026-05-23T20:01:00Z"));
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setStateJson(objectMapper.convertValue(state, Map.class));
        when(gameSnapshotStore.findLatestByGameId(gameId)).thenReturn(Optional.of(snapshot));

        Optional<GameStateDto> latest = service.findLatestVisibleState(gameId, null);

        assertThat(latest).isPresent();
        assertThat(GameStateTestFactory.cardsInHandByPlayer(latest.orElseThrow()))
                .containsEntry(opponentUserId, List.of(opponentHandCardId));
        assertThat(latest.orElseThrow().board().zoneByCardReferenceId())
                .containsEntry(opponentHandCardId, CardZone.HAND);
        assertThat(latest.orElseThrow().board().ownerByCardReferenceId())
                .containsEntry(opponentHandCardId, opponentUserId);
        verifyNoInteractions(gameLookupService);
    }

    private GameSnapshotServiceImpl service() {
        return new GameSnapshotServiceImpl(
                gameSnapshotStore,
                gameLookupService,
                objectMapper(),
                gameStateVisibilitySanitizer);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
