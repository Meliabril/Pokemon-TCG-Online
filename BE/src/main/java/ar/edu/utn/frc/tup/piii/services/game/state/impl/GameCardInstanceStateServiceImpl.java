package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.repositories.GameCardInstanceRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameCardInstanceStateServiceImpl implements GameCardInstanceStateService {

    private static final int FIRST_TEMPORARY_ZONE_POSITION = -1;

    private final GameCardInstanceRepository gameCardInstanceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GameCardInstance> findByGameId(UUID gameId) {
        return gameCardInstanceRepository.findByGame_Id(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameCardInstance> findByGameIdAndOwnerUserIdAndZone(UUID gameId, UUID ownerUserId, CardZone zone) {
        return gameCardInstanceRepository.findByGame_IdAndOwnerUserIdAndZoneOrderByZonePositionAsc(gameId, ownerUserId, zone);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameCardInstance> findByIdAndGameIdAndOwnerUserId(UUID id, UUID gameId, UUID ownerUserId) {
        return gameCardInstanceRepository.findByIdAndGame_IdAndOwnerUserId(id, gameId, ownerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameCardInstance> findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
            UUID gameId,
            UUID ownerUserId,
            UUID cardId,
            CardZone zone) {
        return gameCardInstanceRepository.findFirstByGame_IdAndOwnerUserIdAndCardIdAndZoneOrderByZonePositionAsc(
                gameId, ownerUserId, cardId, zone);
    }

    @Override
    @Transactional
    public GameCardInstance save(GameCardInstance gameCardInstance) {
        return gameCardInstanceRepository.save(gameCardInstance);
    }

    @Override
    @Transactional
    public List<GameCardInstance> saveAll(Iterable<GameCardInstance> gameCardInstances) {
        return gameCardInstanceRepository.saveAll(gameCardInstances);
    }

    @Override
    @Transactional
    public void flush() {
        gameCardInstanceRepository.flush();
    }

    @Override
    @Transactional
    public int swapActiveWithBench(UUID activeInstanceId, UUID benchInstanceId, Integer benchSlot) {
        return gameCardInstanceRepository.swapActiveWithBench(activeInstanceId, benchInstanceId, benchSlot);
    }

    @Override
    @Transactional(readOnly = true)
    public int nextZonePosition(UUID gameId, UUID ownerUserId, CardZone zone) {
        List<GameCardInstance> zoneCards = findByGameIdAndOwnerUserIdAndZone(gameId, ownerUserId, zone);
        int highestPosition = 0;
        for (GameCardInstance zoneCard : zoneCards) {
            Integer zonePosition = zoneCard.getZonePosition();
            if (zonePosition != null && zonePosition > highestPosition) {
                highestPosition = zonePosition;
            }
        }
        return highestPosition + 1;
    }

    @Override
    @Transactional
    public void resequenceZone(UUID gameId, UUID ownerUserId, CardZone zone) {
        List<GameCardInstance> zoneCards = findByGameIdAndOwnerUserIdAndZone(gameId, ownerUserId, zone);
        if (zoneCards.isEmpty()) {
            return;
        }

        int temporaryPosition = FIRST_TEMPORARY_ZONE_POSITION;
        for (GameCardInstance zoneCard : zoneCards) {
            zoneCard.setZonePosition(temporaryPosition--);
        }
        gameCardInstanceRepository.flush();

        int position = 1;
        for (GameCardInstance zoneCard : zoneCards) {
            zoneCard.setZonePosition(position++);
        }
    }
}
