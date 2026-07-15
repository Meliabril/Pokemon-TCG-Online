package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.SupporterLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrainerEffectServiceImpl implements TrainerEffectService {

    private static final String CARD_ID_KEY = "cardId";
    private static final String CARD_INSTANCE_ID_KEY = "cardInstanceId";

    private final List<TrainerEffect> trainerEffects;
    private final GameActionPayloadReader payloadReader;
    private final AvailableActionsFactory availableActionsFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;
    private final SupporterLockService supporterLockService;
    private final PassiveAbilityService passiveAbilityService;
    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;

    public TrainerEffectServiceImpl(
            List<TrainerEffect> trainerEffects,
            GameActionPayloadReader payloadReader,
            AvailableActionsFactory availableActionsFactory,
            GameCardInstanceStateService gameCardInstanceStateService,
            CardService cardService,
            GameEventFactory gameEventFactory,
            SupporterLockService supporterLockService,
            PassiveAbilityService passiveAbilityService,
            GameLookupService gameLookupService,
            GameStateQueryService gameStateQueryService) {
        if (trainerEffects == null) {
            this.trainerEffects = List.of();
        } else {
            this.trainerEffects = List.copyOf(trainerEffects);
        }
        this.payloadReader = payloadReader;
        this.availableActionsFactory = availableActionsFactory;
        this.gameCardInstanceStateService = gameCardInstanceStateService;
        this.cardService = cardService;
        this.gameEventFactory = gameEventFactory;
        this.supporterLockService = supporterLockService;
        this.passiveAbilityService = passiveAbilityService;
        this.gameLookupService = gameLookupService;
        this.gameStateQueryService = gameStateQueryService;
    }

    @Override
    public GameActionExecutionResult executeTrainer(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID gameId = context.gameId();
        GameStateDto currentState = context.currentState();
        int newStateVersion = currentState.stateVersion() + 1;

        GameCardInstance trainerCardInstance = findTrainerCardInHand(gameId, actorUserId, context.request().payload());
        Card trainerCard = cardService.getCardEntityById(trainerCardInstance.getCardId());
        validateTrainerCard(trainerCard, gameId, actorUserId, currentState);

        TrainerEffect trainerEffect = findTrainerEffect(trainerCard);
        TrainerEffectContext effectContext = new TrainerEffectContext(
                gameId,
                actorUserId,
                context.request(),
                currentState,
                trainerCardInstance,
                trainerCard,
                newStateVersion);
        TrainerEffectResult effectResult = trainerEffect.apply(effectContext);
        if (effectResult == null) {
            effectResult = TrainerEffectResult.empty();
        }

        if (!trainerEffect.keepsCardInPlay()) {
            moveResolvedTrainerToDiscard(gameId, actorUserId, trainerCardInstance);
        }

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.TRAINER_PLAYED,
                newStateVersion,
                trainerPlayedPayload(actorUserId, trainerCardInstance, trainerCard, effectResult.effectData())));
        events.addAll(effectResult.emittedEvents());

        boolean supporterPlayedThisTurn = currentState.turn().supporterPlayedThisTurn();
        if (CardCategory.SUPPORTER_TRAINER.equals(trainerCard.getCategory())) {
            supporterPlayedThisTurn = true;
        }
        GameStateDto newState = copyStateAfterTrainer(
                currentState,
                newStateVersion,
                supporterPlayedThisTurn,
                availableActionsFactory.mainPhaseActions(currentState.turn().energyAttachedThisTurn()),
                Instant.now());

        return new GameActionExecutionResult(newState, List.copyOf(events));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> previewTrainer(UUID gameId, UUID actorUserId, Map<String, Object> payload) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;

        Game game = gameLookupService.getRequiredGame(gameId);
        GameStateDto currentState = gameStateQueryService.buildVisibleState(game, actorUserId);

        GameCardInstance trainerCardInstance = findTrainerCardInHand(gameId, actorUserId, safePayload);
        Card trainerCard = cardService.getCardEntityById(trainerCardInstance.getCardId());
        validateTrainerCard(trainerCard, gameId, actorUserId, currentState);

        TrainerEffect trainerEffect = findTrainerEffect(trainerCard);
        GameActionRequestDto syntheticRequest = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                currentState.stateVersion(),
                safePayload);
        TrainerEffectContext effectContext = new TrainerEffectContext(
                gameId,
                actorUserId,
                syntheticRequest,
                currentState,
                trainerCardInstance,
                trainerCard,
                currentState.stateVersion());

        Map<String, Object> previewData = trainerEffect.preview(effectContext);
        if (previewData == null) {
            previewData = Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cardId", trainerCard.getId().toString());
        result.put("cardInstanceId", trainerCardInstance.getId().toString());
        result.put("externalId", trainerCard.getExternalId());
        result.put("category", trainerCard.getCategory().name());
        result.putAll(previewData);
        return Map.copyOf(result);
    }

    private GameCardInstance findTrainerCardInHand(UUID gameId, UUID actorUserId, Map<String, Object> payload) {
        Optional<UUID> cardInstanceId = payloadReader.optionalUuid(payload, CARD_INSTANCE_ID_KEY);
        if (cardInstanceId.isPresent()) {
            Optional<GameCardInstance> instanceResult = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                    cardInstanceId.get(),
                    gameId,
                    actorUserId);
            if (instanceResult.isEmpty() || !CardZone.HAND.equals(instanceResult.get().getZone())) {
                throw new InvalidGameActionException("Trainer card is not available in the player's hand");
            }

            return instanceResult.get();
        }

        UUID cardId = payloadReader.requiredUuid(payload, CARD_ID_KEY);
        Optional<GameCardInstance> handCardResult =
                gameCardInstanceStateService.findFirstByGameIdAndOwnerUserIdAndCardIdAndZone(
                        gameId,
                        actorUserId,
                        cardId,
                        CardZone.HAND);
        if (handCardResult.isEmpty()) {
            throw new InvalidGameActionException("Trainer card is not available in the player's hand");
        }

        return handCardResult.get();
    }

    private void validateTrainerCard(Card trainerCard, UUID gameId, UUID actorUserId, GameStateDto currentState) {
        if (trainerCard == null || !isTrainerCategory(trainerCard.getCategory())) {
            throw new InvalidGameActionException("Only Trainer cards can be played with PLAY_TRAINER");
        }
        if (!isSupportedTrainerCategory(trainerCard.getCategory())) {
            throw new InvalidGameActionException("Only Item, Supporter and Stadium Trainer cards are supported by the current Trainer engine");
        }
        if (CardCategory.ITEM_TRAINER.equals(trainerCard.getCategory())
                && passiveAbilityService.blocksItemCards(gameLookupService.getRequiredGame(gameId), actorUserId)) {
            throw new InvalidGameActionException("Item cards cannot be played while the opponent's Active Trevenant is in play");
        }
        if (CardCategory.SUPPORTER_TRAINER.equals(trainerCard.getCategory())) {
            if (supporterLockService.isSupporterLocked(gameId, actorUserId, currentState.turn().turnNumber())) {
                throw new InvalidGameActionException("Supporter cards cannot be played this turn");
            }
            if (currentState.turn().supporterPlayedThisTurn()) {
                throw new InvalidGameActionException("Supporter already played this turn");
            }
        }
    }

    private boolean isTrainerCategory(CardCategory category) {
        return CardCategory.ITEM_TRAINER.equals(category)
                || CardCategory.SUPPORTER_TRAINER.equals(category)
                || CardCategory.STADIUM_TRAINER.equals(category)
                || CardCategory.POKEMON_TOOL_TRAINER.equals(category);
    }

    private boolean isSupportedTrainerCategory(CardCategory category) {
        return CardCategory.ITEM_TRAINER.equals(category)
                || CardCategory.SUPPORTER_TRAINER.equals(category)
                || CardCategory.POKEMON_TOOL_TRAINER.equals(category)
                || CardCategory.STADIUM_TRAINER.equals(category);
    }

    private TrainerEffect findTrainerEffect(Card trainerCard) {
        for (TrainerEffect trainerEffect : trainerEffects) {
            if (trainerEffect.supports(trainerCard)) {
                return trainerEffect;
            }
        }

        throw new InvalidGameActionException("No Trainer effect is registered for card " + trainerCard.getExternalId());
    }

    private void moveResolvedTrainerToDiscard(UUID gameId, UUID actorUserId, GameCardInstance trainerCardInstance) {
        trainerCardInstance.setZone(CardZone.DISCARD);
        trainerCardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(
                gameId,
                actorUserId,
                CardZone.DISCARD));
        trainerCardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(trainerCardInstance);
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.HAND);
    }

    private Map<String, Object> trainerPlayedPayload(
            UUID actorUserId,
            GameCardInstance trainerCardInstance,
            Card trainerCard,
            Map<String, Object> effectData) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", actorUserId.toString());
        payload.put("cardId", trainerCard.getId().toString());
        payload.put("cardInstanceId", trainerCardInstance.getId().toString());
        payload.put("externalId", trainerCard.getExternalId());
        payload.put("category", trainerCard.getCategory().name());
        if (effectData != null) {
            payload.putAll(effectData);
        }

        return Map.copyOf(payload);
    }

    private GameStateDto copyStateAfterTrainer(
            GameStateDto sourceState,
            int stateVersion,
            boolean supporterPlayedThisTurn,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        TurnContextDto newTurn = sourceState.turn().toBuilder()
                .supporterPlayedThisTurn(supporterPlayedThisTurn)
                .build();
        ActionStateDto newActions = sourceState.actions().toBuilder()
                .availableActions(availableActions)
                .build();
        return sourceState.toBuilder()
                .stateVersion(stateVersion)
                .turn(newTurn)
                .actions(newActions)
                .updatedAt(updatedAt)
                .build();
    }

}
