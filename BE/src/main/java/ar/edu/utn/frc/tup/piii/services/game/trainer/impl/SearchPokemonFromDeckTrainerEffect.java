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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchPokemonFromDeckTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "SEARCH_POKEMON_FROM_DECK";
    private static final String EVENT_SOURCE = "TRAINER";
    private static final String SELECTED_CARD_INSTANCE_ID_KEY = "targetCardInstanceId";
    private static final int LOOK_AT_TOP_N = 7;

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
        return EFFECT_TYPE.equals(definition.type());
    }

    @Override
    public TrainerEffectResult apply(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        List<GameCardInstance> deckCards = new java.util.ArrayList<>(gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK));

        // Sort by position ascending so position 1 = top of deck
        deckCards.sort(Comparator.comparingInt(c -> c.getZonePosition() != null ? c.getZonePosition() : Integer.MAX_VALUE));

        List<GameCardInstance> topCards = deckCards.stream()
                .limit(LOOK_AT_TOP_N)
                .collect(Collectors.toList());

        if (topCards.isEmpty()) {
            return new TrainerEffectResult(
                    Map.of("effectType", EFFECT_TYPE, "cardsFound", 0), List.of());
        }

        // Prefer player-selected card if it is in the top N and is a Pokemon
        UUID selectedInstanceId = optionalUuid(context.request().payload(), SELECTED_CARD_INSTANCE_ID_KEY);
        GameCardInstance chosen = findSelectedPokemon(topCards, selectedInstanceId);

        // Fall back to first Pokemon in the top N
        if (chosen == null) {
            chosen = findFirstPokemon(topCards);
        }

        List<String> revealedCardIds = topCards.stream()
                .map(c -> c.getCardId().toString())
                .collect(Collectors.toList());

        if (chosen == null) {
            // No Pokemon in the top N — shuffle those cards back and return
            shuffleDeck(gameId, actorUserId);
            List<GameEventDto> emptyEvents = buildRevealEvents(
                    context, actorUserId, gameId, revealedCardIds, null);
            return new TrainerEffectResult(
                    Map.of("effectType", EFFECT_TYPE, "cardsFound", 0),
                    List.copyOf(emptyEvents));
        }

        // Move chosen Pokemon to hand
        chosen.setZone(CardZone.HAND);
        chosen.setFaceDown(false);
        chosen.setZonePosition(
                gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND));
        gameCardInstanceStateService.save(chosen);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);

        // Shuffle the entire remaining deck
        shuffleDeck(gameId, actorUserId);

        List<GameEventDto> events = buildRevealEvents(
                context, actorUserId, gameId, revealedCardIds, chosen.getCardId().toString());

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("cardsFound", 1);
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    @Override
    public Map<String, Object> preview(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();

        List<GameCardInstance> deckCards = new ArrayList<>(gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK));
        deckCards.sort(Comparator.comparingInt(c -> c.getZonePosition() != null ? c.getZonePosition() : Integer.MAX_VALUE));

        List<GameCardInstance> topCards = deckCards.stream()
                .limit(LOOK_AT_TOP_N)
                .collect(Collectors.toList());

        List<Map<String, Object>> pokemonOptions = new ArrayList<>();
        for (GameCardInstance instance : topCards) {
            Card card = cardService.getCardEntityById(instance.getCardId());
            if (!isPokemon(card)) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("cardInstanceId", instance.getId().toString());
            option.put("cardId", card.getId().toString());
            option.put("name", card.getName());
            pokemonOptions.add(Map.copyOf(option));
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("cardsLookedAt", topCards.size());
        preview.put("pokemonOptions", List.copyOf(pokemonOptions));
        return Map.copyOf(preview);
    }

    private GameCardInstance findSelectedPokemon(List<GameCardInstance> topCards, UUID selectedInstanceId) {
        if (selectedInstanceId == null) {
            return null;
        }
        for (GameCardInstance c : topCards) {
            if (selectedInstanceId.equals(c.getId())) {
                Card card = cardService.getCardEntityById(c.getCardId());
                if (isPokemon(card)) {
                    return c;
                }
            }
        }
        return null;
    }

    private GameCardInstance findFirstPokemon(List<GameCardInstance> topCards) {
        for (GameCardInstance c : topCards) {
            Card card = cardService.getCardEntityById(c.getCardId());
            if (isPokemon(card)) {
                return c;
            }
        }
        return null;
    }

    private void shuffleDeck(UUID gameId, UUID actorUserId) {
        List<GameCardInstance> remaining = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        List<GameCardInstance> shuffled = gameRandomService.shuffledCopy(remaining);
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setZonePosition(i + 1);
        }
        gameCardInstanceStateService.saveAll(shuffled);
    }

    private List<GameEventDto> buildRevealEvents(
            TrainerEffectContext context,
            UUID actorUserId,
            UUID gameId,
            List<String> revealedCardIds,
            String takenCardId) {
        List<GameEventDto> events = new ArrayList<>();
        int cardsTaken = takenCardId != null ? 1 : 0;

        // Public: opponent knows how many cards were revealed, which one (if any) was taken,
        // and that the remaining deck was shuffled afterwards
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", actorUserId.toString());
        publicPayload.put("cardsRevealed", revealedCardIds.size());
        publicPayload.put("cardsTaken", cardsTaken);
        if (takenCardId != null) {
            publicPayload.put("takenCardId", takenCardId);
        }
        publicPayload.put("deckShuffled", true);
        publicPayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(), Map.copyOf(publicPayload)));

        // Private: actor sees all revealed card IDs and which one was taken
        Map<String, Object> privatePayload = new LinkedHashMap<>();
        privatePayload.put("playerId", actorUserId.toString());
        privatePayload.put("revealedCardIds", List.copyOf(revealedCardIds));
        if (takenCardId != null) {
            privatePayload.put("cardIds", List.of(takenCardId));
        }
        privatePayload.put("source", EVENT_SOURCE);
        events.add(gameEventFactory.privateEvent(
                gameId, GameEventType.CARD_DRAWN, context.stateVersion(),
                Map.copyOf(privatePayload), actorUserId));

        return events;
    }

    private boolean isPokemon(Card card) {
        return card != null && CardSupertype.POKEMON.equals(card.getSupertype());
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }

    private UUID optionalUuid(Map<String, Object> payload, String key) {
        Object value = payload != null ? payload.get(key) : null;
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
