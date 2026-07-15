package ar.edu.utn.frc.tup.piii.services.game.query.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.services.game.query.GameEventService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameVisibleEventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameVisibleEventQueryServiceImpl implements GameVisibleEventQueryService {

    private final GameEventService gameEventService;

    @Override
    @Transactional(readOnly = true)
    public List<GameEventDto> getVisibleEvents(UUID gameId, UUID viewerUserId) {
        List<GameEventDto> visibleEvents = new ArrayList<>();
        for (GameEventDto event : gameEventService.getVisibleEvents(gameId, viewerUserId)) {
            if (GameEventType.STATE_SYNC.equals(event.eventType())) {
                continue;
            }
            visibleEvents.add(event);
        }

        return List.copyOf(visibleEvents);
    }
}
