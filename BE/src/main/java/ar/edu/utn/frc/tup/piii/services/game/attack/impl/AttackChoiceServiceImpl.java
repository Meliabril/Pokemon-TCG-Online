package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackChoiceService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationRequest;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculatorService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackService;
import ar.edu.utn.frc.tup.piii.services.game.attack.PendingChoiceTimeoutPolicy;
import ar.edu.utn.frc.tup.piii.services.game.attack.SpecialConditionApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackChoiceServiceImpl implements AttackChoiceService {

    private static final String CONFIRM_KEY = "confirm";
    private static final String CARD_INSTANCE_ID_KEY = "cardInstanceId";
    private static final String CARD_INSTANCE_IDS_KEY = "cardInstanceIds";
    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";
    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";
    private static final String ATTACHED_CARD_ID_KEY = "attachedCardId";
    private static final String ATTACK_ORDER_KEY = "attackOrder";
    private static final String CONDITION_TYPE_KEY = "conditionType";
    private static final String BEFORE_DAMAGE_CONTINUATION = "BEFORE_DAMAGE";
    private static final String MAGMA_MANTLE_EFFECT = "MAGMA_MANTLE";
    private static final String SEARCH_ENERGY_FROM_DECK_TO_BENCH_EFFECT = "SEARCH_ENERGY_FROM_DECK_TO_BENCH";
    private static final String SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND_EFFECT = "SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND";
    private static final String TURN_TIMEOUT_REASON = "TURN_TIMEOUT";
    private static final int MAX_ENERGY_SALON_CARDS = 3;
    private static final int FIRST_TEMPORARY_ZONE_POSITION = -1;

    private final AttackService attackService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;
    private final DamageApplicationService damageApplicationService;
    private final DamageCalculatorService damageCalculatorService;
    private final SpecialConditionApplicationService specialConditionApplicationService;
    private final CombatResolutionService combatResolutionService;
    private final GameLookupService gameLookupService;
    private final GameStateQueryService gameStateQueryService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public GameActionExecutionResult resolveAttackChoice(GameActionContext context) {
        return resolveAttackChoice(context, false);
    }

    @Override
    public GameActionExecutionResult resolveAttackChoiceForTimeout(GameActionContext context) {
        ResolutionStateDto resolutionState = context.currentState().resolution();
        if (!resolutionState.hasPendingAttackChoice()) {
            throw new InvalidGameActionException("No pending attack choice is available for the acting player");
        }

        Map<String, Object> timeoutPayload = timeoutPayloadFor(resolutionState);
        GameActionRequestDto timeoutRequest = new GameActionRequestDto(
                context.gameId(),
                context.request().clientActionId(),
                GameActionType.RESOLVE_ATTACK_CHOICE,
                context.currentState().stateVersion(),
                timeoutPayload);
        GameActionContext timeoutContext = new GameActionContext(
                context.gameId(),
                resolutionState.pendingChoicePlayerId(),
                timeoutRequest,
                context.currentState());
        return resolveAttackChoice(timeoutContext, true);
    }

    private GameActionExecutionResult resolveAttackChoice(GameActionContext context, boolean autoResolved) {
        ResolutionStateDto resolutionState = context.currentState().resolution();
        if (!resolutionState.hasPendingAttackChoice()) {
            throw new InvalidGameActionException("No pending attack choice is available for the acting player");
        }
        UUID resolvingPlayerId = resolutionState.pendingChoicePlayerId();
        if (!context.actorUserId().equals(resolvingPlayerId)) {
            throw new InvalidGameActionException("Only the pending choice owner can resolve this attack choice");
        }
        UUID defenderUserId = resolutionState.nextActivePlayerId();
        UUID turnEndingPlayerId = resolutionState.effectiveTurnEndingPlayerId();
        String choiceType = resolutionState.pendingChoiceType();
        boolean confirm = Boolean.TRUE.equals(context.request().payload().get(CONFIRM_KEY));
        boolean skipped = isExplicitSkip(context.request().payload(), resolutionState);
        int newStateVersion = context.currentState().stateVersion() + 1;

        List<GameEventDto> events = new ArrayList<>();
        ChoiceApplicationResult applicationResult = skipped
                ? ChoiceApplicationResult.empty()
                : applyChoice(context, resolutionState, choiceType, confirm, newStateVersion);
        events.addAll(applicationResult.events());
        events.add(resolvedEvent(context.gameId(), resolvingPlayerId, choiceType, !skipped, autoResolved, newStateVersion));

        if (applicationResult.gameFinished() || applicationResult.promotionPending()) {
            return resultFromCurrentGame(context.gameId(), newStateVersion, events);
        }

        return attackService.finishAttackTurn(context, turnEndingPlayerId, defenderUserId, newStateVersion, events);
    }

    private GameEventDto resolvedEvent(
            UUID gameId,
            UUID resolvingPlayerId,
            String choiceType,
            boolean confirmed,
            boolean autoResolved,
            int stateVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", resolvingPlayerId.toString());
        payload.put("choiceType", choiceType);
        payload.put("confirmed", confirmed);
        if (autoResolved) {
            payload.put("autoResolved", true);
            payload.put("reason", TURN_TIMEOUT_REASON);
        }
        return gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_CHOICE_RESOLVED,
                stateVersion,
                Map.copyOf(payload));
    }

    private boolean isExplicitSkip(Map<String, Object> requestPayload, ResolutionStateDto resolutionState) {
        if (!isOptional(resolutionState)) {
            return false;
        }
        if (PendingAttackChoiceEffect.YES_NO.equals(resolutionState.pendingChoiceType())) {
            return false;
        }
        return requestPayload.containsKey(CONFIRM_KEY) && Boolean.FALSE.equals(requestPayload.get(CONFIRM_KEY));
    }

    private Map<String, Object> timeoutPayloadFor(ResolutionStateDto resolutionState) {
        PendingChoiceTimeoutPolicy policy = timeoutPolicy(resolutionState);
        if (PendingChoiceTimeoutPolicy.AUTO_SKIP.equals(policy)) {
            return Map.of(CONFIRM_KEY, false);
        }
        if (PendingChoiceTimeoutPolicy.NONE.equals(policy)) {
            return Map.of(CONFIRM_KEY, false);
        }
        return legalTimeoutSelection(resolutionState);
    }

    private Map<String, Object> legalTimeoutSelection(ResolutionStateDto resolutionState) {
        String choiceType = resolutionState.pendingChoiceType();
        Map<String, Object> choicePayload = resolutionState.pendingChoicePayload();
        if (LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE.equals(choiceType)
                || PendingAttackChoiceEffect.YES_NO.equals(choiceType)) {
            return Map.of(CONFIRM_KEY, false);
        }
        if (PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET.equals(choiceType)) {
            return Map.of(TARGET_POKEMON_IN_PLAY_ID_KEY, randomUuidOption(choicePayload, "targets", "pokemonInPlayId"));
        }
        if (PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK.equals(choiceType)) {
            return Map.of(ATTACK_ORDER_KEY, randomIntOption(choicePayload, "attacks", ATTACK_ORDER_KEY));
        }
        if (PendingAttackChoiceEffect.REORDER_TOP_DECK.equals(choiceType)) {
            return Map.of(CARD_INSTANCE_IDS_KEY, cardInstanceIdsFromPayload(choicePayload));
        }
        if (PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF.equals(choiceType)) {
            return Map.of(CARD_INSTANCE_ID_KEY, randomUuidOption(choicePayload, "cards", CARD_INSTANCE_ID_KEY));
        }
        if (PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH.equals(choiceType)) {
            return Map.of(
                    ATTACHED_CARD_ID_KEY, randomUuidOption(choicePayload, "energies", ATTACHED_CARD_ID_KEY),
                    TARGET_POKEMON_IN_PLAY_ID_KEY, randomUuidOption(choicePayload, "targets", "pokemonInPlayId"));
        }
        if (PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON.equals(choiceType)) {
            return Map.of(
                    CARD_INSTANCE_ID_KEY, randomUuidOption(choicePayload, "cards", CARD_INSTANCE_ID_KEY),
                    POKEMON_IN_PLAY_ID_KEY, randomUuidOption(choicePayload, "targets", "pokemonInPlayId"));
        }
        if (PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND.equals(choiceType)) {
            return Map.of(CARD_INSTANCE_IDS_KEY, distinctBasicEnergyTimeoutSelection(choicePayload));
        }
        if (OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD.equals(choiceType)) {
            return Map.of(CARD_INSTANCE_IDS_KEY, randomUuidOptions(
                    choicePayload,
                    "cards",
                    CARD_INSTANCE_ID_KEY,
                    intValue(choicePayload.get("discardCount"), 0)));
        }
        if (PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND.equals(choiceType)) {
            return Map.of(CARD_INSTANCE_IDS_KEY, randomUuidOptions(
                    choicePayload,
                    "cards",
                    CARD_INSTANCE_ID_KEY,
                    intValue(choicePayload.get("pickupCount"), 0)));
        }
        throw new InvalidGameActionException("Unsupported pending attack choice timeout policy: " + choiceType);
    }

    private PendingChoiceTimeoutPolicy timeoutPolicy(ResolutionStateDto resolutionState) {
        Object value = resolutionState.pendingChoicePayload().get(PendingAttackChoiceEffect.TIMEOUT_POLICY_KEY);
        if (value instanceof String text && !text.isBlank()) {
            return PendingChoiceTimeoutPolicy.valueOf(text);
        }
        if (isOptionalChoiceType(resolutionState.pendingChoiceType())) {
            return PendingChoiceTimeoutPolicy.AUTO_SKIP;
        }
        if (PendingAttackChoiceEffect.REORDER_TOP_DECK.equals(resolutionState.pendingChoiceType())) {
            return PendingChoiceTimeoutPolicy.AUTO_RESOLVE_DEFAULT;
        }
        return PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL;
    }

    private boolean isOptional(ResolutionStateDto resolutionState) {
        Object value = resolutionState.pendingChoicePayload().get(PendingAttackChoiceEffect.OPTIONAL_KEY);
        if (value instanceof Boolean optional) {
            return optional;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return isOptionalChoiceType(resolutionState.pendingChoiceType());
    }

    private boolean isOptionalChoiceType(String choiceType) {
        return LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE.equals(choiceType)
                || PendingAttackChoiceEffect.YES_NO.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD.equals(choiceType)
                || PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND.equals(choiceType)
                || PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND.equals(choiceType);
    }

    private UUID randomUuidOption(Map<String, Object> choicePayload, String optionsKey, String idKey) {
        List<UUID> selectedIds = randomUuidOptions(choicePayload, optionsKey, idKey, 1);
        if (selectedIds.isEmpty()) {
            throw new InvalidGameActionException("No legal timeout option is available");
        }
        return selectedIds.getFirst();
    }

    private int randomIntOption(Map<String, Object> choicePayload, String optionsKey, String idKey) {
        Object optionsValue = choicePayload.get(optionsKey);
        if (!(optionsValue instanceof List<?> options) || options.isEmpty()) {
            throw new InvalidGameActionException("No legal timeout option is available");
        }
        List<Object> shuffledOptions = gameRandomService.shuffledCopy(new ArrayList<>(options));
        Object firstOption = shuffledOptions.getFirst();
        if (firstOption instanceof Map<?, ?> option) {
            return intValue(option.get(idKey), -1);
        }
        throw new InvalidGameActionException("Invalid legal timeout option");
    }

    private List<UUID> randomUuidOptions(
            Map<String, Object> choicePayload,
            String optionsKey,
            String idKey,
            int count) {
        if (count <= 0) {
            return List.of();
        }
        Object optionsValue = choicePayload.get(optionsKey);
        if (!(optionsValue instanceof List<?> options) || options.isEmpty()) {
            return List.of();
        }
        List<Object> shuffledOptions = gameRandomService.shuffledCopy(new ArrayList<>(options));
        List<UUID> ids = new ArrayList<>();
        for (Object optionValue : shuffledOptions) {
            if (!(optionValue instanceof Map<?, ?> option)) {
                continue;
            }
            UUID id = uuidValue(option.get(idKey));
            if (id == null) {
                continue;
            }
            ids.add(id);
            if (ids.size() == count) {
                break;
            }
        }
        return List.copyOf(ids);
    }

    private List<UUID> distinctBasicEnergyTimeoutSelection(Map<String, Object> choicePayload) {
        List<UUID> selectedIds = new ArrayList<>();
        Set<String> selectedEnergyTypes = new HashSet<>();
        Object cardsValue = choicePayload.get("cards");
        if (!(cardsValue instanceof List<?> cards)) {
            return List.of();
        }
        List<Object> shuffledCards = gameRandomService.shuffledCopy(new ArrayList<>(cards));
        int maxCards = intValue(choicePayload.get("maxCards"), MAX_ENERGY_SALON_CARDS);
        for (Object cardValue : shuffledCards) {
            if (!(cardValue instanceof Map<?, ?> card)) {
                continue;
            }
            UUID cardInstanceId = uuidValue(card.get(CARD_INSTANCE_ID_KEY));
            String energyType = stringValue(card.get("energyType"));
            if (cardInstanceId == null || energyType == null || selectedEnergyTypes.contains(energyType)) {
                continue;
            }
            selectedEnergyTypes.add(energyType);
            selectedIds.add(cardInstanceId);
            if (selectedIds.size() == maxCards) {
                break;
            }
        }
        return List.copyOf(selectedIds);
    }

    private ChoiceApplicationResult applyChoice(
            GameActionContext context,
            ResolutionStateDto resolutionState,
            String choiceType,
            boolean confirm,
            int stateVersion) {
        UUID gameId = context.gameId();
        UUID defenderUserId = resolutionState.nextActivePlayerId();
        Map<String, Object> choicePayload = resolutionState.pendingChoicePayload();
        if (LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE.equals(choiceType)) {
            if (confirm) {
                shuffleDeck(gameId, defenderUserId);
            }
            return ChoiceApplicationResult.empty();
        }
        if (PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET.equals(choiceType)) {
            return damageSelectedOpponentBench(context, choicePayload, stateVersion);
        }
        if (PendingAttackChoiceEffect.SELECT_OPPONENT_ATTACK.equals(choiceType)) {
            return blockSelectedAttack(context, choicePayload, stateVersion);
        }
        if (PendingAttackChoiceEffect.YES_NO.equals(choiceType)) {
            return resolveYesNo(context, choicePayload, confirm, stateVersion);
        }
        if (PendingAttackChoiceEffect.REORDER_TOP_DECK.equals(choiceType)) {
            reorderTopDeck(context, choicePayload);
            return ChoiceApplicationResult.empty();
        }
        if (PendingAttackChoiceEffect.SELECT_CARD_FROM_DISCARD.equals(choiceType)) {
            recycleDiscardCard(context, choicePayload);
            return ChoiceApplicationResult.empty();
        }
        if (PendingAttackChoiceEffect.MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH.equals(choiceType)) {
            moveOpponentEnergy(context, choicePayload);
            return ChoiceApplicationResult.empty();
        }
        if (PendingAttackChoiceEffect.SELECT_DECK_CARD_AND_ATTACH_TO_SELF.equals(choiceType)) {
            AttachedDeckEnergyResult attachedEnergy = attachSelectedDeckEnergy(
                    context,
                    uuidFromPayload(choicePayload, "targetPokemonInPlayId"),
                    choicePayload);
            shuffleDeck(gameId, context.actorUserId());
            return ChoiceApplicationResult.withEvents(List.of(deckEnergyAttachedEvent(
                    context.gameId(),
                    context.actorUserId(),
                    attachedEnergy,
                    stateVersion)));
        }
        if (PendingAttackChoiceEffect.SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON.equals(choiceType)) {
            AttachedDeckEnergyResult attachedEnergy = attachSelectedDeckEnergy(
                    context,
                    requiredUuid(context.request().payload(), POKEMON_IN_PLAY_ID_KEY),
                    choicePayload);
            shuffleDeck(gameId, context.actorUserId());
            return ChoiceApplicationResult.withEvents(List.of(deckEnergyAttachedEvent(
                    context.gameId(),
                    context.actorUserId(),
                    attachedEnergy,
                    stateVersion)));
        }
        if (PendingAttackChoiceEffect.SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND.equals(choiceType)) {
            List<UUID> movedCardInstanceIds = moveDistinctBasicEnergiesToHand(context);
            shuffleDeck(gameId, context.actorUserId());
            return ChoiceApplicationResult.withEvents(List.of(distinctBasicEnergiesMovedToHandEvent(
                    context.gameId(),
                    context.actorUserId(),
                    movedCardInstanceIds,
                    stateVersion)));
        }
        if (OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD.equals(choiceType)) {
            discardSelectedHandCards(context, choicePayload);
            return ChoiceApplicationResult.empty();
        }
        if (PendingAttackChoiceEffect.SELECT_DISCARD_ITEMS_TO_HAND.equals(choiceType)) {
            moveSelectedDiscardItemsToHand(context, choicePayload);
            return ChoiceApplicationResult.empty();
        }
        if (ChooseSpecialConditionAttackEffect.CHOICE_TYPE.equals(choiceType)) {
            return applyChosenSpecialCondition(context, choicePayload, stateVersion);
        }

        throw new InvalidGameActionException("Unsupported pending attack choice type: " + choiceType);
    }

    private ChoiceApplicationResult applyChosenSpecialCondition(
            GameActionContext context,
            Map<String, Object> choicePayload,
            int stateVersion) {
        String selectedConditionType = stringValue(context.request().payload().get(CONDITION_TYPE_KEY));
        if (!stringInOptions(
                choicePayload,
                ChooseSpecialConditionAttackEffect.CONDITION_TYPES_KEY,
                selectedConditionType)) {
            throw new InvalidGameActionException("Selected special condition was not offered for this choice");
        }

        UUID defenderPokemonId = uuidFromPayload(choicePayload, "defenderPokemonInPlayId");
        UUID defenderUserId = context.currentState().resolution().nextActivePlayerId();
        PokemonInPlay defenderPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                defenderPokemonId,
                context.gameId(),
                defenderUserId)
                .orElseThrow(() -> new InvalidGameActionException("Defending Pokemon is no longer valid"));

        List<GameEventDto> events = specialConditionApplicationService.applyCondition(
                context.gameId(),
                context.actorUserId(),
                defenderPokemon,
                conditionType(selectedConditionType),
                context.currentState().resolution().nextTurnNumber() - 1,
                stateVersion);
        return ChoiceApplicationResult.withEvents(events);
    }

    private ChoiceApplicationResult damageSelectedOpponentBench(
            GameActionContext context,
            Map<String, Object> choicePayload,
            int stateVersion) {
        UUID targetPokemonId = requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);
        if (!uuidInOptions(choicePayload, "targets", "pokemonInPlayId", targetPokemonId)) {
            throw new InvalidGameActionException("Selected Bench Pokemon was not offered for this choice");
        }
        UUID defenderUserId = context.currentState().resolution().nextActivePlayerId();
        PokemonInPlay targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonId,
                context.gameId(),
                defenderUserId)
                .filter(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0)
                .orElseThrow(() -> new InvalidGameActionException("Selected Bench Pokemon is no longer valid"));
        int damage = intValue(choicePayload.get("damage"), 0);
        int damageCounters = damageApplicationService.applyDamage(targetPokemon, damage);
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", damage,
                        "damageCounters", damageCounters,
                        "reason", PendingAttackChoiceEffect.SELECT_OPPONENT_BENCH_TARGET)));
        CombatResolutionService.CombatResolutionResult combatResult = combatResolutionService.resolveKnockoutIfNeeded(
                context.gameId(),
                defenderUserId,
                context.actorUserId(),
                targetPokemon,
                defenderUserId,
                context.currentState().resolution().nextTurnNumber(),
                stateVersion,
                "BENCH_ATTACK_DAMAGE");
        events.addAll(combatResult.events());
        return new ChoiceApplicationResult(combatResult.gameFinished(), combatResult.promotionPending(), events);
    }

    private ChoiceApplicationResult blockSelectedAttack(
            GameActionContext context,
            Map<String, Object> choicePayload,
            int stateVersion) {
        int attackOrder = intValue(context.request().payload().get(ATTACK_ORDER_KEY), -1);
        if (!intInOptions(choicePayload, "attacks", ATTACK_ORDER_KEY, attackOrder)) {
            throw new InvalidGameActionException("Selected attack was not offered for this choice");
        }
        UUID defenderPokemonId = uuidFromPayload(choicePayload, "defenderPokemonInPlayId");
        PokemonInPlay defenderPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                defenderPokemonId,
                context.gameId(),
                context.currentState().resolution().nextActivePlayerId())
                .orElseThrow(() -> new InvalidGameActionException("Defending Pokemon is no longer valid"));
        defenderPokemon.setBlockedAttackTurn(context.currentState().resolution().nextTurnNumber());
        defenderPokemon.setBlockedAttackOrder(attackOrder);
        pokemonInPlayStateService.save(defenderPokemon);
        GameEventDto event = gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", "BLOCK_ATTACK",
                        "pokemonInPlayId", defenderPokemon.getId().toString(),
                        "attackOrder", attackOrder));
        return new ChoiceApplicationResult(false, false, List.of(event));
    }

    private ChoiceApplicationResult resolveYesNo(
            GameActionContext context,
            Map<String, Object> choicePayload,
            boolean confirm,
            int stateVersion) {
        if (!BEFORE_DAMAGE_CONTINUATION.equals(stringValue(choicePayload.get("continuation")))
                || !MAGMA_MANTLE_EFFECT.equals(stringValue(choicePayload.get("effect")))) {
            return ChoiceApplicationResult.empty();
        }

        int bonusDamage = 0;
        List<GameEventDto> events = new ArrayList<>();
        if (confirm) {
            DiscardedTopCardResult discardedTopCard = discardTopDeck(context.gameId(), context.actorUserId(), stateVersion);
            events.addAll(discardedTopCard.events());
            if (discardedTopCard.card() != null
                    && matchesEnergyType(discardedTopCard.card(), stringValue(choicePayload.get("requiredEnergyType")))) {
                bonusDamage = intValue(choicePayload.get("bonusDamage"), 0);
            }
        }

        PokemonInPlay targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                uuidFromPayload(choicePayload, "targetPokemonInPlayId"),
                context.gameId(),
                context.currentState().resolution().nextActivePlayerId())
                .orElseThrow(() -> new InvalidGameActionException("Attack target is no longer valid"));
        Card attackerCard = cardService.getCardEntityById(uuidFromPayload(choicePayload, "attackerCardId"));
        Card defenderCard = cardService.getCardEntityById(targetPokemon.getActiveCardInstance().getCardId());
        Attack selectedAttack = attackByOrder(attackerCard, intValue(choicePayload.get("attackOrder"), -1));
        DamageCalculationResult damageResult = damageCalculatorService.calculateDamage(new DamageCalculationRequest(
                gameLookupService.getRequiredGame(context.gameId()),
                selectedAttack,
                attackerCard,
                defenderCard,
                targetPokemon,
                bonusDamage,
                0));
        int damageCounters = damageApplicationService.applyDamage(targetPokemon, damageResult.finalDamage());
        events.add(gameEventFactory.publicEvent(
                context.gameId(),
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", damageResult.finalDamage(),
                        "damageCounters", damageCounters)));

        CombatResolutionService.CombatResolutionResult combatResult = combatResolutionService.resolveKnockoutIfNeeded(
                context.gameId(),
                targetPokemon.getOwnerUserId(),
                context.actorUserId(),
                targetPokemon,
                context.currentState().resolution().nextActivePlayerId(),
                context.currentState().resolution().nextTurnNumber(),
                stateVersion,
                "ATTACK_DAMAGE");
        events.addAll(combatResult.events());
        return new ChoiceApplicationResult(combatResult.gameFinished(), combatResult.promotionPending(), events);
    }

    private void reorderTopDeck(GameActionContext context, Map<String, Object> choicePayload) {
        List<UUID> requestedIds = uuidList(context.request().payload().get(CARD_INSTANCE_IDS_KEY));
        List<UUID> expectedIds = cardInstanceIdsFromPayload(choicePayload);
        if (requestedIds.size() != expectedIds.size() || !Set.copyOf(requestedIds).equals(Set.copyOf(expectedIds))) {
            throw new InvalidGameActionException("Top deck reorder must contain exactly the offered cards");
        }
        int position = 1;
        for (UUID cardInstanceId : requestedIds) {
            GameCardInstance card = findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.DECK);
            card.setZonePosition(position++);
            gameCardInstanceStateService.save(card);
        }
    }

    private void recycleDiscardCard(GameActionContext context, Map<String, Object> choicePayload) {
        UUID cardInstanceId = requiredUuid(context.request().payload(), CARD_INSTANCE_ID_KEY);
        if (!uuidInOptions(choicePayload, "cards", "cardInstanceId", cardInstanceId)) {
            throw new InvalidGameActionException("Selected discard card was not offered for this choice");
        }
        GameCardInstance card = findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.DISCARD);
        shiftDeckDown(context.gameId(), context.actorUserId());
        card.setZone(CardZone.DECK);
        card.setZonePosition(1);
        card.setFaceDown(true);
        gameCardInstanceStateService.save(card);
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.DECK);
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.DISCARD);
    }

    private void moveOpponentEnergy(GameActionContext context, Map<String, Object> choicePayload) {
        UUID attachedCardId = requiredUuid(context.request().payload(), ATTACHED_CARD_ID_KEY);
        UUID targetPokemonId = requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);
        if (!uuidInOptions(choicePayload, "energies", ATTACHED_CARD_ID_KEY, attachedCardId)) {
            throw new InvalidGameActionException("Selected Energy was not offered for this choice");
        }
        if (!uuidInOptions(choicePayload, "targets", "pokemonInPlayId", targetPokemonId)) {
            throw new InvalidGameActionException("Selected Bench Pokemon was not offered for this choice");
        }
        UUID opponentUserId = context.currentState().resolution().nextActivePlayerId();
        PokemonInPlay targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonId,
                context.gameId(),
                opponentUserId)
                .filter(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0)
                .orElseThrow(() -> new InvalidGameActionException("Target Bench Pokemon is no longer valid"));
        PokemonAttachedCard attachedCard = pokemonAttachedCardStateService.findByGameCardInstanceId(attachedCardId)
                .orElseThrow(() -> new InvalidGameActionException("Selected attached Energy is no longer valid"));
        if (!isEnergy(attachedCard) || !opponentUserId.equals(attachedCard.getPokemonInPlay().getOwnerUserId())) {
            throw new InvalidGameActionException("Selected attached card is not an opponent Energy");
        }
        if (attachedCard.getPokemonInPlay().getSlotPosition() == null
                || attachedCard.getPokemonInPlay().getSlotPosition() != 0) {
            throw new InvalidGameActionException("Selected Energy is not attached to the opponent Active Pokemon");
        }
        attachedCard.setPokemonInPlay(targetPokemon);
        pokemonAttachedCardStateService.save(attachedCard);
    }

    private AttachedDeckEnergyResult attachSelectedDeckEnergy(
            GameActionContext context,
            UUID targetPokemonId,
            Map<String, Object> choicePayload) {
        UUID cardInstanceId = requiredUuid(context.request().payload(), CARD_INSTANCE_ID_KEY);
        if (!uuidInOptions(choicePayload, "cards", "cardInstanceId", cardInstanceId)) {
            throw new InvalidGameActionException("Selected deck card was not offered for this choice");
        }
        GameCardInstance card = findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.DECK);
        if (!uuidInOptions(choicePayload, "targets", "pokemonInPlayId", targetPokemonId)
                && choicePayload.containsKey("targets")) {
            throw new InvalidGameActionException("Selected Pokemon was not offered for this choice");
        }
        PokemonInPlay targetPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonId,
                context.gameId(),
                context.actorUserId())
                .orElseThrow(() -> new InvalidGameActionException("Target Pokemon is no longer valid"));
        Card cardEntity = cardService.getCardEntityById(card.getCardId());
        if (!isEnergyCard(cardEntity)) {
            throw new InvalidGameActionException("Selected card is not an Energy card");
        }
        if (Boolean.TRUE.equals(choicePayload.get("basicOnly"))
                && !CardCategory.BASIC_ENERGY.equals(cardEntity.getCategory())) {
            throw new InvalidGameActionException("Selected card is not a Basic Energy card");
        }
        String requiredEnergyType = stringValue(choicePayload.get("requiredEnergyType"));
        if (requiredEnergyType != null && !requiredEnergyType.isBlank()
                && !matchesEnergyType(cardEntity, requiredEnergyType)) {
            throw new InvalidGameActionException("Selected Energy type is no longer valid");
        }
        card.setZone(CardZone.ATTACHED);
        card.setZonePosition(gameCardInstanceStateService.nextZonePosition(context.gameId(), context.actorUserId(), CardZone.ATTACHED));
        card.setFaceDown(false);
        gameCardInstanceStateService.save(card);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(targetPokemon);
        attachedCard.setGameCardInstance(card);
        attachedCard.setAttachedCardType(CardCategory.SPECIAL_ENERGY.equals(cardEntity.getCategory())
                ? AttachedCardType.SPECIAL_ENERGY
                : AttachedCardType.BASIC_ENERGY);
        pokemonAttachedCardStateService.save(attachedCard);
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.DECK);
        return new AttachedDeckEnergyResult(card.getId(), targetPokemon.getId());
    }

    private List<UUID> moveDistinctBasicEnergiesToHand(GameActionContext context) {
        List<UUID> cardInstanceIds = uuidList(context.request().payload().get(CARD_INSTANCE_IDS_KEY));
        if (cardInstanceIds.size() > MAX_ENERGY_SALON_CARDS || hasDuplicates(cardInstanceIds)) {
            throw new InvalidGameActionException("Energy Salon selection is invalid");
        }
        Set<String> energyTypes = new HashSet<>();
        List<GameCardInstance> cards = new ArrayList<>();
        for (UUID cardInstanceId : cardInstanceIds) {
            GameCardInstance card = findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.DECK);
            Card cardEntity = cardService.getCardEntityById(card.getCardId());
            if (!CardCategory.BASIC_ENERGY.equals(cardEntity.getCategory())) {
                throw new InvalidGameActionException("Energy Salon can only select Basic Energy cards");
            }
            String energyType = energyType(cardEntity);
            if (energyType == null || !energyTypes.add(energyType)) {
                throw new InvalidGameActionException("Energy Salon requires different Energy types");
            }
            cards.add(card);
        }
        for (GameCardInstance card : cards) {
            card.setZone(CardZone.HAND);
            card.setZonePosition(gameCardInstanceStateService.nextZonePosition(context.gameId(), context.actorUserId(), CardZone.HAND));
            card.setFaceDown(false);
            gameCardInstanceStateService.save(card);
        }
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.DECK);
        return List.copyOf(cardInstanceIds);
    }

    private void discardSelectedHandCards(GameActionContext context, Map<String, Object> choicePayload) {
        List<UUID> requestedIds = uuidList(context.request().payload().get(CARD_INSTANCE_IDS_KEY));
        int requiredCount = intValue(choicePayload.get("discardCount"), 0);
        if (requestedIds.size() != requiredCount || hasDuplicates(requestedIds)) {
            throw new InvalidGameActionException(
                    "Selected hand discard must contain exactly the required number of distinct cards");
        }

        List<GameCardInstance> cardsToDiscard = new ArrayList<>();
        for (UUID cardInstanceId : requestedIds) {
            if (!uuidInOptions(choicePayload, "cards", CARD_INSTANCE_ID_KEY, cardInstanceId)) {
                throw new InvalidGameActionException("Selected hand card was not offered for this choice");
            }
            cardsToDiscard.add(findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.HAND));
        }

        int position = gameCardInstanceStateService.nextZonePosition(context.gameId(), context.actorUserId(), CardZone.DISCARD);
        for (GameCardInstance card : cardsToDiscard) {
            card.setZone(CardZone.DISCARD);
            card.setZonePosition(position++);
            card.setFaceDown(false);
            gameCardInstanceStateService.save(card);
        }
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.HAND);
    }

    private void moveSelectedDiscardItemsToHand(GameActionContext context, Map<String, Object> choicePayload) {
        List<UUID> requestedIds = uuidList(context.request().payload().get(CARD_INSTANCE_IDS_KEY));
        int requiredCount = intValue(choicePayload.get("pickupCount"), 0);
        if (requestedIds.size() != requiredCount || hasDuplicates(requestedIds)) {
            throw new InvalidGameActionException(
                    "Pickup selection must contain exactly the required number of distinct Item cards");
        }

        List<GameCardInstance> cardsToMove = new ArrayList<>();
        for (UUID cardInstanceId : requestedIds) {
            if (!uuidInOptions(choicePayload, "cards", CARD_INSTANCE_ID_KEY, cardInstanceId)) {
                throw new InvalidGameActionException("Selected discard card was not offered for this choice");
            }
            cardsToMove.add(findOwnedZoneCard(context.gameId(), context.actorUserId(), cardInstanceId, CardZone.DISCARD));
        }

        int position = gameCardInstanceStateService.nextZonePosition(context.gameId(), context.actorUserId(), CardZone.HAND);
        for (GameCardInstance card : cardsToMove) {
            card.setZone(CardZone.HAND);
            card.setZonePosition(position++);
            card.setFaceDown(false);
            gameCardInstanceStateService.save(card);
        }
        gameCardInstanceStateService.resequenceZone(context.gameId(), context.actorUserId(), CardZone.DISCARD);
    }

    private void shiftDeckDown(UUID gameId, UUID ownerUserId) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                ownerUserId,
                CardZone.DECK);
        if (deckCards.isEmpty()) {
            return;
        }

        List<GameCardInstance> orderedDeckCards = new ArrayList<>(deckCards);
        orderedDeckCards.sort(Comparator.comparing(
                GameCardInstance::getZonePosition,
                Comparator.nullsLast(Integer::compareTo)));
        int temporaryPosition = FIRST_TEMPORARY_ZONE_POSITION;
        for (GameCardInstance deckCard : orderedDeckCards) {
            deckCard.setZonePosition(temporaryPosition--);
        }
        gameCardInstanceStateService.saveAll(orderedDeckCards);
        gameCardInstanceStateService.flush();

        int position = 2;
        for (GameCardInstance deckCard : orderedDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(orderedDeckCards);
    }

    private void shuffleDeck(UUID gameId, UUID ownerUserId) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                ownerUserId,
                CardZone.DECK);
        if (deckCards.size() < 2) {
            return;
        }

        List<GameCardInstance> shuffledDeckCards = gameRandomService.shuffledCopy(deckCards);
        List<GameCardInstance> cardsToSave = new ArrayList<>();
        int temporaryPosition = FIRST_TEMPORARY_ZONE_POSITION;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(temporaryPosition--);
            cardsToSave.add(deckCard);
        }
        gameCardInstanceStateService.saveAll(cardsToSave);
        gameCardInstanceStateService.flush();

        int position = 1;
        for (GameCardInstance deckCard : shuffledDeckCards) {
            deckCard.setZonePosition(position++);
        }
        gameCardInstanceStateService.saveAll(shuffledDeckCards);
    }

    private GameEventDto deckEnergyAttachedEvent(
            UUID gameId,
            UUID actorPlayerId,
            AttachedDeckEnergyResult attachedEnergy,
            int stateVersion) {
        return gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", SEARCH_ENERGY_FROM_DECK_TO_BENCH_EFFECT,
                        "actorPlayerId", actorPlayerId.toString(),
                        "attachedCount", 1,
                        "attachedPokemonInPlayIds", List.of(attachedEnergy.targetPokemonInPlayId().toString()),
                        "attachedCardInstanceIds", List.of(attachedEnergy.cardInstanceId().toString()),
                        "pokemonInPlayId", attachedEnergy.targetPokemonInPlayId().toString()));
    }

    private GameEventDto distinctBasicEnergiesMovedToHandEvent(
            UUID gameId,
            UUID actorPlayerId,
            List<UUID> movedCardInstanceIds,
            int stateVersion) {
        return gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", SEARCH_DISTINCT_BASIC_ENERGIES_TO_HAND_EFFECT,
                        "actorPlayerId", actorPlayerId.toString(),
                        "cardInstanceIds", movedCardInstanceIds.stream().map(UUID::toString).toList(),
                        "cardCount", movedCardInstanceIds.size()));
    }

    private DiscardedTopCardResult discardTopDeck(UUID gameId, UUID ownerUserId, int stateVersion) {
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                ownerUserId,
                CardZone.DECK);
        if (deckCards.isEmpty()) {
            return new DiscardedTopCardResult(null, List.of());
        }
        GameCardInstance card = deckCards.get(0);
        Card cardEntity = cardService.getCardEntityById(card.getCardId());
        card.setZone(CardZone.DISCARD);
        card.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DISCARD));
        card.setFaceDown(false);
        gameCardInstanceStateService.save(card);
        gameCardInstanceStateService.resequenceZone(gameId, ownerUserId, CardZone.DECK);
        GameEventDto event = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", "DISCARD_TOP_DECK",
                        "playerId", ownerUserId.toString(),
                        "discardedCardId", card.getCardId().toString()));
        return new DiscardedTopCardResult(cardEntity, List.of(event));
    }

    private GameActionExecutionResult resultFromCurrentGame(UUID gameId, int stateVersion, List<GameEventDto> events) {
        Game game = gameLookupService.getRequiredGame(gameId);
        GameStateDto state = gameStateQueryService.buildVisibleState(game).toBuilder()
                .stateVersion(stateVersion)
                .updatedAt(Instant.now())
                .build();
        return new GameActionExecutionResult(state, List.copyOf(events));
    }

    private GameCardInstance findOwnedZoneCard(UUID gameId, UUID ownerUserId, UUID cardInstanceId, CardZone zone) {
        Optional<GameCardInstance> card = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(
                cardInstanceId,
                gameId,
                ownerUserId);
        if (card.isEmpty() || !zone.equals(card.get().getZone())) {
            throw new InvalidGameActionException("Selected card is no longer valid");
        }
        return card.get();
    }

    private Attack attackByOrder(Card card, int attackOrder) {
        for (Attack attack : card.getAttacks()) {
            if (attack.getAttackOrder() == attackOrder) {
                return attack;
            }
        }
        throw new InvalidGameActionException("Selected attack is no longer valid");
    }

    private List<UUID> cardInstanceIdsFromPayload(Map<String, Object> choicePayload) {
        Object cardsValue = choicePayload.get("cards");
        if (!(cardsValue instanceof List<?> cards)) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (Object item : cards) {
            if (item instanceof Map<?, ?> map) {
                ids.add(uuidValue(map.get("cardInstanceId")));
            }
        }
        return List.copyOf(ids);
    }

    private boolean uuidInOptions(
            Map<String, Object> choicePayload,
            String optionsKey,
            String idKey,
            UUID expectedId) {
        Object optionsValue = choicePayload.get(optionsKey);
        if (!(optionsValue instanceof List<?> options)) {
            return false;
        }
        for (Object option : options) {
            if (option instanceof Map<?, ?> map && expectedId.equals(uuidValue(map.get(idKey)))) {
                return true;
            }
        }
        return false;
    }

    private boolean intInOptions(
            Map<String, Object> choicePayload,
            String optionsKey,
            String idKey,
            int expectedValue) {
        Object optionsValue = choicePayload.get(optionsKey);
        if (!(optionsValue instanceof List<?> options)) {
            return false;
        }
        for (Object option : options) {
            if (option instanceof Map<?, ?> map && intValue(map.get(idKey), -1) == expectedValue) {
                return true;
            }
        }
        return false;
    }

    private boolean stringInOptions(Map<String, Object> choicePayload, String optionsKey, String expectedValue) {
        if (expectedValue == null || expectedValue.isBlank()) {
            return false;
        }
        Object optionsValue = choicePayload.get(optionsKey);
        if (!(optionsValue instanceof List<?> options)) {
            return false;
        }
        for (Object option : options) {
            if (expectedValue.equalsIgnoreCase(stringValue(option))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDuplicates(List<UUID> values) {
        return values.size() != Set.copyOf(values).size();
    }

    private UUID requiredUuid(Map<String, Object> payload, String key) {
        UUID value = uuidValue(payload.get(key));
        if (value == null) {
            throw new InvalidGameActionException("Missing required payload field: " + key);
        }
        return value;
    }

    private UUID uuidFromPayload(Map<String, Object> payload, String key) {
        UUID value = uuidValue(payload.get(key));
        if (value == null) {
            throw new InvalidGameActionException("Missing required pending choice field: " + key);
        }
        return value;
    }

    private UUID uuidValue(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text && !text.isBlank()) {
            return UUID.fromString(text);
        }
        return null;
    }

    private List<UUID> uuidList(Object value) {
        if (!(value instanceof List<?> values)) {
            throw new InvalidGameActionException("Expected a list of card instance ids");
        }
        List<UUID> ids = new ArrayList<>();
        for (Object item : values) {
            UUID uuid = uuidValue(item);
            if (uuid == null) {
                throw new InvalidGameActionException("Invalid UUID value in payload");
            }
            ids.add(uuid);
        }
        return List.copyOf(ids);
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return defaultValue;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private SpecialConditionType conditionType(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidGameActionException("Missing required payload field: " + CONDITION_TYPE_KEY);
        }
        return SpecialConditionType.valueOf(value.toUpperCase());
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard.getAttachedCardType() == AttachedCardType.BASIC_ENERGY
                || attachedCard.getAttachedCardType() == AttachedCardType.SPECIAL_ENERGY;
    }

    private boolean isEnergyCard(Card card) {
        return card != null
                && (CardCategory.BASIC_ENERGY.equals(card.getCategory())
                || CardCategory.SPECIAL_ENERGY.equals(card.getCategory()));
    }

    private boolean matchesEnergyType(Card card, String energyType) {
        if (!isEnergyCard(card)) {
            return false;
        }
        String resolvedEnergyType = energyType(card);
        return resolvedEnergyType != null && energyType != null && energyType.equalsIgnoreCase(resolvedEnergyType);
    }

    private String energyType(Card card) {
        if (card.getPokemonType() != null) {
            return card.getPokemonType();
        }
        if (card.getName() != null && card.getName().endsWith(" Energy")) {
            return card.getName().replace(" Energy", "");
        }
        return null;
    }

    private record ChoiceApplicationResult(boolean gameFinished, boolean promotionPending, List<GameEventDto> events) {

        static ChoiceApplicationResult empty() {
            return new ChoiceApplicationResult(false, false, List.of());
        }

        static ChoiceApplicationResult withEvents(List<GameEventDto> events) {
            return new ChoiceApplicationResult(false, false, List.copyOf(events));
        }
    }

    private record DiscardedTopCardResult(Card card, List<GameEventDto> events) {
    }

    private record AttachedDeckEnergyResult(UUID cardInstanceId, UUID targetPokemonInPlayId) {
    }
}
