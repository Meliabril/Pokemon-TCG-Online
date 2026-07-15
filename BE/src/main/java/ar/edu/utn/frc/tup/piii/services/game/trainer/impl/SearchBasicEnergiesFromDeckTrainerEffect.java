package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchBasicEnergiesFromDeckTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "SEARCH_BASIC_ENERGIES_FROM_DECK";
    private static final String EVENT_SOURCE = "TRAINER";
    private static final String SELECTED_CARD_IDS_KEY = "selectedCardIds";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(Card card) {
        if (!isItemOrSupporter(card)) {
            return false;
        }
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(card);
        return EFFECT_TYPE.equals(definition.type()) && definition.amount() > 0;
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(context.trainerCard());
        int maxToSearch = definition.amount();
        if (maxToSearch <= 0) {
            throw new InvalidGameActionException("Search amount must be positive");
        }

        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);

        // Resolve which energies to take: prefer player selection, fall back to auto-pick
        List<GameCardInstance> energiesToTake = resolveSelection(
                context.request().payload(), deckCards, maxToSearch);

        if (energiesToTake.isEmpty()) {
            shuffleDeck(gameId, actorUserId);
            return new TrainerEffectResult(
                    Map.of("effectType", EFFECT_TYPE, "cardsFound", 0), List.of());
        }

        Set<UUID> takenIds = energiesToTake.stream()
                .map(GameCardInstance::getId)
                .collect(Collectors.toSet());

        int nextHandPosition = gameCardInstanceStateService
                .nextZonePosition(gameId, actorUserId, CardZone.HAND);
        for (GameCardInstance instance : energiesToTake) {
            instance.setZone(CardZone.HAND);
            instance.setFaceDown(false);
            instance.setZonePosition(nextHandPosition++);
        }
        gameCardInstanceStateService.saveAll(energiesToTake);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        // Shuffle remaining deck cards (exclude the ones just moved to hand)
        List<GameCardInstance> remainingDeck = deckCards.stream()
                .filter(c -> !takenIds.contains(c.getId()))
                .collect(Collectors.toList());
        shuffleCards(gameId, actorUserId, remainingDeck);

        List<GameEventDto> events = buildEvents(context, actorUserId, gameId, energiesToTake);

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("cardsFound", energiesToTake.size());
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    /**
     * Returns up to maxToSearch basic energies from the deck.
     * selectedCardIds accepts repeated catalog card ids to express quantity. Instance ids
     * are still accepted for compatibility with older clients.
     */
    private List<GameCardInstance> resolveSelection(
            Map<String, Object> payload, List<GameCardInstance> deckCards, int maxToSearch) {

        List<UUID> selectedIds = parseSelectedIds(payload);
        if (!selectedIds.isEmpty()) {
            if (selectedIds.size() > maxToSearch) {
                throw new InvalidGameActionException("Professor's Letter can search up to " + maxToSearch + " Basic Energy cards");
            }

            List<GameCardInstance> selectedByCardId = resolveSelectedCardTypes(selectedIds, deckCards);
            if (selectedByCardId.size() == selectedIds.size()) {
                return selectedByCardId;
            }

            List<GameCardInstance> selectedByInstanceId = resolveSelectedInstances(selectedIds, deckCards);
            if (selectedByInstanceId.size() == selectedIds.size()) {
                return selectedByInstanceId;
            }

            throw new InvalidGameActionException("Selected cards must be Basic Energy cards available in your deck");
        }

        // Auto-pick: first N basic energies found in the deck
        List<GameCardInstance> autoPicked = new ArrayList<>();
        for (GameCardInstance instance : deckCards) {
            if (autoPicked.size() >= maxToSearch) {
                break;
            }
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (isBasicEnergy(card)) {
                autoPicked.add(instance);
            }
        }
        return autoPicked;
    }

    private List<GameCardInstance> resolveSelectedCardTypes(List<UUID> selectedCardIds, List<GameCardInstance> deckCards) {
        List<GameCardInstance> chosen = new ArrayList<>();
        Set<UUID> chosenInstanceIds = new java.util.LinkedHashSet<>();
        for (UUID selectedCardId : selectedCardIds) {
            GameCardInstance match = findFirstBasicEnergyByCardId(selectedCardId, deckCards, chosenInstanceIds);
            if (match == null) {
                break;
            }
            chosen.add(match);
            chosenInstanceIds.add(match.getId());
        }
        return chosen;
    }

    private GameCardInstance findFirstBasicEnergyByCardId(
            UUID selectedCardId,
            List<GameCardInstance> deckCards,
            Set<UUID> excludedInstanceIds) {

        for (GameCardInstance instance : deckCards) {
            if (excludedInstanceIds.contains(instance.getId()) || !selectedCardId.equals(instance.getCardId())) {
                continue;
            }
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (isBasicEnergy(card)) {
                return instance;
            }
        }
        return null;
    }

    private List<GameCardInstance> resolveSelectedInstances(List<UUID> selectedInstanceIds, List<GameCardInstance> deckCards) {
        List<GameCardInstance> chosen = new ArrayList<>();
        Set<UUID> chosenInstanceIds = new java.util.LinkedHashSet<>();
        for (UUID selectedInstanceId : selectedInstanceIds) {
            GameCardInstance match = findBasicEnergyByInstanceId(selectedInstanceId, deckCards, chosenInstanceIds);
            if (match == null) {
                break;
            }
            chosen.add(match);
            chosenInstanceIds.add(match.getId());
        }
        return chosen;
    }

    private GameCardInstance findBasicEnergyByInstanceId(
            UUID selectedInstanceId,
            List<GameCardInstance> deckCards,
            Set<UUID> excludedInstanceIds) {

        for (GameCardInstance instance : deckCards) {
            if (excludedInstanceIds.contains(instance.getId()) || !selectedInstanceId.equals(instance.getId())) {
                continue;
            }
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (isBasicEnergy(card)) {
                return instance;
            }
        }
        return null;
    }

    private List<UUID> parseSelectedIds(Map<String, Object> payload) {
        if (payload == null) {
            return List.of();
        }
        Object raw = payload.get(SELECTED_CARD_IDS_KEY);
        if (!(raw instanceof List<?> rawList)) {
            return List.of();
        }
        List<UUID> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof UUID uuid) {
                result.add(uuid);
            } else if (item instanceof String str && !str.isBlank()) {
                try {
                    result.add(UUID.fromString(str));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed IDs
                }
            }
        }
        return result;
    }

    private void shuffleDeck(UUID gameId, UUID actorUserId) {
        List<GameCardInstance> deck = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        shuffleCards(gameId, actorUserId, deck);
    }

    private void shuffleCards(UUID gameId, UUID actorUserId, List<GameCardInstance> cards) {
        if (cards.isEmpty()) {
            return;
        }
        List<GameCardInstance> shuffled = gameRandomService.shuffledCopy(cards);
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setZonePosition(i + 1);
        }
        gameCardInstanceStateService.saveAll(shuffled);
    }

    private List<GameEventDto> buildEvents(
            TrainerEffectContext context,
            UUID actorUserId,
            UUID gameId,
            List<GameCardInstance> energiesTaken) {

        List<GameEventDto> events = new ArrayList<>();

        List<String> foundCardIds = energiesTaken.stream()
                .map(c -> c.getCardId().toString())
                .collect(Collectors.toList());
        List<Map<String, Object>> revealedEnergies = groupRevealedEnergies(energiesTaken);

        // Public: opponent sees exactly which energy types (and how many of each) were revealed,
        // not just a bare count
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", actorUserId.toString());
        publicPayload.put("cardsFound", foundCardIds.size());
        publicPayload.put("revealedEnergies", revealedEnergies);
        publicPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(publicPayload)));

        // Private: actor sees which cards were taken
        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", actorUserId.toString());
        privatePayload.put("cardIds", List.copyOf(foundCardIds));
        privatePayload.put("revealedEnergies", revealedEnergies);
        privatePayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(),
                Map.copyOf(privatePayload), actorUserId));

        return events;
    }

    private List<Map<String, Object>> groupRevealedEnergies(List<GameCardInstance> energiesTaken) {
        Map<UUID, Card> cardById = new LinkedHashMap<>();
        Map<UUID, Integer> countById = new LinkedHashMap<>();
        for (GameCardInstance instance : energiesTaken) {
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (card == null) {
                continue;
            }
            cardById.putIfAbsent(card.getId(), card);
            countById.merge(card.getId(), 1, Integer::sum);
        }

        List<Map<String, Object>> grouped = new ArrayList<>();
        for (UUID cardId : cardById.keySet()) {
            Card card = cardById.get(cardId);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("cardId", card.getId().toString());
            entry.put("name", card.getName());
            entry.put("count", countById.get(cardId));
            grouped.add(Map.copyOf(entry));
        }
        return List.copyOf(grouped);
    }

    @Override
    public Map<String, Object> preview(TrainerEffectContext context) {
        TrainerEffectDefinition definition = trainerEffectDefinitionReader.read(context.trainerCard());
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);

        Map<UUID, Card> cardById = new LinkedHashMap<>();
        Map<UUID, Integer> countById = new LinkedHashMap<>();
        for (GameCardInstance instance : deckCards) {
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (!isBasicEnergy(card)) {
                continue;
            }
            cardById.putIfAbsent(card.getId(), card);
            countById.merge(card.getId(), 1, Integer::sum);
        }

        List<Map<String, Object>> options = new ArrayList<>();
        for (UUID cardId : cardById.keySet()) {
            Card card = cardById.get(cardId);
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("cardId", card.getId().toString());
            option.put("name", card.getName());
            option.put("availableInDeck", countById.get(cardId));
            if (card.getImageSmallUrl() != null) {
                option.put("imageSmallUrl", card.getImageSmallUrl());
            }
            if (card.getImageLargeUrl() != null) {
                option.put("imageLargeUrl", card.getImageLargeUrl());
            }
            options.add(Map.copyOf(option));
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("maxSelectable", definition.amount());
        preview.put("options", List.copyOf(options));
        return Map.copyOf(preview);
    }

    private boolean isBasicEnergy(Card card) {
        return card != null
                && CardSupertype.ENERGY.equals(card.getSupertype())
                && (card.getSubtype() == null || card.getSubtype().isBlank()
                    || card.getSubtype().equalsIgnoreCase("Basic"));
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }
}
