package ar.edu.utn.frc.tup.piii.controllers.websocket;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class GameWebSocketPresenceRegistry {

    private final Map<String, Set<GamePresenceKey>> participantKeysBySessionId = new HashMap<>();
    private final Map<GamePresenceKey, Set<String>> sessionIdsByParticipantKey = new HashMap<>();

    public synchronized boolean register(String sessionId, UUID gameId, UUID userId) {
        GamePresenceKey key = new GamePresenceKey(gameId, userId);
        Set<GamePresenceKey> sessionKeys = participantKeysBySessionId.computeIfAbsent(sessionId, ignored -> new HashSet<>());
        if (!sessionKeys.add(key)) {
            return false;
        }

        sessionIdsByParticipantKey.computeIfAbsent(key, ignored -> new HashSet<>()).add(sessionId);
        return true;
    }

    public synchronized List<GamePresenceKey> unregisterSession(String sessionId) {
        Set<GamePresenceKey> keys = participantKeysBySessionId.remove(sessionId);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        List<GamePresenceKey> disconnectedParticipants = new ArrayList<>();
        for (GamePresenceKey key : keys) {
            Set<String> participantSessionIds = sessionIdsByParticipantKey.get(key);
            if (participantSessionIds == null) {
                continue;
            }

            participantSessionIds.remove(sessionId);
            if (participantSessionIds.isEmpty()) {
                sessionIdsByParticipantKey.remove(key);
                disconnectedParticipants.add(key);
            }
        }

        return disconnectedParticipants;
    }

    public synchronized boolean unregister(String sessionId, UUID gameId, UUID userId) {
        GamePresenceKey key = new GamePresenceKey(gameId, userId);
        Set<GamePresenceKey> sessionKeys = participantKeysBySessionId.get(sessionId);
        if (sessionKeys == null || !sessionKeys.remove(key)) {
            return false;
        }

        if (sessionKeys.isEmpty()) {
            participantKeysBySessionId.remove(sessionId);
        }

        Set<String> participantSessionIds = sessionIdsByParticipantKey.get(key);
        if (participantSessionIds == null || !participantSessionIds.remove(sessionId)) {
            return false;
        }

        if (!participantSessionIds.isEmpty()) {
            return false;
        }

        sessionIdsByParticipantKey.remove(key);
        return true;
    }

    public record GamePresenceKey(UUID gameId, UUID userId) {
    }
}
