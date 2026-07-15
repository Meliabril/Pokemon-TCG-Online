package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameEvent;
import ar.edu.utn.frc.tup.piii.mappers.GameEventMapper;
import ar.edu.utn.frc.tup.piii.repositories.GameEventAccessRepository;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameEventServiceImpl implements GameEventService {

    private final GameEventAccessRepository gameEventRepository;
    private final GameLookupService gameLookupService;
    private final GameEventMapper gameEventMapper;

    @Override
    @Transactional
    public GameEventDto recordPublicEvent(
            UUID gameId,
            GameEventType eventType,
            Integer version,
            Map<String, Object> payload) {
        return persist(gameId, eventType, version, payload, null);
    }

    @Override
    @Transactional
    public GameEventDto recordPrivateEvent(
            UUID gameId,
            GameEventType eventType,
            Integer version,
            Map<String, Object> payload,
            UUID visibleToUserId) {
        return persist(gameId, eventType, version, payload, visibleToUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameEventDto> getVisibleEvents(UUID gameId, UUID viewerUserId) {
        gameLookupService.assertGameExists(gameId);

        List<GameEventDto> visibleEvents = new ArrayList<>();
        for (GameEvent event : gameEventRepository.findVisibleEvents(gameId, viewerUserId)) {
            visibleEvents.add(gameEventMapper.toDto(event));
        }
        return List.copyOf(visibleEvents);
    }

    private GameEventDto persist(
            UUID gameId,
            GameEventType eventType,
            Integer version,
            Map<String, Object> payload,
            UUID visibleToUserId) {
        Game game = gameLookupService.getRequiredGame(gameId);

        GameEvent event = new GameEvent();
        event.setGame(game);
        event.setEventType(eventType);
        event.setVersion(version);
        if (payload == null) {
            event.setPayload(Map.of());
        } else {
            event.setPayload(Map.copyOf(payload));
        }
        event.setVisibleToUserId(visibleToUserId);

        return gameEventMapper.toDto(gameEventRepository.save(event));
    }
}
