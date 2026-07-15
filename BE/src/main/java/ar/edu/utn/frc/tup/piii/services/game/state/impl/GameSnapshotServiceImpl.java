package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameStateSnapshot;
import ar.edu.utn.frc.tup.piii.repositories.GameSnapshotStore;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.util.ChecksumUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameSnapshotServiceImpl implements GameSnapshotService {

    private final GameSnapshotStore gameSnapshotStore;
    private final GameLookupService gameLookupService;
    private final ObjectMapper objectMapper;
    private final GameStateVisibilitySanitizer gameStateVisibilitySanitizer;

    @Override
    @Transactional
    public void saveSnapshot(UUID gameId, int stateVersion, GameStateDto gameState, UUID triggeredByUserId) {
        Game game = gameLookupService.getRequiredGame(gameId);

        Map<String, Object> stateJson = toStateJson(gameState);
        String checksum = ChecksumUtils.sha256Hex(writeBytes(gameState));

        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setGame(game);
        snapshot.setVersion(stateVersion);
        snapshot.setStateJson(stateJson);
        snapshot.setChecksum(checksum);

        gameSnapshotStore.save(snapshot);
    }

    @Override
    @Transactional
    public void updateLatestSnapshot(UUID gameId, GameStateDto gameState) {
        GameStateSnapshot snapshot = gameSnapshotStore.findLatestByGameId(gameId)
                .orElseThrow(() -> new IllegalStateException("No snapshot found for game " + gameId));
        snapshot.setStateJson(toStateJson(gameState));
        snapshot.setChecksum(ChecksumUtils.sha256Hex(writeBytes(gameState)));
        gameSnapshotStore.save(snapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameStateDto> findLatestVisibleState(UUID gameId, UUID viewerUserId) {
        if (viewerUserId != null) {
            gameLookupService.assertParticipant(gameId, viewerUserId);
        }

        Optional<GameStateSnapshot> snapshot = gameSnapshotStore.findLatestByGameId(gameId);
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }

        GameStateDto state = toGameStateDto(snapshot.get().getStateJson());
        GameStateDto visibleState = gameStateVisibilitySanitizer.sanitizeForViewer(state, viewerUserId);
        return Optional.of(visibleState);
    }

    private byte[] writeBytes(GameStateDto gameState) {
        try {
            return objectMapper.writeValueAsBytes(gameState);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize game state for checksum", exception);
        }
    }

    private GameStateDto toGameStateDto(Map<String, Object> stateJson) {
        return objectMapper.convertValue(stateJson, GameStateDto.class);
    }

    private Map<String, Object> toStateJson(GameStateDto gameState) {
        return objectMapper.convertValue(gameState, new TypeReference<>() {
        });
    }
}
