package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.repositories.GameCardInstanceRepository;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Override
    @Transactional
    public void reorderAndPersistZone(UUID gameId, UUID ownerUserId, CardZone targetZone, List<GameCardInstance> cards) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        validateUniqueCardIds(gameId, ownerUserId, targetZone, cards);

        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (GameCardInstance card : cards) {
                sb.append(card.getId()).append(":").append(card.getZone()).append("(").append(card.getZonePosition()).append(") ");
            }
            log.debug("[REORDER_ZONE] Before positions: {}", sb.toString());
        }

        int tempPos = -1000;
        for (GameCardInstance card : cards) {
            card.setZone(targetZone);
            card.setZonePosition(tempPos--);
        }
        gameCardInstanceRepository.saveAll(cards);
        gameCardInstanceRepository.flush();

        int finalPos = 1;
        for (GameCardInstance card : cards) {
            card.setZonePosition(finalPos++);
        }
        validateFinalPositions(gameId, ownerUserId, targetZone, cards);
        gameCardInstanceRepository.saveAll(cards);
        gameCardInstanceRepository.flush();

        if (log.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            for (GameCardInstance card : cards) {
                sb.append(card.getId()).append(":").append(card.getZone()).append("(").append(card.getZonePosition()).append(") ");
            }
            log.debug("[REORDER_ZONE] After positions: {}", sb.toString());
        }

        log.info("[REORDER_ZONE] Reordered cards for gameId={}, ownerUserId={}, zone={}, count={} in {} ms",
                gameId, ownerUserId, targetZone, cards.size(), System.currentTimeMillis() - startTime);
    }

    private void validateUniqueCardIds(
            UUID gameId,
            UUID ownerUserId,
            CardZone targetZone,
            List<GameCardInstance> cards) {
        Set<UUID> uniqueIds = new LinkedHashSet<>();
        for (GameCardInstance card : cards) {
            UUID cardId = card.getId();
            if (cardId == null || !uniqueIds.add(cardId)) {
                log.error(
                        "[REORDER_ZONE] Duplicate or null card id before temporary reorder. gameId={}, ownerUserId={}, zone={}, cardId={}",
                        gameId,
                        ownerUserId,
                        targetZone,
                        cardId);
                throw new IllegalStateException("Duplicate or null card id detected while reordering zone");
            }
        }
    }

    private void validateFinalPositions(
            UUID gameId,
            UUID ownerUserId,
            CardZone targetZone,
            List<GameCardInstance> cards) {
        Set<Integer> usedPositions = new LinkedHashSet<>();
        for (GameCardInstance card : cards) {
            Integer zonePosition = card.getZonePosition();
            if (zonePosition == null || !usedPositions.add(zonePosition)) {
                log.error(
                        "[REORDER_ZONE] Duplicate or null final position detected before flush. gameId={}, ownerUserId={}, zone={}, cardId={}, zonePosition={}",
                        gameId,
                        ownerUserId,
                        targetZone,
                        card.getId(),
                        zonePosition);
                throw new IllegalStateException("Duplicate or null final zone position detected while reordering zone");
            }
        }
    }
}
