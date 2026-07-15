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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RevivePokemonFromDiscardTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "REVIVE_POKEMON_FROM_DISCARD";
    private static final String TARGET_CARD_INSTANCE_ID_KEY = "targetCardInstanceId";

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
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

        UUID pokemonInstanceId = requiredUuid(context.request().payload(), TARGET_CARD_INSTANCE_ID_KEY);

        GameCardInstance pokemonInstance = gameCardInstanceStateService
                .findByIdAndGameIdAndOwnerUserId(pokemonInstanceId, gameId, actorUserId)
                .orElseThrow(() -> new InvalidGameActionException("Pokemon card not found in your discard pile"));

        if (!CardZone.DISCARD.equals(pokemonInstance.getZone())) {
            throw new InvalidGameActionException("Pokemon card must be in the discard pile");
        }

        Card pokemonCard = cardService.getCardEntityById(pokemonInstance.getCardId());
        if (pokemonCard == null || !CardSupertype.POKEMON.equals(pokemonCard.getSupertype())) {
            throw new InvalidGameActionException("Only Pokemon cards can be revived with Max Revive");
        }
        // Max Revive does not restrict revival to Basic-stage Pokemon: Basic, Stage 1 and
        // Stage 2 Pokemon already in the discard pile are all valid targets.

        // Shift all DECK cards one position down to make room at position 1 (top)
        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        for (GameCardInstance deckCard : deckCards) {
            if (deckCard.getZonePosition() != null) {
                deckCard.setZonePosition(deckCard.getZonePosition() + 1);
            }
        }
        gameCardInstanceStateService.saveAll(deckCards);

        // Place the Pokemon face-down on top of the deck (position 1)
        pokemonInstance.setZone(CardZone.DECK);
        pokemonInstance.setZonePosition(1);
        pokemonInstance.setFaceDown(true);
        gameCardInstanceStateService.save(pokemonInstance);

        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.DISCARD);

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", actorUserId.toString());
        publicPayload.put("cardId", pokemonInstance.getCardId().toString());
        publicPayload.put("source", EFFECT_TYPE);
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.CARD_PLAYED, context.stateVersion(),
                Map.copyOf(publicPayload)));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("cardId", pokemonInstance.getCardId().toString());
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    private boolean isItemOrSupporter(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }

    private UUID requiredUuid(Map<String, Object> payload, String key) {
        Object value = payload != null ? payload.get(key) : null;
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new InvalidGameActionException("Payload field '" + key + "' must be a valid UUID");
    }
}
