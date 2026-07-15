package ar.edu.utn.frc.tup.piii.services.game.presence.impl;

import ar.edu.utn.frc.tup.piii.controllers.websocket.GameWebSocketPresenceRegistry;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.GameParticipantRepository;
import ar.edu.utn.frc.tup.piii.repositories.GameRepository;
import ar.edu.utn.frc.tup.piii.services.game.presence.GameParticipantConnectedEvent;
import ar.edu.utn.frc.tup.piii.services.game.presence.GameParticipantPresenceChangedEvent;
import ar.edu.utn.frc.tup.piii.services.game.presence.GamePresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class GamePresenceServiceImpl implements GamePresenceService {

    private final GameParticipantRepository gameParticipantRepository;
    private final GameRepository gameRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameWebSocketPresenceRegistry gameWebSocketPresenceRegistry;

    @Override
    @Transactional
    public void markConnected(UUID gameId, UUID userId) {
        GameParticipant participant = loadParticipant(gameId, userId);
        boolean wasConnected = Boolean.TRUE.equals(participant.getConnected());
        Instant now = Instant.now();

        participant.setConnected(Boolean.TRUE);
        participant.setLastSeenAt(now);
        gameParticipantRepository.save(participant);

        Game game = loadGameForUpdate(gameId);
        publishPresenceUpdate(game, userId, true, now);

        if (!wasConnected) {
            applicationEventPublisher.publishEvent(new GameParticipantConnectedEvent(gameId, userId));
        }
    }
    private Optional<GameParticipant> findParticipant(UUID gameId, UUID userId) {
        return gameParticipantRepository.findByGame_IdAndUserId(gameId, userId);
    }
    @Override
    @Transactional
    public void markDisconnected(UUID gameId, UUID userId) {

        Optional<GameParticipant> optionalParticipant =
                gameParticipantRepository.findByGame_IdAndUserId(gameId, userId);

        if (optionalParticipant.isEmpty()) {
            log.warn(
                    "Ignoring disconnect. Participant not found. game={}, user={}",
                    gameId,
                    userId
            );
            return;
        }

        GameParticipant participant = optionalParticipant.get();

        if (!Boolean.TRUE.equals(participant.getConnected())) {
            return;
        }

        participant.setConnected(Boolean.FALSE);
        gameParticipantRepository.save(participant);

        publishPresenceUpdate(
                loadGameForUpdate(gameId),
                userId,
                false,
                participant.getLastSeenAt()
        );
    }

    @Override
    @Transactional
    public void leaveGame(String sessionId, UUID gameId, UUID userId) {
        if (!gameWebSocketPresenceRegistry.unregister(sessionId, gameId, userId)) {
            return;
        }

        markDisconnected(gameId, userId);
    }

    private void publishPresenceUpdate(Game game, UUID userId, boolean connected, Instant lastSeenAt) {
        applicationEventPublisher.publishEvent(new GameParticipantPresenceChangedEvent(
                game.getId(),
                game.getStateVersion(),
                userId,
                connected,
                lastSeenAt));
    }

    private GameParticipant loadParticipant(UUID gameId, UUID userId) {
        Optional<GameParticipant> participant = gameParticipantRepository.findByGame_IdAndUserId(gameId, userId);
        if (participant.isEmpty()) {
            throw new ResourceNotFoundException("Game participant not found for user " + userId + " in game " + gameId);
        }
        return participant.get();
    }

    private Game loadGameForUpdate(UUID gameId) {
        Optional<Game> game = gameRepository.findDetailByIdForUpdate(gameId);
        if (game.isEmpty()) {
            throw new ResourceNotFoundException("Game not found with id: " + gameId);
        }
        return game.get();
    }
}
