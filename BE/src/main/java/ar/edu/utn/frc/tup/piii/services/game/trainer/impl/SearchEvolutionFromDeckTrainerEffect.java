package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffect;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchEvolutionFromDeckTrainerEffect implements TrainerEffect {

    private static final String EFFECT_TYPE = "SEARCH_EVOLUTION_FROM_DECK";
    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";
    private static final String SELECTED_CARD_INSTANCE_ID_KEY = "selectedCardInstanceId";
    private static final String SELECTED_EVOLUTION_EXTERNAL_ID_KEY = "selectedEvolutionExternalId";
    private static final int FIRST_TURN_NUMBER = 1;

    private final TrainerEffectDefinitionReader trainerEffectDefinitionReader;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final SpecialConditionStateService specialConditionStateService;
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
        int currentTurn = context.currentState().turn().turnNumber();

        if (currentTurn <= FIRST_TURN_NUMBER) {
            throw new InvalidGameActionException("Evosoda cannot be played on the first turn");
        }

        UUID targetPokemonInPlayId = requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);

        PokemonInPlay targetPokemon = pokemonInPlayStateService
                .findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId)
                .orElseThrow(() -> new InvalidGameActionException("Target Pokemon was not found"));

        if (targetPokemon.getEnteredPlayTurn() != null && targetPokemon.getEnteredPlayTurn() == currentTurn) {
            throw new InvalidGameActionException(
                    "Cannot evolve a Pokemon that was put into play this same turn");
        }

        PokemonEvolutionStack topStack = pokemonEvolutionStackStateService
                .findTopByPokemonInPlayId(targetPokemon.getId())
                .orElseThrow(() -> new InvalidGameActionException("Evolution stack is missing for the target Pokemon"));

        Card topCard = cardService.getCardEntityById(topStack.getGameCardInstance().getCardId());
        String topName = topCard.getName();
        if (topName == null || topName.isBlank()) {
            throw new InvalidGameActionException("Target Pokemon has no name on its card");
        }

        // Search deck for any card that evolves from the current top card
        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);

        GameCardInstance evolutionInstance = resolveEvolutionInstance(context, deckCards, topCard);
        if (evolutionInstance == null) {
            throw new InvalidGameActionException(
                    "No valid evolution for " + topName + " found in your deck");
        }
        Card evolutionCard = cardService.getCardEntityById(evolutionInstance.getCardId());

        // Move old top card to EVOLUTION_STACK
        GameCardInstance previousTopInstance = topStack.getGameCardInstance();
        int nextStackPosition = gameCardInstanceStateService.nextZonePosition(
                gameId, actorUserId, CardZone.EVOLUTION_STACK);
        previousTopInstance.setZone(CardZone.EVOLUTION_STACK);
        previousTopInstance.setZonePosition(nextStackPosition);
        gameCardInstanceStateService.save(previousTopInstance);
        gameCardInstanceStateService.flush();

        // Move evolution card from DECK to ACTIVE or BENCH
        CardZone targetZone = targetPokemon.getSlotPosition() != null && targetPokemon.getSlotPosition() == 0
                ? CardZone.ACTIVE : CardZone.BENCH;
        evolutionInstance.setZone(targetZone);
        evolutionInstance.setZonePosition(targetPokemon.getSlotPosition());
        evolutionInstance.setFaceDown(false);
        gameCardInstanceStateService.save(evolutionInstance);

        // Update PokemonInPlay
        targetPokemon.setActiveCardInstance(evolutionInstance);
        targetPokemon.setEnteredPlayTurn(currentTurn);
        pokemonInPlayStateService.save(targetPokemon);

        // Add new evolution stack entry
        int nextStackOrder = pokemonEvolutionStackStateService.nextStackOrder(targetPokemon.getId());
        PokemonEvolutionStack newStack = new PokemonEvolutionStack();
        newStack.setPokemonInPlay(targetPokemon);
        newStack.setGameCardInstance(evolutionInstance);
        newStack.setStackOrder(nextStackOrder);
        newStack.setCreatedAtTurn(currentTurn);
        pokemonEvolutionStackStateService.save(newStack);

        // Clear special conditions (evolution cures them)
        specialConditionStateService.deleteByPokemonInPlayId(targetPokemon.getId());

        // Shuffle remaining deck
        List<GameCardInstance> remainingDeck = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);
        List<GameCardInstance> shuffled = gameRandomService.shuffledCopy(remainingDeck);
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setZonePosition(i + 1);
        }
        gameCardInstanceStateService.saveAll(shuffled);

        List<GameEventDto> events = new ArrayList<>();
        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("playerId", actorUserId.toString());
        publicPayload.put("pokemonInPlayId", targetPokemon.getId().toString());
        publicPayload.put("cardId", evolutionCard.getId().toString());
        publicPayload.put("previousCardId", topCard.getId().toString());
        events.add(gameEventFactory.publicEvent(
                gameId, GameEventType.POKEMON_EVOLVED, context.stateVersion(),
                Map.copyOf(publicPayload)));

        Map<String, Object> effectData = new LinkedHashMap<>();
        effectData.put("effectType", EFFECT_TYPE);
        effectData.put("pokemonInPlayId", targetPokemon.getId().toString());
        effectData.put("evolvedTo", evolutionCard.getName());
        return new TrainerEffectResult(Map.copyOf(effectData), List.copyOf(events));
    }

    @Override
    public Map<String, Object> preview(TrainerEffectContext context) {
        UUID gameId = context.gameId();
        UUID actorUserId = context.actorUserId();
        int currentTurn = context.currentState().turn().turnNumber();

        Map<UUID, Card> eligibleTargetsTopCard = new LinkedHashMap<>();
        if (currentTurn > FIRST_TURN_NUMBER) {
            List<PokemonInPlay> ownPokemon =
                    pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, actorUserId);
            for (PokemonInPlay pokemon : ownPokemon) {
                if (pokemon.getEnteredPlayTurn() != null && pokemon.getEnteredPlayTurn() == currentTurn) {
                    continue;
                }
                Optional<PokemonEvolutionStack> topStackOpt =
                        pokemonEvolutionStackStateService.findTopByPokemonInPlayId(pokemon.getId());
                if (topStackOpt.isEmpty()) {
                    continue;
                }
                Card topCard = cardService.getCardEntityById(
                        topStackOpt.get().getGameCardInstance().getCardId());
                if (topCard != null) {
                    eligibleTargetsTopCard.put(pokemon.getId(), topCard);
                }
            }
        }

        if (eligibleTargetsTopCard.isEmpty()) {
            return Map.of("options", List.of());
        }

        List<GameCardInstance> deckCards = gameCardInstanceStateService
                .findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK);

        Map<UUID, Card> cardById = new LinkedHashMap<>();
        Map<UUID, Integer> countById = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> validTargetsById = new LinkedHashMap<>();

        for (GameCardInstance deckCard : deckCards) {
            Card candidate = cardService.getCardEntityById(deckCard.getCardId());
            if (candidate == null || candidate.getExternalId() == null || candidate.getExternalId().isBlank()) {
                continue;
            }
            Set<UUID> matchingTargets = new LinkedHashSet<>();
            for (Map.Entry<UUID, Card> entry : eligibleTargetsTopCard.entrySet()) {
                if (isValidEvolution(candidate, entry.getValue())) {
                    matchingTargets.add(entry.getKey());
                }
            }
            if (matchingTargets.isEmpty()) {
                continue;
            }
            cardById.put(candidate.getId(), candidate);
            countById.merge(candidate.getId(), 1, Integer::sum);
            validTargetsById.computeIfAbsent(candidate.getId(), ignored -> new LinkedHashSet<>())
                    .addAll(matchingTargets);
        }

        List<Map<String, Object>> options = new ArrayList<>();
        for (UUID candidateCardId : cardById.keySet()) {
            Card candidate = cardById.get(candidateCardId);
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("cardId", candidate.getId().toString());
            option.put("externalId", candidate.getExternalId());
            option.put("name", candidate.getName());
            option.put("evolvesFrom", candidate.getEvolvesFrom());
            option.put("count", countById.get(candidateCardId));
            option.put("validTargetPokemonInPlayIds", validTargetsById.get(candidateCardId).stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));
            options.add(Map.copyOf(option));
        }

        return Map.of("options", List.copyOf(options));
    }

    private GameCardInstance resolveEvolutionInstance(
            TrainerEffectContext context, List<GameCardInstance> deckCards, Card topCard) {
        String selectedExternalId = optionalString(
                context.request().payload(),
                SELECTED_EVOLUTION_EXTERNAL_ID_KEY);
        if (selectedExternalId != null) {
            for (GameCardInstance deckCard : deckCards) {
                Card candidate = cardService.getCardEntityById(deckCard.getCardId());
                if (candidate != null
                        && selectedExternalId.equalsIgnoreCase(candidate.getExternalId())
                        && isValidEvolution(candidate, topCard)) {
                    return deckCard;
                }
            }
            throw new InvalidGameActionException("Selected evolution card was not found in the deck");
        }

        Object selectedRaw = context.request().payload() != null
                ? context.request().payload().get(SELECTED_CARD_INSTANCE_ID_KEY)
                : null;

        if (selectedRaw != null) {
            UUID selectedId = requiredUuid(context.request().payload(), SELECTED_CARD_INSTANCE_ID_KEY);
            for (GameCardInstance deckCard : deckCards) {
                if (deckCard.getId().equals(selectedId)) {
                    Card candidate = cardService.getCardEntityById(deckCard.getCardId());
                    if (!isValidEvolution(candidate, topCard)) {
                        throw new InvalidGameActionException(
                                "Selected card is not a valid evolution for the target Pokemon");
                    }
                    return deckCard;
                }
            }
            throw new InvalidGameActionException("Selected evolution card was not found in the deck");
        }

        for (GameCardInstance deckCard : deckCards) {
            Card candidate = cardService.getCardEntityById(deckCard.getCardId());
            if (isValidEvolution(candidate, topCard)) {
                return deckCard;
            }
        }
        return null;
    }

    private String optionalString(Map<String, Object> payload, String key) {
        Object value = payload != null ? payload.get(key) : null;
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        return null;
    }

    private boolean isValidEvolution(Card candidate, Card currentTop) {
        if (candidate == null || candidate.getEvolvesFrom() == null || candidate.getEvolvesFrom().isBlank()) {
            return false;
        }
        if (!CardCategory.STAGE_1_POKEMON.equals(candidate.getCategory())
                && !CardCategory.STAGE_2_POKEMON.equals(candidate.getCategory())) {
            return false;
        }
        String topName = currentTop != null ? currentTop.getName() : null;
        return topName != null && candidate.getEvolvesFrom().equalsIgnoreCase(topName);
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
