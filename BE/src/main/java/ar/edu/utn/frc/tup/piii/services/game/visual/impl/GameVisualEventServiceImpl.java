package ar.edu.utn.frc.tup.piii.services.game.visual.impl;

import ar.edu.utn.frc.tup.piii.dtos.websocket.CardHoverChangedVisualEventDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.BadRequestException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.query.GameDataService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameSnapshotService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.visual.GameVisualEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameVisualEventServiceImpl implements GameVisualEventService {

    private static final String CARD_HOVER_CHANGED = "CARD_HOVER_CHANGED";
    private static final String SETUP_SLOT_CHANGED = "SETUP_SLOT_CHANGED";
    private static final String VISUAL_EVENTS_DESTINATION_PREFIX = "/topic/games/";
    private static final String VISUAL_EVENTS_DESTINATION_SUFFIX = "/visual-events";
    private static final String SETUP_ACTIVE_OCCUPIED_KEY = "setupActiveOccupiedByPlayer";
    private static final String SETUP_BENCH_OCCUPIED_INDEXES_KEY = "setupBenchOccupiedIndexesByPlayer";
    private static final String SETUP_ACTIVE_CARD_INSTANCE_ID_KEY = "setupActiveCardInstanceIdByPlayer";
    private static final String SETUP_BENCH_CARD_INSTANCE_IDS_KEY = "setupBenchCardInstanceIdsByPlayer";
    private static final int MAXIMUM_BENCH_CAPACITY = 5;
    private static final Set<String> VALID_ZONES = Set.of("HAND", "DECK", "PRIZES", "BENCH", "ACTIVE", "DISCARD");
    private static final Set<String> VALID_SETUP_ZONES = Set.of("BENCH", "ACTIVE");
    private static final Set<String> VALID_OWNERS = Set.of("SELF", "OPPONENT");

    private final GameLookupService gameLookupService;
    private final GameDataService gameDataService;
    private final GameStateQueryService gameStateQueryService;
    private final GameSnapshotService gameSnapshotService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void publishVisualEvent(UUID gameId, UUID playerId, CardHoverChangedVisualEventDto event) {
        gameLookupService.assertParticipant(gameId, playerId);
        validate(event);

        CardHoverChangedVisualEventDto safeEvent = switch (event.type()) {
            case CARD_HOVER_CHANGED -> new CardHoverChangedVisualEventDto(
                    CARD_HOVER_CHANGED,
                    gameId,
                    playerId,
                    event.zone(),
                    event.owner(),
                    event.visualIndex(),
                    event.cardInstanceId(),
                    event.hovered(),
                    null);
            case SETUP_SLOT_CHANGED -> new CardHoverChangedVisualEventDto(
                    SETUP_SLOT_CHANGED,
                    gameId,
                    playerId,
                    event.zone(),
                    event.owner(),
                    event.visualIndex(),
                    null,
                    null,
                    event.occupied());
            default -> throw new BadRequestException("Unsupported visual event type.");
        };

        if (SETUP_SLOT_CHANGED.equals(safeEvent.type())) {
            persistSetupOccupancy(gameId, playerId, event);
        }
        messagingTemplate.convertAndSend(destination(gameId), safeEvent);
    }

    private void persistSetupOccupancy(
            UUID gameId,
            UUID playerId,
            CardHoverChangedVisualEventDto event) {
        Game game = gameDataService.getRequiredGameForUpdate(gameId);
        if (!GameStatus.SETUP.equals(game.getStatus())) {
            throw new BadRequestException("Setup slot changes are only allowed during setup.");
        }

        Map<String, Object> setupState = mutableMap(game.getSetupState());
        String playerKey = playerId.toString();
        UUID selectedCardInstanceId = Boolean.TRUE.equals(event.occupied())
                ? validateSetupCard(gameId, playerId, event.cardInstanceId())
                : null;
        ensureSetupCardIsNotDuplicated(setupState, playerKey, event, selectedCardInstanceId);
        if ("ACTIVE".equals(event.zone())) {
            Map<String, Object> activeOccupiedByPlayer = nestedMap(setupState, SETUP_ACTIVE_OCCUPIED_KEY);
            activeOccupiedByPlayer.put(playerKey, event.occupied());
            setupState.put(SETUP_ACTIVE_OCCUPIED_KEY, Map.copyOf(activeOccupiedByPlayer));
            Map<String, Object> activeCardsByPlayer = nestedMap(setupState, SETUP_ACTIVE_CARD_INSTANCE_ID_KEY);
            if (selectedCardInstanceId == null) {
                activeCardsByPlayer.remove(playerKey);
            } else {
                activeCardsByPlayer.put(playerKey, selectedCardInstanceId.toString());
            }
            setupState.put(SETUP_ACTIVE_CARD_INSTANCE_ID_KEY, Map.copyOf(activeCardsByPlayer));
        } else {
            Map<String, Object> benchIndexesByPlayer = nestedMap(setupState, SETUP_BENCH_OCCUPIED_INDEXES_KEY);
            Set<Integer> occupiedIndexes = integerSet(benchIndexesByPlayer.get(playerKey));
            if (Boolean.TRUE.equals(event.occupied())) {
                occupiedIndexes.add(event.visualIndex());
            } else {
                occupiedIndexes.remove(event.visualIndex());
            }
            benchIndexesByPlayer.put(playerKey, List.copyOf(occupiedIndexes));
            setupState.put(SETUP_BENCH_OCCUPIED_INDEXES_KEY, Map.copyOf(benchIndexesByPlayer));

            Map<String, Object> benchCardsByPlayer = nestedMap(setupState, SETUP_BENCH_CARD_INSTANCE_IDS_KEY);
            List<String> selectedBenchCards = stringList(benchCardsByPlayer.get(playerKey));
            int index = event.visualIndex();
            if (selectedCardInstanceId != null) {
                if (index > selectedBenchCards.size()) {
                    throw new BadRequestException("Bench setup preview slots must be contiguous.");
                }
                if (index == selectedBenchCards.size()) {
                    selectedBenchCards.add(selectedCardInstanceId.toString());
                } else {
                    selectedBenchCards.set(index, selectedCardInstanceId.toString());
                }
            } else if (index < selectedBenchCards.size()) {
                selectedBenchCards.remove(index);
            }
            benchCardsByPlayer.put(playerKey, List.copyOf(selectedBenchCards));
            setupState.put(SETUP_BENCH_CARD_INSTANCE_IDS_KEY, Map.copyOf(benchCardsByPlayer));
        }

        game.setSetupState(Map.copyOf(setupState));
        Game savedGame = gameDataService.save(game);
        gameSnapshotService.updateLatestSnapshot(
                gameId,
                gameStateQueryService.buildVisibleState(savedGame));
    }

    private Map<String, Object> mutableMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }

    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object nestedValue = source.get(key);
        Map<String, Object> values = new LinkedHashMap<>();
        if (!(nestedValue instanceof Map<?, ?> nestedMap)) {
            return values;
        }
        for (Map.Entry<?, ?> entry : nestedMap.entrySet()) {
            if (entry.getKey() != null) {
                values.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return values;
    }

    private Set<Integer> integerSet(Object source) {
        Set<Integer> indexes = new TreeSet<>();
        if (!(source instanceof List<?> values)) {
            return indexes;
        }
        for (Object value : values) {
            if (value instanceof Number number) {
                indexes.add(number.intValue());
            }
        }
        return indexes;
    }

    private List<String> stringList(Object source) {
        List<String> values = new java.util.ArrayList<>();
        if (source instanceof List<?> sourceValues) {
            for (Object value : sourceValues) {
                if (value != null) {
                    values.add(String.valueOf(value));
                }
            }
        }
        return values;
    }

    private UUID validateSetupCard(UUID gameId, UUID playerId, UUID cardInstanceId) {
        if (cardInstanceId == null) {
            throw new BadRequestException("Occupied setup slots require a card identity.");
        }
        GameCardInstance cardInstance = gameCardInstanceStateService
                .findByIdAndGameIdAndOwnerUserId(cardInstanceId, gameId, playerId)
                .orElseThrow(() -> new BadRequestException("Selected setup card is not in the game."));
        if (!CardZone.HAND.equals(cardInstance.getZone())) {
            throw new BadRequestException("Selected setup card is not in the player's hand.");
        }
        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card == null || !card.isBasicStage()) {
            throw new BadRequestException("Only Basic Pokemon can occupy setup slots.");
        }
        return cardInstanceId;
    }

    private void ensureSetupCardIsNotDuplicated(
            Map<String, Object> setupState,
            String playerKey,
            CardHoverChangedVisualEventDto event,
            UUID selectedCardInstanceId) {
        if (selectedCardInstanceId == null) {
            return;
        }
        String selectedId = selectedCardInstanceId.toString();
        String activeId = String.valueOf(
                nestedMap(setupState, SETUP_ACTIVE_CARD_INSTANCE_ID_KEY).get(playerKey));
        List<String> benchIds = stringList(
                nestedMap(setupState, SETUP_BENCH_CARD_INSTANCE_IDS_KEY).get(playerKey));
        boolean duplicated = benchIds.contains(selectedId);
        if ("BENCH".equals(event.zone())) {
            duplicated = selectedId.equals(activeId);
            for (int index = 0; index < benchIds.size() && !duplicated; index++) {
                duplicated = index != event.visualIndex() && selectedId.equals(benchIds.get(index));
            }
        }
        if (duplicated) {
            throw new BadRequestException("The same card cannot occupy more than one setup slot.");
        }
    }

    private String destination(UUID gameId) {
        return VISUAL_EVENTS_DESTINATION_PREFIX + gameId + VISUAL_EVENTS_DESTINATION_SUFFIX;
    }

    private void validate(CardHoverChangedVisualEventDto event) {
        if (event == null) {
            throw new BadRequestException("Visual event payload is required.");
        }

        if (CARD_HOVER_CHANGED.equals(event.type())) {
            validateHoverEvent(event);
            return;
        }

        if (SETUP_SLOT_CHANGED.equals(event.type())) {
            validateSetupPreviewEvent(event);
            return;
        }

        throw new BadRequestException("Unsupported visual event type.");
    }

    private void validateHoverEvent(CardHoverChangedVisualEventDto event) {
        if (!VALID_ZONES.contains(event.zone())) {
            throw new BadRequestException("Unsupported visual event zone.");
        }

        if (!VALID_OWNERS.contains(event.owner())) {
            throw new BadRequestException("Unsupported visual event owner.");
        }

        if (Boolean.TRUE.equals(event.hovered()) && event.visualIndex() == null && event.cardInstanceId() == null) {
            throw new BadRequestException("Visual event target is required.");
        }
    }

    private void validateSetupPreviewEvent(CardHoverChangedVisualEventDto event) {
        if (!VALID_SETUP_ZONES.contains(event.zone())) {
            throw new BadRequestException("Unsupported setup preview zone.");
        }

        if (!VALID_OWNERS.contains(event.owner())) {
            throw new BadRequestException("Unsupported visual event owner.");
        }

        if (event.occupied() == null) {
            throw new BadRequestException("Setup preview occupied state is required.");
        }

        if (event.visualIndex() == null) {
            throw new BadRequestException("Setup preview slot index is required.");
        }

        if ("ACTIVE".equals(event.zone()) && event.visualIndex() != 0) {
            throw new BadRequestException("Active setup preview slot index must be zero.");
        }

        if ("BENCH".equals(event.zone())
                && (event.visualIndex() < 0 || event.visualIndex() >= MAXIMUM_BENCH_CAPACITY)) {
            throw new BadRequestException("Bench setup preview slot index is out of range.");
        }

        if (Boolean.TRUE.equals(event.occupied()) && event.cardInstanceId() == null) {
            throw new BadRequestException("Occupied setup slots require a card identity.");
        }
    }
}
