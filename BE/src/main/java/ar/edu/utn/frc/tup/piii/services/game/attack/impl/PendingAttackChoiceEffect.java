package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.PendingChoiceTimeoutPolicy;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingAttackChoiceEffect implements AttackEffect {

    public static final String SELECT_OPPONENT_BENCH_TARGET = "SELECT_OPPONENT_BENCH_TARGET";
    public static final String SELECT_OPPONENT_ATTACK = "SELECT_OPPONENT_ATTACK";
    public static final String YES_NO = "YES_NO";
    public static final String REORDER_TOP_DECK = "REORDER_TOP_DECK";
    public static final String SELECT_CARD_FROM_DISCARD = "SELECT_CARD_FROM_DISCARD";
    public static final String MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH = "MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH";
    public static final String SELECT_DECK_CARD_AND_ATTACH_TO_SELF = "SELECT_DECK_CARD_AND_ATTACH_TO_SELF";
    public static final String SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON =
            "SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON";
    public static final String SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND = "SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND";
    public static final String SELECT_DISCARD_ITEMS_TO_HAND = "SELECT_DISCARD_ITEMS_TO_HAND";
    public static final String OPTIONAL_KEY = "optional";
    public static final String TIMEOUT_POLICY_KEY = "timeoutPolicy";
    public static final String MIN_SELECTIONS_KEY = "minSelections";
    public static final String MAX_SELECTIONS_KEY = "maxSelections";

    private static final String EFFECT_KEY = "effect";
    private static final String CONTINUATION_KEY = "continuation";
    private static final String BEFORE_DAMAGE_CONTINUATION = "BEFORE_DAMAGE";
    private static final String MAGMA_MANTLE_EFFECT = "MAGMA_MANTLE";
    private static final String PICKUP_NO_VALID_CARDS_EFFECT = "PICKUP_NO_VALID_CARDS";

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        if (operation == null) {
            return false;
        }
        return SELECT_OPPONENT_BENCH_TARGET.equals(operation.type())
                || SELECT_OPPONENT_ATTACK.equals(operation.type())
                || YES_NO.equals(operation.type())
                || REORDER_TOP_DECK.equals(operation.type())
                || SELECT_CARD_FROM_DISCARD.equals(operation.type())
                || MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH.equals(operation.type())
                || SELECT_DECK_CARD_AND_ATTACH_TO_SELF.equals(operation.type())
                || SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON.equals(operation.type())
                || SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND.equals(operation.type())
                || SELECT_DISCARD_ITEMS_TO_HAND.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        String choiceType = context.operation().type();
        Map<String, Object> payload = switch (choiceType) {
            case SELECT_OPPONENT_BENCH_TARGET -> opponentBenchPayload(context);
            case SELECT_OPPONENT_ATTACK -> opponentAttackPayload(context);
            case YES_NO -> yesNoPayload(context);
            case REORDER_TOP_DECK -> reorderTopDeckPayload(context);
            case SELECT_CARD_FROM_DISCARD -> discardPayload(context);
            case MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH -> moveOpponentEnergyPayload(context);
            case SELECT_DECK_CARD_AND_ATTACH_TO_SELF -> deckCardAttachToSelfPayload(context);
            case SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON -> deckEnergyAttachToOwnPokemonPayload(context);
            case SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND -> distinctBasicEnergiesPayload(context);
            case SELECT_DISCARD_ITEMS_TO_HAND -> pickupChoicePayload(context);
            default -> Map.of();
        };
        if (payload.isEmpty() && !SELECT_DISCARD_ITEMS_TO_HAND.equals(choiceType)) {
            return AttackEffectResult.empty();
        }
        if (payload.isEmpty()) {
            return new AttackEffectResult(
                    0,
                    false,
                    List.of(gameEventFactory.publicEvent(
                            context.resolutionContext().gameId(),
                            GameEventType.ATTACK_EFFECT_RESOLVED,
                            context.stateVersion(),
                            Map.of(
                                    "effectType", PICKUP_NO_VALID_CARDS_EFFECT,
                                    "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                                    "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()))));
        }
        return AttackEffectResult.requiringChoice(choiceType, payload, List.of());
    }

    private Map<String, Object> opponentBenchPayload(AttackEffectContext context) {
        List<Map<String, Object>> targets = benchTargets(context, context.resolutionContext().defenderUserId());
        if (targets.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("targets", targets);
        payload.put("damage", context.operation().amount());
        return Map.copyOf(payload);
    }

    private Map<String, Object> opponentAttackPayload(AttackEffectContext context) {
        Card defenderCard = context.resolutionContext().defenderCard();
        if (defenderCard == null || defenderCard.getAttacks() == null || defenderCard.getAttacks().isEmpty()) {
            return Map.of();
        }
        List<Attack> attacks = new ArrayList<>(defenderCard.getAttacks());
        attacks.sort(Comparator.comparing(Attack::getAttackOrder));
        List<Map<String, Object>> options = new ArrayList<>();
        for (Attack attack : attacks) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("attackOrder", attack.getAttackOrder());
            option.put("attackName", attack.getName());
            options.add(Map.copyOf(option));
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("attacks", List.copyOf(options));
        return Map.copyOf(payload);
    }

    private Map<String, Object> yesNoPayload(AttackEffectContext context) {
        List<GameCardInstance> deck = deckCards(context, context.resolutionContext().attackerUserId());
        if (deck.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put(EFFECT_KEY, MAGMA_MANTLE_EFFECT);
        payload.put(CONTINUATION_KEY, BEFORE_DAMAGE_CONTINUATION);
        payload.put("baseDamage", baseDamage(context.resolutionContext().selectedAttack()));
        payload.put("bonusDamage", context.operation().amount());
        payload.put("requiredEnergyType", context.operation().energyType());
        payload.put("targetPokemonInPlayId", context.targetPokemon().getId().toString());
        return Map.copyOf(payload);
    }

    private Map<String, Object> reorderTopDeckPayload(AttackEffectContext context) {
        List<GameCardInstance> deck = deckCards(context, context.resolutionContext().attackerUserId());
        if (deck.size() <= 1) {
            return Map.of();
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        int limit = Math.min(3, deck.size());
        for (int index = 0; index < limit; index++) {
            cards.add(cardOption(deck.get(index)));
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", List.copyOf(cards));
        return Map.copyOf(payload);
    }

    private Map<String, Object> discardPayload(AttackEffectContext context) {
        List<GameCardInstance> discard = zoneCards(context, context.resolutionContext().attackerUserId(), CardZone.DISCARD);
        if (discard.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> cards = new ArrayList<>();
        for (GameCardInstance card : discard) {
            cards.add(cardOption(card));
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", List.copyOf(cards));
        return Map.copyOf(payload);
    }

    private Map<String, Object> discardItemsPayload(AttackEffectContext context) {
        List<GameCardInstance> discard = zoneCards(context, context.resolutionContext().attackerUserId(), CardZone.DISCARD);
        List<Map<String, Object>> cards = new ArrayList<>();
        for (GameCardInstance card : discard) {
            Card cardEntity = cardService.getCardEntityById(card.getCardId());
            if (CardCategory.ITEM_TRAINER.equals(cardEntity.getCategory())) {
                cards.add(cardOption(card));
            }
        }
        if (cards.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", List.copyOf(cards));
        payload.put("pickupCount", Math.min(context.operation().amount(), cards.size()));
        return Map.copyOf(payload);
    }

    private Map<String, Object> moveOpponentEnergyPayload(AttackEffectContext context) {
        List<Map<String, Object>> energies = attachedEnergyOptions(context.resolutionContext().defenderPokemon());
        List<Map<String, Object>> targets = benchTargets(context, context.resolutionContext().defenderUserId());
        if (energies.isEmpty() || targets.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("energies", energies);
        payload.put("targets", targets);
        return Map.copyOf(payload);
    }

    private Map<String, Object> deckCardAttachToSelfPayload(AttackEffectContext context) {
        List<GameCardInstance> deck = deckCards(context, context.resolutionContext().attackerUserId());
        List<Map<String, Object>> cards = new ArrayList<>();
        for (GameCardInstance card : deck) {
            Card cardEntity = cardService.getCardEntityById(card.getCardId());
            if (isEnergyCard(cardEntity) && matchesEnergyType(cardEntity, context.operation().energyType())) {
                cards.add(cardOption(card));
            }
        }
        if (cards.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", List.copyOf(cards));
        payload.put("targetPokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString());
        payload.put("requiredEnergyType", context.operation().energyType());
        return Map.copyOf(payload);
    }

    private Map<String, Object> deckEnergyAttachToOwnPokemonPayload(AttackEffectContext context) {
        List<Map<String, Object>> cards = basicEnergyDeckOptions(context);
        List<Map<String, Object>> targets = ownPokemonTargets(context);
        if (cards.isEmpty() || targets.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", cards);
        payload.put("targets", targets);
        payload.put("basicOnly", true);
        return Map.copyOf(payload);
    }

    private Map<String, Object> distinctBasicEnergiesPayload(AttackEffectContext context) {
        List<Map<String, Object>> cards = basicEnergyDeckOptions(context);
        if (cards.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> payload = basePayload(context);
        payload.put("cards", cards);
        payload.put("maxCards", 3);
        payload.put("distinctEnergyTypes", true);
        return Map.copyOf(payload);
    }

    private Map<String, Object> basePayload(AttackEffectContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attackerPokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString());
        payload.put("defenderPokemonInPlayId", context.resolutionContext().defenderPokemon().getId().toString());
        payload.put("attackerCardId", context.resolutionContext().attackerCard().getId().toString());
        payload.put("defenderCardId", context.resolutionContext().defenderCard().getId().toString());
        payload.put("attackOrder", context.resolutionContext().selectedAttack().getAttackOrder());
        payload.put(OPTIONAL_KEY, isOptionalChoice(context.operation().type()));
        payload.put(TIMEOUT_POLICY_KEY, timeoutPolicy(context.operation().type()).name());
        return payload;
    }

    private boolean isOptionalChoice(String choiceType) {
        return YES_NO.equals(choiceType)
                || SELECT_CARD_FROM_DISCARD.equals(choiceType)
                || MOVE_OPPONENT_ACTIVE_ENERGY_TO_BENCH.equals(choiceType)
                || SELECT_DECK_CARD_AND_ATTACH_TO_SELF.equals(choiceType)
                || SELECT_DECK_ENERGY_AND_ATTACH_TO_OWN_POKEMON.equals(choiceType)
                || SELECT_DISTINCT_BASIC_ENERGIES_TO_HAND.equals(choiceType)
                || SELECT_DISCARD_ITEMS_TO_HAND.equals(choiceType);
    }

    private PendingChoiceTimeoutPolicy timeoutPolicy(String choiceType) {
        if (isOptionalChoice(choiceType)) {
            return PendingChoiceTimeoutPolicy.AUTO_SKIP;
        }
        if (REORDER_TOP_DECK.equals(choiceType)) {
            return PendingChoiceTimeoutPolicy.AUTO_RESOLVE_DEFAULT;
        }
        return PendingChoiceTimeoutPolicy.AUTO_RANDOM_LEGAL;
    }

    private List<Map<String, Object>> benchTargets(AttackEffectContext context, UUID ownerUserId) {
        List<Map<String, Object>> targets = new ArrayList<>();
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                ownerUserId)) {
            if (pokemon.getSlotPosition() == null || pokemon.getSlotPosition() <= 0) {
                continue;
            }
            targets.add(targetOption(pokemon));
        }
        return List.copyOf(targets);
    }

    private List<Map<String, Object>> ownPokemonTargets(AttackEffectContext context) {
        List<Map<String, Object>> targets = new ArrayList<>();
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId())) {
            targets.add(targetOption(pokemon));
        }
        return List.copyOf(targets);
    }

    private Map<String, Object> targetOption(PokemonInPlay pokemon) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("pokemonInPlayId", pokemon.getId().toString());
        target.put("slotPosition", pokemon.getSlotPosition());

        GameCardInstance activeCardInstance = pokemon.getActiveCardInstance();
        if (activeCardInstance != null) {
            Card activeCard = cardService.getCardEntityById(activeCardInstance.getCardId());
            target.put("name", activeCard.getName());
            target.put("imageSmallUrl", activeCard.getImageSmallUrl());
            target.put("imageLargeUrl", activeCard.getImageLargeUrl());
        }

        return Map.copyOf(target);
    }

    private List<Map<String, Object>> attachedEnergyOptions(PokemonInPlay pokemon) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (PokemonAttachedCard attachedCard : pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())) {
            if (!isEnergy(attachedCard)) {
                continue;
            }
            GameCardInstance card = attachedCard.getGameCardInstance();
            Map<String, Object> option = cardOption(card);
            Map<String, Object> mutableOption = new LinkedHashMap<>(option);
            mutableOption.put("attachedCardId", card.getId().toString());
            options.add(Collections.unmodifiableMap(mutableOption));
        }
        return List.copyOf(options);
    }

    private List<Map<String, Object>> basicEnergyDeckOptions(AttackEffectContext context) {
        List<Map<String, Object>> cards = new ArrayList<>();
        for (GameCardInstance card : deckCards(context, context.resolutionContext().attackerUserId())) {
            Card cardEntity = cardService.getCardEntityById(card.getCardId());
            if (CardCategory.BASIC_ENERGY.equals(cardEntity.getCategory())) {
                cards.add(cardOption(card));
            }
        }
        return List.copyOf(cards);
    }

    private Map<String, Object> cardOption(GameCardInstance card) {
        Card cardEntity = cardService.getCardEntityById(card.getCardId());
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("cardInstanceId", card.getId().toString());
        option.put("cardId", card.getCardId().toString());
        option.put("externalId", cardEntity.getExternalId());
        option.put("name", cardEntity.getName());
        option.put("imageSmallUrl", cardEntity.getImageSmallUrl());
        option.put("imageLargeUrl", cardEntity.getImageLargeUrl());
        option.put("category", cardEntity.getCategory() == null ? null : cardEntity.getCategory().name());
        option.put("energyType", energyType(cardEntity));
        return Collections.unmodifiableMap(option);
    }

    private Map<String, Object> pickupChoicePayload(AttackEffectContext context) {
        return discardItemsPayload(context);
    }

    private List<GameCardInstance> deckCards(AttackEffectContext context, UUID ownerUserId) {
        return zoneCards(context, ownerUserId, CardZone.DECK);
    }

    private List<GameCardInstance> zoneCards(AttackEffectContext context, UUID ownerUserId, CardZone zone) {
        List<GameCardInstance> cards = new ArrayList<>(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                context.resolutionContext().gameId(),
                ownerUserId,
                zone));
        cards.sort(Comparator.comparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)));
        return List.copyOf(cards);
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
        if (card == null || energyType == null || energyType.isBlank()) {
            return false;
        }
        String resolvedEnergyType = energyType(card);
        return resolvedEnergyType != null && energyType.equalsIgnoreCase(resolvedEnergyType);
    }

    private String energyType(Card card) {
        if (card == null) {
            return null;
        }
        if (card.getPokemonType() != null) {
            return card.getPokemonType();
        }
        if (card.getName() != null && card.getName().endsWith(" Energy")) {
            return card.getName().replace(" Energy", "");
        }
        return null;
    }

    private int baseDamage(Attack attack) {
        if (attack == null || attack.getBaseDamage() == null) {
            return 0;
        }
        return attack.getBaseDamage();
    }
}
