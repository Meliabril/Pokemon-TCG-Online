package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.card.AbilityTranslationDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardAbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardAttackDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardPlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardPokemonDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardZoneSummaryDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleAbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleAttackCostDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleAttackDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardActionHintsDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePlayerBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePokemonDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePokemonEffectDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleStadiumDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleZoneDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameActionLog;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.repositories.GameActionLogReadRepository;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.card.CardTranslationService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectDefinition;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEnergyRequirementService;
import ar.edu.utn.frc.tup.piii.services.game.attack.impl.AttackEffectDefinitionReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateQueryServiceImpl implements GameStateQueryService {

    private static final String MULLIGAN_FLOW_KEY = "mulliganFlow";
    private static final String MULLIGAN_FLOW_ACTIVE_KEY = "active";
    private static final String MULLIGAN_FLOW_ROUND_NUMBER_KEY = "roundNumber";
    private static final String MULLIGAN_FLOW_CURRENT_PLAYERS_KEY = "currentMulliganPlayerIds";
    private static final String MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY = "readyForInitialSelection";
    private static final String SETUP_ACTIVE_OCCUPIED_KEY = "setupActiveOccupiedByPlayer";
    private static final String SETUP_BENCH_OCCUPIED_INDEXES_KEY = "setupBenchOccupiedIndexesByPlayer";
    // Operation types that automatically apply to every Pokemon on a bench (no choice involved)
    // even though their "target" is OWN_BENCH/OPPONENT_BENCH - the same target strings used by
    // single-target-selection effects like HEAL_DAMAGE, MOVE_ENERGY_TO_BENCH, or BENCH_DAMAGE.
    // These must be excluded from attackTargetMode() so the attack-declare UI never asks the
    // player to pick a target for an effect that was never going to read one.
    private static final Set<String> AUTOMATIC_BENCH_SWEEP_OPERATION_TYPES = Set.of("ALL_BENCH_DAMAGE");

    private final GameParticipantStateService gameParticipantStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final SpecialConditionStateService specialConditionStateService;
    private final GameActionLogReadRepository gameActionLogReadRepository;
    private final CardService cardService;
    private final CardTranslationService cardTranslationService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    private final GameStateVisibilitySanitizer gameStateVisibilitySanitizer;
    private final AttackEnergyRequirementService attackEnergyRequirementService;
    private final AttackEffectDefinitionReader attackEffectDefinitionReader;
    private final AbilityCatalogService abilityCatalogService;
    private final AbilityUsageTracker abilityUsageTracker;
    private final PassiveAbilityService passiveAbilityService;

    @Override
    public GameStateDto buildVisibleState(Game game) {
        return buildCanonicalVisibleState(game);
    }

    @Override
    public GameStateDto buildVisibleState(Game game, UUID viewerUserId) {
        return gameStateVisibilitySanitizer.sanitizeForViewer(buildCanonicalVisibleState(game), viewerUserId);
    }

    @Override
    public GameStateDto sanitizeVisibleStateForViewer(GameStateDto state, UUID viewerUserId) {
        return gameStateVisibilitySanitizer.sanitizeForViewer(state, viewerUserId);
    }

    private GameStateDto buildCanonicalVisibleState(Game game) {
        long start = System.currentTimeMillis();
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(game.getId());
        List<UUID> playerIds = new ArrayList<>();
        for (GameParticipant participant : participants) {
            playerIds.add(participant.getUserId());
        }
        UUID playerWhoWentFirstId = game.getPlayerWhoWentFirstId();

        Map<UUID, Integer> benchCountByPlayer = new HashMap<>();
        Map<UUID, Integer> pokemonEnteredPlayTurnMap = new HashMap<>();
        Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer = new HashMap<>();
        Map<UUID, List<UUID>> cardsInHandByPlayer = new HashMap<>();
        Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer = new HashMap<>();
        Map<UUID, Set<UUID>> affordableAttacksByPlayer = new HashMap<>();
        Map<UUID, CardZone> cardZones = new HashMap<>();
        Map<UUID, UUID> cardOwners = new HashMap<>();
        for (UUID playerId : playerIds) {
            benchCountByPlayer.put(playerId, 0);
            activePokemonConditionsByPlayer.put(playerId, List.of());
            cardsInHandByPlayer.put(playerId, new ArrayList<>());
            cardsInHandInstanceIdsByPlayer.put(playerId, new ArrayList<>());
            affordableAttacksByPlayer.put(playerId, new HashSet<>());
        }

        List<GameCardInstance> cardInstances = gameCardInstanceStateService.findByGameId(game.getId());
        for (GameCardInstance cardInstance : cardInstances) {
            putCardReferences(cardZones, cardOwners, cardInstance);
            if (CardZone.HAND.equals(cardInstance.getZone())) {
                List<UUID> handCards = cardsInHandByPlayer.get(cardInstance.getOwnerUserId());
                if (handCards == null) {
                    handCards = new ArrayList<>();
                    cardsInHandByPlayer.put(cardInstance.getOwnerUserId(), handCards);
                }
                handCards.add(cardInstance.getCardId());
                List<UUID> handCardInstances = cardsInHandInstanceIdsByPlayer.get(cardInstance.getOwnerUserId());
                if (handCardInstances == null) {
                    handCardInstances = new ArrayList<>();
                    cardsInHandInstanceIdsByPlayer.put(cardInstance.getOwnerUserId(), handCardInstances);
                }
                handCardInstances.add(cardInstance.getId());
            }
        }

        SetupStateView setupStateView = setupStateView(game, playerIds);

        List<PokemonInPlay> pokemonInPlay = pokemonInPlayStateService.findByGameIdOrdered(game.getId());
        for (PokemonInPlay slot : pokemonInPlay) {
            pokemonEnteredPlayTurnMap.put(slot.getId(), slot.getEnteredPlayTurn());
            Integer slotPosition = slot.getSlotPosition();
            if (slotPosition != null && slotPosition > 0) {
                incrementBenchCount(benchCountByPlayer, slot.getOwnerUserId());
            }
            if (slotPosition != null && slotPosition == 0) {
                activePokemonConditionsByPlayer.put(
                        slot.getOwnerUserId(),
                        specialConditionStateService.activeConditionTypes(slot.getId()));
                affordableAttacksByPlayer.put(slot.getOwnerUserId(), affordableAttacksFor(slot));
            }
            putPokemonInPlayReference(cardZones, cardOwners, slot);
        }

        if (pokemonInPlay.isEmpty()) {
            for (GameCardInstance cardInstance : cardInstances) {
                if (CardZone.BENCH.equals(cardInstance.getZone())) {
                    incrementBenchCount(benchCountByPlayer, cardInstance.getOwnerUserId());
                }
            }
        }

        List<GameActionType> availableActions = availableActionsFor(game);

        GameStateDto resultState = GameStateDto.builder()
                .gameId(game.getId())
                .status(game.getStatus())
                .stateVersion(game.getStateVersion())
                .playerIds(playerIds)
                .players(players(
                        playerIds,
                        benchCountByPlayer,
                        activePokemonConditionsByPlayer,
                        cardsInHandByPlayer,
                        cardsInHandInstanceIdsByPlayer,
                        affordableAttacksByPlayer,
                        setupStateView))
                .boardPlayers(boardPlayers(game, playerIds, cardInstances, pokemonInPlay, affordableAttacksByPlayer, availableActions))
                .turn(TurnContextDto.builder()
                        .currentPhase(game.getCurrentPhase())
                        .turnNumber(game.getTurnNumber())
                        .activePlayerId(game.getActivePlayerId())
                        .playerWhoWentFirstId(playerWhoWentFirstId)
                        .turnStartedAt(game.getTurnStartedAt())
                        .build())
                .board(BoardStateDto.builder()
                        .enteredPlayTurnByPokemonInPlayId(Map.copyOf(pokemonEnteredPlayTurnMap))
                        .zoneByCardReferenceId(Map.copyOf(cardZones))
                        .ownerByCardReferenceId(Map.copyOf(cardOwners))
                        .view(buildVisibleBoard(game, participants, cardInstances, pokemonInPlay, setupStateView, availableActions))
                        .build())
                .actions(ActionStateDto.builder()
                        .availableActions(availableActions)
                        .processedClientActionIds(processedClientActionIds(game.getId()))
                        .build())
                .resolution(resolutionStateView(game.getResolutionState()))
                .updatedAt(game.getUpdatedAt())
                .build();

        log.info("[MAPPER_STATE] buildCanonicalVisibleState para gameId={}: {} ms", game.getId(), System.currentTimeMillis() - start);
        return resultState;
    }

    private Map<UUID, BoardPlayerStateDto> boardPlayers(
            Game game,
            List<UUID> playerIds,
            List<GameCardInstance> cardInstances,
            List<PokemonInPlay> pokemonInPlay,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            List<GameActionType> availableActions) {
        Map<UUID, List<GameCardInstance>> cardsByOwner = cardsByOwner(cardInstances);
        Map<UUID, List<PokemonInPlay>> pokemonByOwner = pokemonByOwner(pokemonInPlay);
        Map<UUID, BoardPlayerStateDto> boardPlayers = new LinkedHashMap<>();

        for (UUID playerId : playerIds) {
            List<GameCardInstance> ownerCards = cardsByOwner.getOrDefault(playerId, List.of());
            List<PokemonInPlay> ownerPokemon = pokemonByOwner.getOrDefault(playerId, List.of());
            Map<UUID, Card> cardsById = new HashMap<>();
            Map<UUID, HandCardPlayability> handPlayability = handPlayabilityFor(
                    game, playerId, ownerCards, ownerPokemon, availableActions, cardsById);
            boardPlayers.put(playerId, new BoardPlayerStateDto(
                    playerId,
                    zoneSummary(ownerCards, CardZone.HAND, handPlayability),
                    zoneSummary(ownerCards, CardZone.DECK, Map.of()),
                    zoneSummary(ownerCards, CardZone.PRIZE, Map.of()),
                    zoneSummary(ownerCards, CardZone.DISCARD, Map.of()),
                    zoneSummary(ownerCards, CardZone.STADIUM, Map.of()),
                    activePokemon(game, ownerPokemon, affordableAttacksByPlayer.getOrDefault(playerId, Set.of())),
                    benchPokemon(game, ownerPokemon, affordableAttacksByPlayer.getOrDefault(playerId, Set.of()))));
        }

        return Map.copyOf(boardPlayers);
    }

    private Map<UUID, List<GameCardInstance>> cardsByOwner(List<GameCardInstance> cardInstances) {
        Map<UUID, List<GameCardInstance>> cardsByOwner = new LinkedHashMap<>();
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            UUID ownerUserId = cardInstance.getOwnerUserId();
            if (ownerUserId == null) {
                continue;
            }
            cardsByOwner.computeIfAbsent(ownerUserId, ignored -> new ArrayList<>()).add(cardInstance);
        }

        return cardsByOwner;
    }

    private Map<UUID, List<PokemonInPlay>> pokemonByOwner(List<PokemonInPlay> pokemonInPlay) {
        Map<UUID, List<PokemonInPlay>> pokemonByOwner = new LinkedHashMap<>();
        for (PokemonInPlay pokemon : safeList(pokemonInPlay)) {
            UUID ownerUserId = pokemon.getOwnerUserId();
            if (ownerUserId == null) {
                continue;
            }
            pokemonByOwner.computeIfAbsent(ownerUserId, ignored -> new ArrayList<>()).add(pokemon);
        }

        return pokemonByOwner;
    }

    private BoardZoneSummaryDto zoneSummary(
            List<GameCardInstance> cardInstances,
            CardZone zone,
            Map<UUID, HandCardPlayability> handPlayability) {
        List<BoardCardDto> cards = new ArrayList<>();
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            if (zone.equals(cardInstance.getZone())) {
                HandCardPlayability playability = zone == CardZone.HAND
                        ? handPlayability.getOrDefault(cardInstance.getId(), HandCardPlayability.notPlayable(null))
                        : HandCardPlayability.notPlayable(null);
                cards.add(boardCard(cardInstance, playability));
            }
        }
        cards.sort((left, right) -> compareNullableInteger(left.zonePosition(), right.zonePosition()));

        return new BoardZoneSummaryDto(cards.size(), cards);
    }

    private BoardPokemonDto activePokemon(Game game, List<PokemonInPlay> pokemonInPlay, Set<UUID> affordableAttackIds) {
        for (PokemonInPlay pokemon : safeList(pokemonInPlay)) {
            Integer slotPosition = pokemon.getSlotPosition();
            if (slotPosition != null && slotPosition == 0) {
                return boardPokemon(game, pokemon, affordableAttackIds);
            }
        }

        return null;
    }

    private List<BoardPokemonDto> benchPokemon(Game game, List<PokemonInPlay> pokemonInPlay, Set<UUID> affordableAttackIds) {
        List<BoardPokemonDto> benchPokemon = new ArrayList<>();
        for (PokemonInPlay pokemon : safeList(pokemonInPlay)) {
            Integer slotPosition = pokemon.getSlotPosition();
            if (slotPosition != null && slotPosition > 0) {
                benchPokemon.add(boardPokemon(game, pokemon, affordableAttackIds));
            }
        }
        benchPokemon.sort((left, right) -> compareNullableInteger(left.slotPosition(), right.slotPosition()));

        return List.copyOf(benchPokemon);
    }

    private BoardPokemonDto boardPokemon(Game game, PokemonInPlay pokemon, Set<UUID> affordableAttackIds) {
        List<SpecialConditionType> specialConditions = safeList(specialConditionStateService.activeConditionTypes(pokemon.getId()));
        Card activeCard = findCardOrNull(pokemon.getActiveCardInstance().getCardId());
        return new BoardPokemonDto(
                pokemon.getId(),
                pokemon.getOwnerUserId(),
                pokemon.getSlotPosition(),
                pokemon.getDamageCounters(),
                pokemon.getEnteredPlayTurn(),
                boardCard(pokemon.getActiveCardInstance(), HandCardPlayability.notPlayable(null)),
                evolutionStackCards(pokemon.getId()),
                attachedCards(pokemon.getId()),
                specialConditions,
                attacks(game, pokemon, affordableAttackIds),
                boardAbilities(game, activeCard, pokemon));
    }

    private List<BoardCardDto> evolutionStackCards(UUID pokemonInPlayId) {
        List<PokemonEvolutionStack> evolutionStack = pokemonEvolutionStackStateService.findByPokemonInPlayId(pokemonInPlayId);
        List<PokemonEvolutionStack> safeEvolutionStack = new ArrayList<>(safeList(evolutionStack));
        safeEvolutionStack.sort((left, right) -> compareNullableInteger(left.getStackOrder(), right.getStackOrder()));
        List<BoardCardDto> cards = new ArrayList<>();
        for (PokemonEvolutionStack stackItem : safeEvolutionStack) {
            cards.add(boardCard(stackItem.getGameCardInstance(), HandCardPlayability.notPlayable(null)));
        }

        return List.copyOf(cards);
    }

    private List<BoardCardDto> attachedCards(UUID pokemonInPlayId) {
        List<BoardCardDto> cards = new ArrayList<>();
        for (PokemonAttachedCard attachedCard : safeList(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemonInPlayId))) {
            cards.add(boardCard(attachedCard.getGameCardInstance(), HandCardPlayability.notPlayable(null)));
        }

        return List.copyOf(cards);
    }

    private List<BoardAttackDto> attacks(Game game, PokemonInPlay pokemon, Set<UUID> affordableAttackIds) {
        if (pokemon == null || pokemon.getActiveCardInstance() == null) {
            return List.of();
        }

        Card activeCard = findCardOrNull(pokemon.getActiveCardInstance().getCardId());
        if (activeCard == null || activeCard.getAttacks() == null) {
            return List.of();
        }

        List<Attack> attacks = new ArrayList<>(activeCard.getAttacks());
        attacks.sort((left, right) -> Integer.compare(left.getAttackOrder(), right.getAttackOrder()));
        List<BoardAttackDto> boardAttacks = new ArrayList<>();
        for (Attack attack : attacks) {
            boolean blockedByTorment = isBlockedAttackForCurrentTurn(game, pokemon, attack);
            boolean available = affordableAttackIds.contains(attack.getId()) && !blockedByTorment;
            String disabledReason = null;
            if (blockedByTorment) {
                disabledReason = "Este ataque esta bloqueado este turno.";
            } else if (!available) {
                disabledReason = "Energia insuficiente o accion no disponible";
            }

            AttackTargetMode attackTargetMode = attackTargetMode(safeAttackEffectDefinition(activeCard, attack));
            List<UUID> targetIds = attackTargetMode != null
                    ? attackTargetPokemonIds(pokemon.getGame(), pokemon.getOwnerUserId(), attackTargetMode)
                    : List.of();
            boolean requiresTarget = attackTargetMode != null && !targetIds.isEmpty();

            boardAttacks.add(new BoardAttackDto(
                    attack.getId(),
                    attack.getName(),
                    attack.getDamageText(),
                    attack.getBaseDamage(),
                    attack.getEffectText(),
                    attack.getAttackOrder(),
                    available,
                    disabledReason,
                    requiresTarget,
                    targetIds,
                    requiresTarget && attackTargetMode.targetsOwnPokemon()));
        }

        return List.copyOf(boardAttacks);
    }

    private BoardCardDto boardCard(GameCardInstance cardInstance, HandCardPlayability playability) {
        if (cardInstance == null) {
            return null;
        }

        Card card = findCardOrNull(cardInstance.getCardId());
        if (card == null) {
            return new BoardCardDto(
                    cardInstance.getId(),
                    cardInstance.getCardId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    cardInstance.getZone(),
                    cardInstance.getZonePosition(),
                    Boolean.TRUE.equals(cardInstance.getFaceDown()),
                    playability.playable(),
                    playability.suggestedAction(),
                    playability.disabledReason(),
                    playability.validTargetPokemonInPlayIds());
        }

        return new BoardCardDto(
                cardInstance.getId(),
                card.getId(),
                card.getExternalId(),
                card.getName(),
                card.getSetCode(),
                card.getNumber(),
                card.getSupertype(),
                card.getCategory(),
                card.getSubtype(),
                card.getImageSmallUrl(),
                card.getImageLargeUrl(),
                card.getHp(),
                cardInstance.getZone(),
                cardInstance.getZonePosition(),
                Boolean.TRUE.equals(cardInstance.getFaceDown()),
                playability.playable(),
                playability.suggestedAction(),
                playability.disabledReason(),
                playability.validTargetPokemonInPlayIds());
    }

    private Map<UUID, PlayerStateDto> players(
            List<UUID> playerIds,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            SetupStateView setupStateView) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>();
        Map<UUID, List<UUID>> copiedHandCardIds = copyUuidListMap(cardsInHandByPlayer);
        Map<UUID, List<UUID>> copiedHandInstanceIds = copyUuidListMap(cardsInHandInstanceIdsByPlayer);
        Map<UUID, Set<UUID>> copiedAffordableAttacks = copyAffordableAttacks(affordableAttacksByPlayer);

        for (UUID playerId : playerIds) {
            Integer benchCount = benchCountByPlayer.get(playerId);
            int safeBenchCount = 0;
            if (benchCount != null) {
                safeBenchCount = benchCount;
            }

            Integer mulliganCount = setupStateView.mulliganCountByPlayer().get(playerId);
            int safeMulliganCount = 0;
            if (mulliganCount != null) {
                safeMulliganCount = mulliganCount;
            }

            Boolean selectionSubmitted = setupStateView.setupSelectionSubmittedByPlayer().get(playerId);
            boolean safeSelectionSubmitted = false;
            if (selectionSubmitted != null) {
                safeSelectionSubmitted = selectionSubmitted;
            }

            players.put(playerId, PlayerStateDto.builder()
                    .benchPokemonCount(safeBenchCount)
                    .activePokemonConditions(activePokemonConditionsByPlayer.get(playerId))
                    .cardIdsInHand(copiedHandCardIds.get(playerId))
                    .cardInstanceIdsInHand(copiedHandInstanceIds.get(playerId))
                    .affordableAttackIds(copiedAffordableAttacks.get(playerId))
                    .mulliganCount(safeMulliganCount)
                    .mulliganNoticePending(setupStateView.pendingMulliganAcknowledgementsByPlayer().getOrDefault(playerId, Boolean.FALSE))
                    .mulliganFlowActive(setupStateView.mulliganFlowActive())
                    .mulliganReadyForInitialSelection(setupStateView.mulliganReadyForInitialSelection())
                    .mulliganRoundNumber(setupStateView.mulliganRoundNumber())
                    .mulliganCurrentPlayer(setupStateView.mulliganCurrentPlayerByPlayer().getOrDefault(playerId, Boolean.FALSE))
                    .initialPokemonSelectionSubmitted(safeSelectionSubmitted)
                    .initialActiveCardInstanceId(setupStateView.setupActiveCardInstanceIdByPlayer().get(playerId))
                    .initialBenchCardInstanceIds(setupStateView.setupBenchCardInstanceIdsByPlayer().get(playerId))
                    .build());
        }

        return Map.copyOf(players);
    }

    private List<GameActionType> availableActionsFor(Game game) {
        ResolutionStateDto resolutionState = resolutionStateView(game.getResolutionState());
        if (resolutionState.hasPendingPromotion()) {
            return List.of(GameActionType.PROMOTE_BENCH_POKEMON);
        }
        if (resolutionState.hasPendingAttackChoice()) {
            return List.of(GameActionType.RESOLVE_ATTACK_CHOICE);
        }
        if (resolutionState.hasPendingSuddenDeath()) {
            return List.of();
        }

        if (game.getStatus() == null) {
            return List.of();
        }

        switch (game.getStatus()) {
            case WAITING:
                return List.of(GameActionType.START_GAME);
            case SETUP:
                if (isMulliganFlowActive(game.getSetupState()) || hasAnyPendingMulliganAcknowledgement(game.getSetupState())) {
                    return List.of(GameActionType.ACK_MULLIGAN_NOTICE, GameActionType.CHOOSE_INITIAL_POKEMON);
                }
                return List.of(GameActionType.CHOOSE_INITIAL_POKEMON);
            case ACTIVE:
                return activePhaseActions(game.getCurrentPhase());
            default:
                return List.of();
        }
    }

    private List<GameActionType> activePhaseActions(TurnPhase currentPhase) {
        if (currentPhase == null) {
            return List.of();
        }

        switch (currentPhase) {
            case DRAW:
                return List.of(GameActionType.DRAW_CARD);
            case MAIN:
                return List.of(
                        GameActionType.PLAY_BASIC_POKEMON,
                        GameActionType.ATTACH_ENERGY,
                        GameActionType.PLAY_TRAINER,
                        GameActionType.EVOLVE_POKEMON,
                        GameActionType.USE_ABILITY,
                        GameActionType.RETREAT,
                        GameActionType.DECLARE_ATTACK,
                        GameActionType.END_TURN);
            case ATTACK:
                return List.of(GameActionType.USE_ABILITY, GameActionType.DECLARE_ATTACK, GameActionType.SELECT_TARGET, GameActionType.END_TURN);
            case BETWEEN_TURNS:
                return List.of();
            default:
                return List.of();
        }
    }

    private boolean hasAnyPendingMulliganAcknowledgement(Map<String, Object> setupState) {
        Map<String, Object> pendingValues = nestedMap(setupState, "pendingMulliganAcknowledgementsByPlayer");
        for (Object pendingValue : pendingValues.values()) {
            if (Boolean.TRUE.equals(booleanValue(pendingValue))) {
                return true;
            }
        }

        return false;
    }

    private boolean isMulliganFlowActive(Map<String, Object> setupState) {
        Map<String, Object> flowState = nestedMap(setupState, MULLIGAN_FLOW_KEY);
        return booleanValue(flowState.get(MULLIGAN_FLOW_ACTIVE_KEY));
    }

    private void putCardReferences(
            Map<UUID, CardZone> cardZones,
            Map<UUID, UUID> cardOwners,
            GameCardInstance cardInstance) {
        if (cardInstance == null || cardInstance.getZone() == null) {
            return;
        }

        putReference(cardZones, cardOwners, cardInstance.getId(), cardInstance.getZone(), cardInstance.getOwnerUserId());
        putReference(cardZones, cardOwners, cardInstance.getCardId(), cardInstance.getZone(), cardInstance.getOwnerUserId());
    }

    private void putPokemonInPlayReference(
            Map<UUID, CardZone> cardZones,
            Map<UUID, UUID> cardOwners,
            PokemonInPlay pokemonInPlay) {
        if (pokemonInPlay == null) {
            return;
        }

        CardZone zone = CardZone.BENCH;
        Integer slotPosition = pokemonInPlay.getSlotPosition();
        if (slotPosition != null && slotPosition == 0) {
            zone = CardZone.ACTIVE;
        }

        putReference(cardZones, cardOwners, pokemonInPlay.getId(), zone, pokemonInPlay.getOwnerUserId());
    }

    private void putReference(
            Map<UUID, CardZone> cardZones,
            Map<UUID, UUID> cardOwners,
            UUID referenceId,
            CardZone zone,
            UUID ownerUserId) {
        if (referenceId == null) {
            return;
        }

        cardZones.put(referenceId, zone);
        cardOwners.put(referenceId, ownerUserId);
    }

    private void incrementBenchCount(Map<UUID, Integer> benchCountByPlayer, UUID ownerUserId) {
        if (ownerUserId == null) {
            return;
        }

        Integer currentCount = benchCountByPlayer.get(ownerUserId);
        if (currentCount == null) {
            benchCountByPlayer.put(ownerUserId, 1);
            return;
        }

        benchCountByPlayer.put(ownerUserId, currentCount + 1);
    }

    private Map<UUID, Set<UUID>> copyAffordableAttacks(Map<UUID, Set<UUID>> source) {
        Map<UUID, Set<UUID>> copiedValues = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : source.entrySet()) {
            Set<UUID> attacks = entry.getValue();
            if (attacks == null) {
                copiedValues.put(entry.getKey(), Set.of());
            } else {
                copiedValues.put(entry.getKey(), Set.copyOf(attacks));
            }
        }

        return Map.copyOf(copiedValues);
    }

    private Set<UUID> affordableAttacksFor(PokemonInPlay activePokemon) {
        Set<UUID> affordableAttacks = new HashSet<>();
        if (activePokemon == null || activePokemon.getActiveCardInstance() == null) {
            return affordableAttacks;
        }

        Card activeCard = findCardOrNull(activePokemon.getActiveCardInstance().getCardId());
        if (activeCard == null || activeCard.getAttacks() == null) {
            return affordableAttacks;
        }

        for (Attack attack : activeCard.getAttacks()) {
            if (attack != null && attackEnergyRequirementService.hasRequiredEnergy(activePokemon, attack)) {
                affordableAttacks.add(attack.getId());
            }
        }

        return affordableAttacks;
    }

    private Card findCardOrNull(UUID cardId) {
        try {
            return cardService.getCardEntityById(cardId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Set<UUID> processedClientActionIds(UUID gameId) {
        Set<UUID> processedIds = new HashSet<>();
        List<GameActionLog> history = gameActionLogReadRepository.findHistory(gameId);
        for (GameActionLog log : history) {
            if (log.getClientActionId() != null) {
                processedIds.add(log.getClientActionId());
            }
        }

        return Set.copyOf(processedIds);
    }

    private SetupStateView setupStateView(Game game, List<UUID> playerIds) {
        Map<UUID, Integer> mulliganCountByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> pendingMulliganAcknowledgementsByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> mulliganCurrentPlayerByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> setupSelectionSubmittedByPlayer = new LinkedHashMap<>();
        Map<UUID, UUID> setupActiveCardInstanceIdByPlayer = new LinkedHashMap<>();
        Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer = new LinkedHashMap<>();
        Map<UUID, Boolean> setupActiveOccupiedByPlayer = new LinkedHashMap<>();
        Map<UUID, Integer> setupBenchOccupiedCountByPlayer = new LinkedHashMap<>();

        Map<String, Object> setupState = game.getSetupState();
        Map<String, Object> mulliganValues = nestedMap(setupState, "mulliganCountByPlayer");
        Map<String, Object> pendingMulliganAcknowledgementValues = nestedMap(setupState, "pendingMulliganAcknowledgementsByPlayer");
        Map<String, Object> submittedValues = nestedMap(setupState, "setupSelectionSubmittedByPlayer");
        Map<String, Object> activeValues = nestedMap(setupState, "setupActiveCardInstanceIdByPlayer");
        Map<String, Object> benchValues = nestedMap(setupState, "setupBenchCardInstanceIdsByPlayer");
        Map<String, Object> mulliganFlow = nestedMap(setupState, MULLIGAN_FLOW_KEY);
        boolean mulliganFlowActive = booleanValue(mulliganFlow.get(MULLIGAN_FLOW_ACTIVE_KEY));
        boolean mulliganReadyForInitialSelection = booleanValue(mulliganFlow.get(MULLIGAN_READY_FOR_INITIAL_SELECTION_KEY));
        if (!mulliganFlowActive && mulliganFlow.isEmpty()) {
            mulliganReadyForInitialSelection = true;
        }
        int mulliganRoundNumber = integerValue(mulliganFlow.get(MULLIGAN_FLOW_ROUND_NUMBER_KEY), 0);
        Set<String> currentMulliganPlayerKeys = stringSet(mulliganFlow.get(MULLIGAN_FLOW_CURRENT_PLAYERS_KEY));
        Map<String, Object> activeOccupiedValues = nestedMap(setupState, SETUP_ACTIVE_OCCUPIED_KEY);
        Map<String, Object> benchOccupiedValues = nestedMap(setupState, SETUP_BENCH_OCCUPIED_INDEXES_KEY);

        for (UUID playerId : playerIds) {
            String playerKey = playerId.toString();
            mulliganCountByPlayer.put(playerId, integerValue(mulliganValues.get(playerKey), 0));
            pendingMulliganAcknowledgementsByPlayer.put(playerId, booleanValue(pendingMulliganAcknowledgementValues.get(playerKey)));
            mulliganCurrentPlayerByPlayer.put(playerId, currentMulliganPlayerKeys.contains(playerKey));
            setupSelectionSubmittedByPlayer.put(playerId, booleanValue(submittedValues.get(playerKey)));
            UUID activeCardInstanceId = uuidValue(activeValues.get(playerKey));
            if (activeCardInstanceId != null) {
                setupActiveCardInstanceIdByPlayer.put(playerId, activeCardInstanceId);
            }
            List<UUID> selectedBenchCards = uuidListValue(benchValues.get(playerKey));
            setupBenchCardInstanceIdsByPlayer.put(playerId, selectedBenchCards);
            setupActiveOccupiedByPlayer.put(
                    playerId,
                    activeCardInstanceId != null || booleanValue(activeOccupiedValues.get(playerKey)));
            setupBenchOccupiedCountByPlayer.put(
                    playerId,
                    Math.max(selectedBenchCards.size(), listSize(benchOccupiedValues.get(playerKey))));
        }

        return new SetupStateView(
                Map.copyOf(mulliganCountByPlayer),
                Map.copyOf(pendingMulliganAcknowledgementsByPlayer),
                Map.copyOf(mulliganCurrentPlayerByPlayer),
                mulliganFlowActive,
                mulliganReadyForInitialSelection,
                mulliganRoundNumber,
                Map.copyOf(setupSelectionSubmittedByPlayer),
                Map.copyOf(setupActiveCardInstanceIdByPlayer),
                copyUuidListMap(setupBenchCardInstanceIdsByPlayer),
                Map.copyOf(setupActiveOccupiedByPlayer),
                Map.copyOf(setupBenchOccupiedCountByPlayer));
    }

    private Set<String> stringSet(Object rawValue) {
        Set<String> values = new HashSet<>();
        if (rawValue instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item != null) {
                    values.add(item.toString());
                }
            }
        }
        return values;
    }

    private ResolutionStateDto resolutionStateView(Map<String, Object> source) {
        if (source == null) {
            return ResolutionStateDto.builder().build();
        }

        return ResolutionStateDto.builder()
                .resolutionType(stringValue(source.get(ResolutionStateDto.RESOLUTION_TYPE_KEY)))
                .playerToPromoteId(uuidValue(source.get(ResolutionStateDto.PLAYER_TO_PROMOTE_ID_KEY)))
                .nextActivePlayerId(uuidValue(source.get(ResolutionStateDto.NEXT_ACTIVE_PLAYER_ID_KEY)))
                .nextTurnNumber(integerValue(source.get(ResolutionStateDto.NEXT_TURN_NUMBER_KEY), 0))
                .pendingChoicePlayerId(uuidValue(source.get(ResolutionStateDto.PENDING_CHOICE_PLAYER_ID_KEY)))
                .pendingChoiceType(stringValue(source.get(ResolutionStateDto.PENDING_CHOICE_TYPE_KEY)))
                .pendingChoicePayload(nestedMap(source, ResolutionStateDto.PENDING_CHOICE_PAYLOAD_KEY))
                .turnEndingPlayerId(uuidValue(source.get(ResolutionStateDto.TURN_ENDING_PLAYER_ID_KEY)))
                .build();
    }

    private Map<UUID, List<UUID>> copyUuidListMap(Map<UUID, List<UUID>> source) {
        Map<UUID, List<UUID>> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<UUID, List<UUID>> entry : source.entrySet()) {
            List<UUID> values = entry.getValue();
            if (values == null) {
                copiedValues.put(entry.getKey(), List.of());
            } else {
                copiedValues.put(entry.getKey(), List.copyOf(values));
            }
        }

        return Map.copyOf(copiedValues);
    }

    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        if (source == null) {
            return Map.of();
        }

        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> sourceMap)) {
            return Map.of();
        }

        Map<String, Object> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
            if (entry.getKey() != null) {
                copiedValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        return Map.copyOf(copiedValues);
    }

    private Integer integerValue(Object value, Integer defaultValue) {
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value == null) {
            return defaultValue;
        }

        return Integer.valueOf(String.valueOf(value));
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return Boolean.FALSE;
        }

        return Boolean.valueOf(String.valueOf(value));
    }

    private UUID uuidValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuidValue) {
            return uuidValue;
        }

        return UUID.fromString(String.valueOf(value));
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value);
    }

    private List<UUID> uuidListValue(Object value) {
        if (!(value instanceof List<?> sourceList)) {
            return List.of();
        }

        List<UUID> values = new ArrayList<>();
        for (Object item : sourceList) {
            UUID uuid = uuidValue(item);
            if (uuid != null) {
                values.add(uuid);
            }
        }

        return List.copyOf(values);
    }

    private int listSize(Object value) {
        return value instanceof List<?> values ? values.size() : 0;
    }

    private VisibleBoardDto buildVisibleBoard(
            Game game,
            List<GameParticipant> participants,
            List<GameCardInstance> cardInstances,
            List<PokemonInPlay> pokemonInPlay,
            SetupStateView setupStateView,
            List<GameActionType> availableActions) {
        Map<UUID, GameCardInstance> cardInstancesById = new HashMap<>();
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            cardInstancesById.put(cardInstance.getId(), cardInstance);
        }

        Map<UUID, List<PokemonInPlay>> pokemonByOwner = new HashMap<>();
        for (PokemonInPlay pokemon : safeList(pokemonInPlay)) {
            pokemonByOwner.computeIfAbsent(pokemon.getOwnerUserId(), ignored -> new ArrayList<>()).add(pokemon);
        }
        for (List<PokemonInPlay> slots : pokemonByOwner.values()) {
            slots.sort(Comparator.comparing(PokemonInPlay::getSlotPosition, Comparator.nullsLast(Integer::compareTo)));
        }

        Map<UUID, Card> cardsById = new HashMap<>();
        List<VisiblePlayerBoardDto> playerBoards = new ArrayList<>();
        for (GameParticipant participant : safeList(participants)) {
            playerBoards.add(buildVisiblePlayerBoard(
                    game,
                    participant,
                    cardInstances,
                    cardInstancesById,
                    pokemonByOwner.getOrDefault(participant.getUserId(), List.of()),
                    setupStateView,
                    availableActions,
                    cardsById));
        }

        return new VisibleBoardDto(
                null,
                List.copyOf(playerBoards),
                buildVisibleStadium(cardInstances, cardsById),
                VisibleBoardActionHintsDto.empty());
    }

    private VisiblePlayerBoardDto buildVisiblePlayerBoard(
            Game game,
            GameParticipant participant,
            List<GameCardInstance> cardInstances,
            Map<UUID, GameCardInstance> cardInstancesById,
            List<PokemonInPlay> playerPokemon,
            SetupStateView setupStateView,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        UUID playerId = participant.getUserId();
        List<GameCardInstance> ownCards = filterCardsByOwner(cardInstances, playerId);

        VisiblePokemonDto activePokemon = buildPlayerActivePokemon(
                game,
                playerId,
                playerPokemon,
                cardInstancesById,
                setupStateView,
                availableActions,
                cardsById);
        List<VisiblePokemonDto> benchPokemon = buildPlayerBenchPokemon(
                game,
                playerId,
                playerPokemon,
                cardInstancesById,
                setupStateView,
                availableActions,
                cardsById);
        List<VisiblePokemonDto> allVisiblePokemon = new ArrayList<>();
        if (activePokemon != null) {
            allVisiblePokemon.add(activePokemon);
        }
        allVisiblePokemon.addAll(benchPokemon);

        List<UUID> energyTargetIds = energyTargetPokemonIds(game, playerId, allVisiblePokemon, availableActions);
        List<UUID> trainerTargetIds = genericTrainerTargetPokemonIds(game, playerId, allVisiblePokemon, availableActions);
        List<UUID> trainerToolTargetIds = trainerToolTargetPokemonIds(game, playerId, allVisiblePokemon, availableActions);
        int playerOrder = participant.getPlayerOrder() == null ? 0 : participant.getPlayerOrder();

        return new VisiblePlayerBoardDto(
                playerId,
                playerOrder,
                Boolean.TRUE.equals(participant.getConnected()),
                false,
                setupStateView.mulliganCountByPlayer().getOrDefault(playerId, 0),
                setupStateView.pendingMulliganAcknowledgementsByPlayer().getOrDefault(playerId, Boolean.FALSE),
                setupStateView.mulliganFlowActive(),
                setupStateView.mulliganReadyForInitialSelection(),
                setupStateView.mulliganRoundNumber(),
                setupStateView.setupSelectionSubmittedByPlayer().getOrDefault(playerId, Boolean.FALSE),
                activePokemon != null
                        || setupStateView.setupActiveOccupiedByPlayer().getOrDefault(playerId, Boolean.FALSE),
                Math.max(
                        benchPokemon.size(),
                        setupStateView.setupBenchOccupiedCountByPlayer().getOrDefault(playerId, 0)),
                buildHandZone(game, playerId, ownCards, playerPokemon, setupStateView, availableActions, allVisiblePokemon, energyTargetIds, trainerTargetIds, trainerToolTargetIds, cardsById),
                buildDeckZone(ownCards, cardsById),
                buildCounterZone(CardZone.PRIZE, countZoneCards(ownCards, CardZone.PRIZE)),
                buildDiscardZone(ownCards, cardsById),
                activePokemon,
                benchPokemon);
    }

    private VisibleZoneDto buildHandZone(
            Game game,
            UUID playerId,
            List<GameCardInstance> ownCards,
            List<PokemonInPlay> playerPokemon,
            SetupStateView setupStateView,
            List<GameActionType> availableActions,
            List<VisiblePokemonDto> allVisiblePokemon,
            List<UUID> energyTargetIds,
            List<UUID> trainerTargetIds,
            List<UUID> trainerToolTargetIds,
            Map<UUID, Card> cardsById) {
        List<GameCardInstance> handCards = filterCardsByZone(ownCards, CardZone.HAND);
        if (GameStatus.SETUP.equals(game.getStatus())) {
            Set<UUID> selectedSetupCardIds = new java.util.HashSet<>();
            UUID activeCardId = setupStateView.setupActiveCardInstanceIdByPlayer().get(playerId);
            if (activeCardId != null) {
                selectedSetupCardIds.add(activeCardId);
            }
            selectedSetupCardIds.addAll(
                    setupStateView.setupBenchCardInstanceIdsByPlayer().getOrDefault(playerId, List.of()));
            handCards = handCards.stream()
                    .filter(card -> !selectedSetupCardIds.contains(card.getId()))
                    .toList();
        }
        List<VisibleCardDto> visibleCards = new ArrayList<>();
        for (GameCardInstance handCard : handCards) {
            visibleCards.add(buildVisibleHandCard(
                    game,
                    playerId,
                    handCard,
                    playerPokemon,
                    availableActions,
                    allVisiblePokemon,
                    energyTargetIds,
                    trainerTargetIds,
                    trainerToolTargetIds,
                    cardsById));
        }
        return new VisibleZoneDto(CardZone.HAND, handCards.size(), List.copyOf(visibleCards));
    }

    private VisibleZoneDto buildCounterZone(CardZone zone, int count) {
        return new VisibleZoneDto(zone, count, List.of());
    }

    private VisibleZoneDto buildDeckZone(List<GameCardInstance> ownCards, Map<UUID, Card> cardsById) {
        List<GameCardInstance> deckCards = filterCardsByZone(ownCards, CardZone.DECK);
        List<VisibleCardDto> visibleCards = new ArrayList<>();
        for (GameCardInstance deckCard : deckCards) {
            visibleCards.add(toVisibleCard(deckCard, cardsById, false, null, null, List.of()));
        }
        return new VisibleZoneDto(CardZone.DECK, deckCards.size(), List.copyOf(visibleCards));
    }

    private VisibleZoneDto buildDiscardZone(List<GameCardInstance> ownCards, Map<UUID, Card> cardsById) {
        List<GameCardInstance> discardCards = filterCardsByZone(ownCards, CardZone.DISCARD);
        List<VisibleCardDto> visibleCards = new ArrayList<>();
        for (GameCardInstance discardCard : discardCards) {
            visibleCards.add(toVisibleCard(discardCard, cardsById, false, null, null, List.of()));
        }
        return new VisibleZoneDto(CardZone.DISCARD, discardCards.size(), List.copyOf(visibleCards));
    }

    private VisiblePokemonDto buildPlayerActivePokemon(
            Game game,
            UUID playerId,
            List<PokemonInPlay> playerPokemon,
            Map<UUID, GameCardInstance> cardInstancesById,
            SetupStateView setupStateView,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        if (ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.SETUP.equals(game.getStatus())) {
            UUID setupActiveCardInstanceId = setupStateView.setupActiveCardInstanceIdByPlayer().get(playerId);
            if (setupActiveCardInstanceId == null) {
                return null;
            }
            return buildSetupPokemonView(
                    game,
                    playerId,
                    setupActiveCardInstanceId,
                    0,
                    cardInstancesById,
                    availableActions,
                    cardsById);
        }

        for (PokemonInPlay pokemon : safeList(playerPokemon)) {
            if (pokemon.getSlotPosition() != null && pokemon.getSlotPosition() == 0) {
                return buildPokemonView(game, playerId, pokemon, availableActions, cardsById);
            }
        }
        return null;
    }

    private List<VisiblePokemonDto> buildPlayerBenchPokemon(
            Game game,
            UUID playerId,
            List<PokemonInPlay> playerPokemon,
            Map<UUID, GameCardInstance> cardInstancesById,
            SetupStateView setupStateView,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        List<VisiblePokemonDto> benchPokemon = new ArrayList<>();
        if (ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.SETUP.equals(game.getStatus())) {
            List<UUID> setupBenchIds = setupStateView.setupBenchCardInstanceIdsByPlayer().getOrDefault(playerId, List.of());
            int slotPosition = 1;
            for (UUID benchCardInstanceId : setupBenchIds) {
                benchPokemon.add(buildSetupPokemonView(
                        game,
                        playerId,
                        benchCardInstanceId,
                        slotPosition++,
                        cardInstancesById,
                        availableActions,
                        cardsById));
            }
            return List.copyOf(benchPokemon);
        }

        for (PokemonInPlay pokemon : safeList(playerPokemon)) {
            if (pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0) {
                benchPokemon.add(buildPokemonView(game, playerId, pokemon, availableActions, cardsById));
            }
        }
        benchPokemon.sort(Comparator.comparing(VisiblePokemonDto::slotPosition, Comparator.nullsLast(Integer::compareTo)));
        return List.copyOf(benchPokemon);
    }

    private VisiblePokemonDto buildSetupPokemonView(
            Game game,
            UUID playerId,
            UUID cardInstanceId,
            Integer slotPosition,
            Map<UUID, GameCardInstance> cardInstancesById,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        GameCardInstance cardInstance = cardInstancesById.get(cardInstanceId);
        if (cardInstance == null) {
            return null;
        }

        VisibleCardDto activeCard = toVisibleCard(cardInstance, cardsById, false, null, null, List.of());
        boolean canPromote = hasPendingPromotionFor(game, playerId) && slotPosition != null && slotPosition > 0;

        return new VisiblePokemonDto(
                null,
                playerId,
                slotPosition,
                activeCard,
                List.of(),
                List.of(),
                List.of(),
                0,
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                false,
                canPromote,
                List.of());
    }

    private VisiblePokemonDto buildPokemonView(
            Game game,
            UUID playerId,
            PokemonInPlay pokemon,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        Card activeCardEntity = cardForId(pokemon.getActiveCardInstance().getCardId(), cardsById);
        VisibleCardDto activeCard = toVisibleCard(pokemon.getActiveCardInstance(), cardsById, false, null, null, List.of());
        List<VisibleCardDto> evolutionStack = buildEvolutionStack(pokemon.getId(), pokemon.getActiveCardInstance().getId(), cardsById);
        List<PokemonAttachedCard> attachedCards = safeList(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId()));
        List<VisibleCardDto> attachedEnergyCards = new ArrayList<>();
        List<VisibleCardDto> attachedTrainerCards = new ArrayList<>();
        for (PokemonAttachedCard attachedCard : attachedCards) {
            VisibleCardDto attachedCardView = toVisibleCard(attachedCard.getGameCardInstance(), cardsById, false, null, null, List.of());
            if (AttachedCardType.POKEMON_TOOL.equals(attachedCard.getAttachedCardType())) {
                attachedTrainerCards.add(attachedCardView);
            } else {
                attachedEnergyCards.add(attachedCardView);
            }
        }

        return new VisiblePokemonDto(
                pokemon.getId(),
                playerId,
                pokemon.getSlotPosition(),
                activeCard,
                List.copyOf(evolutionStack),
                List.copyOf(attachedEnergyCards),
                List.copyOf(attachedTrainerCards),
                pokemon.getDamageCounters(),
                safeList(specialConditionStateService.activeConditionTypes(pokemon.getId())),
                buildAttacksForPokemon(game, playerId, pokemon, availableActions, cardsById),
                buildVisibleAbilities(game, activeCardEntity, pokemon, cardsById),
                canReceiveEnergy(game, playerId, availableActions),
                canReceiveTrainer(game, playerId, pokemon, attachedTrainerCards, availableActions),
                canRetreatTo(game, playerId, pokemon, availableActions),
                hasPendingPromotionFor(game, playerId) && pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0,
                buildVisualEffects(pokemon, game.getTurnNumber()));
    }

    private List<BoardAbilityDto> boardAbilities(Game game, Card activeCard, PokemonInPlay pokemon) {
        List<AbilityDefinition> abilityDefinitions = normalizedAbilities(activeCard);
        if (abilityDefinitions.isEmpty()) {
            return List.of();
        }

        boolean abilitiesDisabled = abilitiesDisabled(pokemon, game.getTurnNumber());
        List<BoardAbilityDto> abilities = new ArrayList<>();
        for (AbilityDefinition ability : abilityDefinitions) {
            boolean available = abilityAvailable(game, pokemon, ability, abilitiesDisabled);
            abilities.add(new BoardAbilityDto(
                    ability.code().name(),
                    ability.displayName() != null ? ability.displayName() : ability.name(),
                    abilityActivationType(ability),
                    available,
                    abilityDisabledReason(game, pokemon, ability, abilitiesDisabled)));
        }
        return List.copyOf(abilities);
    }

    private List<VisibleAbilityDto> buildVisibleAbilities(
            Game game,
            Card activeCard,
            PokemonInPlay pokemon,
            Map<UUID, Card> cardsById) {
        List<AbilityDefinition> abilityDefinitions = normalizedAbilities(activeCard);
        if (abilityDefinitions.isEmpty()) {
            return List.of();
        }

        boolean abilitiesDisabled = abilitiesDisabled(pokemon, game.getTurnNumber());
        List<VisibleAbilityDto> abilities = new ArrayList<>();
        for (AbilityDefinition ability : abilityDefinitions) {
            abilities.add(new VisibleAbilityDto(
                    ability.code().name(),
                    ability.displayName() != null ? ability.displayName() : ability.name(),
                    abilityActivationType(ability),
                    abilityAvailable(game, pokemon, ability, abilitiesDisabled),
                    abilityDisabledReason(game, pokemon, ability, abilitiesDisabled),
                    deckCardOptionsForAbility(game, pokemon, activeCard, ability, cardsById)));
        }
        return List.copyOf(abilities);
    }

    private List<VisibleCardDto> deckCardOptionsForAbility(
            Game game,
            PokemonInPlay pokemon,
            Card activeCard,
            AbilityDefinition ability,
            Map<UUID, Card> cardsById) {
        if (!AbilityCode.UPSIDE_DOWN_EVOLUTION.equals(ability.code()) || game == null || pokemon == null || activeCard == null) {
            return List.of();
        }

        List<VisibleCardDto> options = new ArrayList<>();
        for (GameCardInstance deckCard : gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                game.getId(),
                pokemon.getOwnerUserId(),
                CardZone.DECK)) {
            Card candidate = cardForId(deckCard.getCardId(), cardsById);
            if (candidate == null || candidate.getEvolvesFrom() == null) {
                continue;
            }
            if (!candidate.getEvolvesFrom().equalsIgnoreCase(activeCard.getName())) {
                continue;
            }
            options.add(toVisibleCard(deckCard, cardsById, false, null, null, List.of()));
        }
        return List.copyOf(options);
    }

    private List<AbilityDefinition> normalizedAbilities(Card activeCard) {
        if (activeCard == null || activeCard.getExternalId() == null) {
            return List.of();
        }
        return abilityCatalogService.abilitiesFor(activeCard.getExternalId());
    }

    private List<AbilityTranslationDto> translatedAbilities(Card activeCard) {
        if (activeCard == null) {
            return List.of();
        }
        return cardTranslationService.findByExternalId(activeCard.getExternalId())
                .map(translation -> safeList(translation.displayAbilities()))
                .orElse(List.of());
    }

    private boolean abilitiesDisabled(PokemonInPlay pokemon, int currentTurnNumber) {
        Integer abilitiesDisabledUntilTurn = pokemon.getAbilitiesDisabledUntilTurn();
        return abilitiesDisabledUntilTurn != null && currentTurnNumber <= abilitiesDisabledUntilTurn;
    }

    private boolean abilityAvailable(Game game, PokemonInPlay pokemon, AbilityDefinition ability, boolean abilitiesDisabled) {
        if (abilitiesDisabled) {
            return false;
        }
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED.equals(ability.activation())) {
            return false;
        }
        if (!GameStatus.ACTIVE.equals(game.getStatus())) {
            return false;
        }
        if (!pokemon.getOwnerUserId().equals(game.getActivePlayerId())) {
            return false;
        }
        if (!TurnPhase.MAIN.equals(game.getCurrentPhase()) && !TurnPhase.ATTACK.equals(game.getCurrentPhase())) {
            return false;
        }
        if (ability.oncePerTurn() && abilityUsageTracker.wasUsedThisTurn(game, pokemon.getId(), ability.code())) {
            return false;
        }
        if (AbilityCode.MYSTICAL_FIRE.equals(ability.code())) {
            return handSize(game, pokemon.getOwnerUserId()) < 6 && hasCardsInDeck(game, pokemon.getOwnerUserId());
        }
        if (AbilityCode.WATER_SHURIKEN.equals(ability.code())
                && (!hasWaterEnergyInHand(game, pokemon.getOwnerUserId()) || !hasOpponentPokemonInPlay(game, pokemon.getOwnerUserId()))) {
            return false;
        }
        if (AbilityCode.UPSIDE_DOWN_EVOLUTION.equals(ability.code())) {
            return specialConditionStateService.activeConditionTypes(pokemon.getId()).contains(SpecialConditionType.CONFUSED)
                    && hasDeckEvolutionFor(game, pokemon);
        }
        return true;
    }

    private String abilityActivationType(AbilityDefinition ability) {
        if (ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED.equals(ability.activation())) {
            return "ACTIVE";
        }
        return "PASSIVE";
    }

    private String abilityDisabledReason(Game game, PokemonInPlay pokemon, AbilityDefinition ability, boolean abilitiesDisabled) {
        if (abilitiesDisabled) {
            return "Las habilidades de este Pokemon estan bloqueadas por un efecto.";
        }
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType.ACTIVATED.equals(ability.activation())) {
            return "Esta habilidad es pasiva.";
        }
        if (!GameStatus.ACTIVE.equals(game.getStatus()) || !pokemon.getOwnerUserId().equals(game.getActivePlayerId())) {
            return "Habilidad no disponible en este momento.";
        }
        if (!TurnPhase.MAIN.equals(game.getCurrentPhase()) && !TurnPhase.ATTACK.equals(game.getCurrentPhase())) {
            return "Habilidad no disponible en este momento.";
        }
        if (ability.oncePerTurn() && abilityUsageTracker.wasUsedThisTurn(game, pokemon.getId(), ability.code())) {
            return "Esta habilidad ya fue usada este turno.";
        }
        if (AbilityCode.MYSTICAL_FIRE.equals(ability.code()) && handSize(game, pokemon.getOwnerUserId()) >= 6) {
            return "Ya tenes 6 o mas cartas en mano.";
        }
        if (AbilityCode.MYSTICAL_FIRE.equals(ability.code()) && !hasCardsInDeck(game, pokemon.getOwnerUserId())) {
            return "No hay cartas en el mazo para robar.";
        }
        if (AbilityCode.WATER_SHURIKEN.equals(ability.code()) && !hasWaterEnergyInHand(game, pokemon.getOwnerUserId())) {
            return "Necesitas una Energia Agua en la mano.";
        }
        if (AbilityCode.WATER_SHURIKEN.equals(ability.code()) && !hasOpponentPokemonInPlay(game, pokemon.getOwnerUserId())) {
            return "El rival no tiene Pokemon en juego.";
        }
        if (AbilityCode.UPSIDE_DOWN_EVOLUTION.equals(ability.code())) {
            if (!specialConditionStateService.activeConditionTypes(pokemon.getId()).contains(SpecialConditionType.CONFUSED)) {
                return "Inkay debe estar Confundido para usar esta habilidad.";
            }
            if (!hasDeckEvolutionFor(game, pokemon)) {
                return "No hay una evolucion valida de Inkay en el mazo.";
            }
        }
        return null;
    }

    private int handSize(Game game, UUID ownerUserId) {
        return gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(game.getId(), ownerUserId, CardZone.HAND).size();
    }

    private boolean hasCardsInDeck(Game game, UUID ownerUserId) {
        return !gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(game.getId(), ownerUserId, CardZone.DECK).isEmpty();
    }

    private boolean hasWaterEnergyInHand(Game game, UUID ownerUserId) {
        for (GameCardInstance handCard : gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                game.getId(),
                ownerUserId,
                CardZone.HAND)) {
            Card card = cardService.getCardEntityById(handCard.getCardId());
            if (isWaterEnergy(card)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWaterEnergy(Card card) {
        if (card == null) {
            return false;
        }
        if (card.getPokemonType() != null && "Water".equalsIgnoreCase(card.getPokemonType())) {
            return true;
        }
        return card.getName() != null && "Water Energy".equalsIgnoreCase(card.getName());
    }

    private boolean hasOpponentPokemonInPlay(Game game, UUID ownerUserId) {
        for (PokemonInPlay candidate : pokemonInPlayStateService.findByGameIdOrdered(game.getId())) {
            if (!ownerUserId.equals(candidate.getOwnerUserId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDeckEvolutionFor(Game game, PokemonInPlay pokemon) {
        if (game == null || pokemon == null || pokemon.getActiveCardInstance() == null) {
            return false;
        }
        Card activeCard = cardService.getCardEntityById(pokemon.getActiveCardInstance().getCardId());
        for (GameCardInstance deckCard : gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                game.getId(),
                pokemon.getOwnerUserId(),
                CardZone.DECK)) {
            Card candidate = cardService.getCardEntityById(deckCard.getCardId());
            if (candidate.getEvolvesFrom() != null && candidate.getEvolvesFrom().equalsIgnoreCase(activeCard.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the public, cosmetic-only list of temporary effects currently affecting this
     * Pokemon, so the frontend can render an indicator (e.g. Harden's damage-prevention shield)
     * without recomputing any rule itself. Mirrors the exact fields/condition that
     * {@link ar.edu.utn.frc.tup.piii.services.game.attack.DamageProtectionService} uses to
     * decide whether protection is still in effect, so the visual never drifts from the real
     * rule: it covers both the remainder of the turn Harden was used on and the opponent's
     * following turn, then disappears once that window has fully elapsed.
     */
    private List<VisiblePokemonEffectDto> buildVisualEffects(PokemonInPlay pokemon, int currentTurnNumber) {
        Integer protectionTurn = pokemon.getDamageProtectionTurn();
        if (protectionTurn == null || protectionTurn < currentTurnNumber) {
            return List.of();
        }
        return List.of(VisiblePokemonEffectDto.hardenShield());
    }

    private List<VisibleCardDto> buildEvolutionStack(UUID pokemonInPlayId, UUID activeCardInstanceId, Map<UUID, Card> cardsById) {
        List<VisibleCardDto> evolutionStack = new ArrayList<>();
        for (PokemonEvolutionStack stackEntry : safeList(pokemonEvolutionStackStateService.findByPokemonInPlayId(pokemonInPlayId))) {
            GameCardInstance stackCardInstance = stackEntry.getGameCardInstance();
            if (stackCardInstance == null || activeCardInstanceId.equals(stackCardInstance.getId())) {
                continue;
            }
            evolutionStack.add(toVisibleCard(stackCardInstance, cardsById, false, null, null, List.of()));
        }
        return List.copyOf(evolutionStack);
    }

    private List<VisibleAttackDto> buildAttacksForPokemon(
            Game game,
            UUID playerId,
            PokemonInPlay pokemon,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        Card activeCard = cardForId(pokemon.getActiveCardInstance().getCardId(), cardsById);
        if (activeCard == null || activeCard.getAttacks() == null || activeCard.getAttacks().isEmpty()) {
            return List.of();
        }

        Set<UUID> affordableAttackIds = affordableAttacksFor(pokemon);
        boolean attackAvailable = availableActions.contains(GameActionType.DECLARE_ATTACK)
                && playerId.equals(game.getActivePlayerId())
                && (TurnPhase.MAIN.equals(game.getCurrentPhase()) || TurnPhase.ATTACK.equals(game.getCurrentPhase()));
        List<UUID> defaultTargetIds = defaultAttackTargetPokemonIds(game, playerId);
        List<VisibleAttackDto> attacks = new ArrayList<>();
        for (Attack attack : safeSet(activeCard.getAttacks())) {
            if (attack == null) {
                continue;
            }
            boolean affordable = affordableAttackIds.contains(attack.getId());
            boolean blockedByTorment = isBlockedAttackForCurrentTurn(game, pokemon, attack);
            boolean enabled = attackAvailable && affordable && !blockedByTorment;
            String disabledReason = null;
            if (!attackAvailable) {
                disabledReason = "Ataque no disponible en este momento.";
            } else if (!affordable) {
                disabledReason = "No alcanza la energia para este ataque.";
            } else if (blockedByTorment) {
                disabledReason = "Este ataque esta bloqueado este turno.";
            }

            List<VisibleAttackCostDto> costs = new ArrayList<>();
            for (AttackCost cost : safeSet(attack.getCosts())) {
                costs.add(new VisibleAttackCostDto(cost.getEnergyType(), cost.getQuantity()));
            }

            AttackTargetMode attackTargetMode = attackTargetMode(safeAttackEffectDefinition(activeCard, attack));
            List<UUID> targetIds = attackTargetMode != null
                    ? attackTargetPokemonIds(game, playerId, attackTargetMode)
                    : defaultTargetIds;
            boolean requiresTarget = attackTargetMode != null && !targetIds.isEmpty();

            attacks.add(new VisibleAttackDto(
                    attack.getId(),
                    attack.getName(),
                    attack.getDamageText(),
                    attack.getBaseDamage(),
                    attack.getEffectText(),
                    List.copyOf(costs),
                    enabled,
                    disabledReason,
                    requiresTarget,
                    targetIds,
                    requiresTarget && attackTargetMode.targetsOwnPokemon()));
        }
        return List.copyOf(attacks);
    }

    private boolean isBlockedAttackForCurrentTurn(Game game, PokemonInPlay pokemon, Attack attack) {
        return pokemon.getBlockedAttackTurn() != null
                && pokemon.getBlockedAttackOrder() != null
                && game.getTurnNumber() == pokemon.getBlockedAttackTurn()
                && attack.getAttackOrder() == pokemon.getBlockedAttackOrder();
    }

    private AttackEffectDefinition safeAttackEffectDefinition(Card card, Attack attack) {
        try {
            return attackEffectDefinitionReader.read(card, attack);
        } catch (InvalidGameActionException exception) {
            return null;
        }
    }

    private AttackTargetMode attackTargetMode(AttackEffectDefinition definition) {
        if (definition == null || definition.isEmpty()) {
            return null;
        }
        for (AttackEffectOperation operation : definition.operations()) {
            if (operation == null || isAutomaticBenchSweep(operation)) {
                // OWN_BENCH/OPPONENT_BENCH is also how "hit every Pokemon on this bench
                // automatically" effects (e.g. Earthquake's ALL_BENCH_DAMAGE) describe who they
                // hit. Those effects never read a selected target id from the attack-declare
                // payload, so they must never force the FE's single-target picker (which is only
                // meant for effects like HEAL_DAMAGE/MOVE_ENERGY_TO_BENCH/BENCH_DAMAGE that do).
                continue;
            }
            if ("OWN_BENCH".equals(operation.target()) || "OWN_ANY".equals(operation.target())) {
                return new AttackTargetMode(operation.target(), true);
            }
            if ("OPPONENT_BENCH".equals(operation.target()) || "OPPONENT_ANY".equals(operation.target())) {
                return new AttackTargetMode(operation.target(), false);
            }
        }
        return null;
    }

    private boolean isAutomaticBenchSweep(AttackEffectOperation operation) {
        return AUTOMATIC_BENCH_SWEEP_OPERATION_TYPES.contains(operation.type());
    }

    private List<UUID> attackTargetPokemonIds(Game game, UUID attackerUserId, AttackTargetMode attackTargetMode) {
        if (attackTargetMode.targetsOwnPokemon()) {
            return ownAttackTargetPokemonIds(game, attackerUserId, "OWN_BENCH".equals(attackTargetMode.target()));
        }
        return opponentAttackTargetPokemonIds(game, attackerUserId, "OPPONENT_BENCH".equals(attackTargetMode.target()));
    }

    private List<UUID> ownAttackTargetPokemonIds(Game game, UUID attackerUserId, boolean benchOnly) {
        List<UUID> ids = new ArrayList<>();
        for (PokemonInPlay pokemonInPlay : pokemonInPlayStateService.findByGameIdAndOwnerUserId(game.getId(), attackerUserId)) {
            if (benchOnly && pokemonInPlay.getSlotPosition() != null && pokemonInPlay.getSlotPosition() == 0) {
                continue;
            }
            ids.add(pokemonInPlay.getId());
        }
        return List.copyOf(ids);
    }

    private List<UUID> opponentAttackTargetPokemonIds(Game game, UUID attackerUserId, boolean benchOnly) {
        UUID defenderUserId = opponentUserId(game, attackerUserId);
        if (defenderUserId == null) {
            return List.of();
        }

        List<UUID> ids = new ArrayList<>();
        for (PokemonInPlay pokemonInPlay : pokemonInPlayStateService.findByGameIdAndOwnerUserId(game.getId(), defenderUserId)) {
            if (benchOnly && pokemonInPlay.getSlotPosition() != null && pokemonInPlay.getSlotPosition() == 0) {
                continue;
            }
            if (pokemonInPlay.getId() != null) {
                ids.add(pokemonInPlay.getId());
            }
        }
        return List.copyOf(ids);
    }

    private VisibleStadiumDto buildVisibleStadium(List<GameCardInstance> cardInstances, Map<UUID, Card> cardsById) {
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            if (CardZone.STADIUM.equals(cardInstance.getZone())) {
                return new VisibleStadiumDto(
                        toVisibleCard(cardInstance, cardsById, false, null, null, List.of()),
                        cardInstance.getOwnerUserId());
            }
        }
        return null;
    }

    private VisibleCardDto buildVisibleHandCard(
            Game game,
            UUID playerId,
            GameCardInstance handCard,
            List<PokemonInPlay> playerPokemon,
            List<GameActionType> availableActions,
            List<VisiblePokemonDto> allVisiblePokemon,
            List<UUID> energyTargetIds,
            List<UUID> trainerTargetIds,
            List<UUID> trainerToolTargetIds,
            Map<UUID, Card> cardsById) {
        Card handCardEntity = cardForId(handCard.getCardId(), cardsById);
        int currentTurnNumber = game.getTurnNumber() == null ? 0 : game.getTurnNumber().intValue();
        List<UUID> evolutionTargetIds = evolutionTargetsFor(
                handCardEntity, playerPokemon, currentTurnNumber, cardsById);
        HandCardPlayability playability = resolveHandCardPlayability(
                game,
                playerId,
                handCard,
                availableActions,
                allVisiblePokemon.size(),
                energyTargetIds,
                trainerTargetIds,
                trainerToolTargetIds,
                evolutionTargetIds,
                cardsById);

        return toVisibleCard(
                handCard,
                cardsById,
                playability.playable(),
                playability.suggestedAction(),
                playability.disabledReason(),
                playability.validTargetPokemonInPlayIds());
    }

    private HandCardPlayability resolveHandCardPlayability(
            Game game,
            UUID playerId,
            GameCardInstance handCard,
            List<GameActionType> availableActions,
            int pokemonInPlayCount,
            List<UUID> energyTargetIds,
            List<UUID> trainerTargetIds,
            List<UUID> trainerToolTargetIds,
            List<UUID> evolutionTargetIds,
            Map<UUID, Card> cardsById) {
        Card card = cardForId(handCard.getCardId(), cardsById);
        if (card == null) {
            return HandCardPlayability.notPlayable("No se pudo cargar la carta.");
        }

        boolean isPlayersTurn = playerId.equals(game.getActivePlayerId())
                || ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.SETUP.equals(game.getStatus());
        if (!isPlayersTurn) {
            return HandCardPlayability.notPlayable("No es el turno del jugador.");
        }

        if (ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.SETUP.equals(game.getStatus())) {
            if (availableActions.contains(GameActionType.CHOOSE_INITIAL_POKEMON) && card.isBasicStage()) {
                return HandCardPlayability.playable(GameActionType.CHOOSE_INITIAL_POKEMON, List.of());
            }
            return HandCardPlayability.notPlayable("Solo se pueden elegir Pokemon basicos durante el setup.");
        }

        if (availableActions.contains(GameActionType.PLAY_BASIC_POKEMON) && card.isBasicStage()) {
            if (pokemonInPlayCount >= 6) {
                return HandCardPlayability.notPlayable("La banca esta llena.");
            }
            return HandCardPlayability.playable(GameActionType.PLAY_BASIC_POKEMON, List.of());
        }

        if (availableActions.contains(GameActionType.ATTACH_ENERGY)
                && (CardCategory.BASIC_ENERGY.equals(card.getCategory()) || CardCategory.SPECIAL_ENERGY.equals(card.getCategory()))) {
            if (energyTargetIds.isEmpty()) {
                return HandCardPlayability.notPlayable("No hay Pokemon validos para unir energia.");
            }
            return HandCardPlayability.playable(GameActionType.ATTACH_ENERGY, energyTargetIds);
        }

        if (CardCategory.STADIUM_TRAINER.equals(card.getCategory())) {
            if (!availableActions.contains(GameActionType.PLAY_TRAINER)) {
                return HandCardPlayability.notPlayable("No se puede jugar un entrenador ahora.");
            }
            return HandCardPlayability.playable(GameActionType.PLAY_TRAINER, List.of());
        }

        if (CardCategory.POKEMON_TOOL_TRAINER.equals(card.getCategory())) {
            if (!availableActions.contains(GameActionType.PLAY_TRAINER)) {
                return HandCardPlayability.notPlayable("No se puede jugar un entrenador ahora.");
            }
            if (trainerToolTargetIds.isEmpty()) {
                return HandCardPlayability.notPlayable("No hay Pokemon validos para unir una herramienta.");
            }
            return HandCardPlayability.playable(GameActionType.PLAY_TRAINER, trainerToolTargetIds);
        }

        if (isStandardTrainer(card)) {
            if (!availableActions.contains(GameActionType.PLAY_TRAINER)) {
                return HandCardPlayability.notPlayable("No se puede jugar un entrenador ahora.");
            }
            if (CardCategory.ITEM_TRAINER.equals(card.getCategory())
                    && passiveAbilityService.blocksItemCards(game, playerId)) {
                return HandCardPlayability.notPlayable("El Trevenant activo del rival impide jugar cartas Item.");
            }
            return HandCardPlayability.playable(GameActionType.PLAY_TRAINER, trainerTargetIds);
        }

        if (CardCategory.STAGE_1_POKEMON.equals(card.getCategory())
                || CardCategory.STAGE_2_POKEMON.equals(card.getCategory())
                || CardCategory.MEGA_POKEMON.equals(card.getCategory())) {
            if (!availableActions.contains(GameActionType.EVOLVE_POKEMON)) {
                return HandCardPlayability.notPlayable("La evolucion no esta disponible en este momento.");
            }
            List<UUID> resolvedEvolutionTargets = evolutionTargetIds == null ? List.of() : evolutionTargetIds;
            if (resolvedEvolutionTargets.isEmpty()) {
                return HandCardPlayability.notPlayable("No hay Pokemon en juego compatibles con esta evolucion.");
            }
            return HandCardPlayability.playable(GameActionType.EVOLVE_POKEMON, resolvedEvolutionTargets);
        }

        return HandCardPlayability.notPlayable("La carta todavia no tiene una accion jugable desde UI.");
    }

    private Map<UUID, HandCardPlayability> handPlayabilityFor(
            Game game,
            UUID playerId,
            List<GameCardInstance> ownerCards,
            List<PokemonInPlay> ownerPokemon,
            List<GameActionType> availableActions,
            Map<UUID, Card> cardsById) {
        Map<UUID, HandCardPlayability> result = new HashMap<>();
        List<GameCardInstance> handCards = new ArrayList<>();
        for (GameCardInstance cardInstance : safeList(ownerCards)) {
            if (CardZone.HAND.equals(cardInstance.getZone())) {
                handCards.add(cardInstance);
            }
        }
        if (handCards.isEmpty()) {
            return result;
        }

        int pokemonInPlayCount = (int) safeList(ownerPokemon).stream()
                .filter(pokemon -> pokemon != null && pokemon.getId() != null)
                .count();
        List<UUID> energyTargetIds = handEnergyTargetIds(game, playerId, ownerPokemon, availableActions);
        List<UUID> trainerTargetIds = handGenericTrainerTargetIds(game, playerId, ownerPokemon, availableActions);
        List<UUID> trainerToolTargetIds = handTrainerToolTargetIds(game, playerId, ownerPokemon, availableActions);
        int currentTurnNumber = game.getTurnNumber() == null ? 0 : game.getTurnNumber().intValue();

        for (GameCardInstance handCard : handCards) {
            Card handCardEntity = cardForId(handCard.getCardId(), cardsById);
            List<UUID> evolutionTargetIds = evolutionTargetsFor(
                    handCardEntity, ownerPokemon, currentTurnNumber, cardsById);
            HandCardPlayability playability = resolveHandCardPlayability(
                    game,
                    playerId,
                    handCard,
                    availableActions,
                    pokemonInPlayCount,
                    energyTargetIds,
                    trainerTargetIds,
                    trainerToolTargetIds,
                    evolutionTargetIds,
                    cardsById);
            if (handCard.getId() != null) {
                result.put(handCard.getId(), playability);
            }
        }

        return Map.copyOf(result);
    }

    private List<UUID> handEnergyTargetIds(
            Game game, UUID playerId, List<PokemonInPlay> ownerPokemon, List<GameActionType> availableActions) {
        if (!canReceiveEnergy(game, playerId, availableActions)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (PokemonInPlay pokemon : safeList(ownerPokemon)) {
            if (pokemon != null && pokemon.getId() != null) {
                targetIds.add(pokemon.getId());
            }
        }
        return List.copyOf(targetIds);
    }

    private List<UUID> handGenericTrainerTargetIds(
            Game game, UUID playerId, List<PokemonInPlay> ownerPokemon, List<GameActionType> availableActions) {
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                || !playerId.equals(game.getActivePlayerId())
                || !availableActions.contains(GameActionType.PLAY_TRAINER)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (PokemonInPlay pokemon : safeList(ownerPokemon)) {
            if (pokemon != null
                    && pokemon.getId() != null
                    && pokemon.getDamageCounters() != null
                    && pokemon.getDamageCounters() > 0) {
                targetIds.add(pokemon.getId());
            }
        }
        return List.copyOf(targetIds);
    }

    private List<UUID> handTrainerToolTargetIds(
            Game game, UUID playerId, List<PokemonInPlay> ownerPokemon, List<GameActionType> availableActions) {
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                || !playerId.equals(game.getActivePlayerId())
                || !availableActions.contains(GameActionType.PLAY_TRAINER)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (PokemonInPlay pokemon : safeList(ownerPokemon)) {
            if (pokemon != null
                    && pokemon.getId() != null
                    && !hasAttachedPokemonTool(pokemon.getId())) {
                targetIds.add(pokemon.getId());
            }
        }
        return List.copyOf(targetIds);
    }

    private boolean hasAttachedPokemonTool(UUID pokemonInPlayId) {
        for (PokemonAttachedCard attachedCard : safeList(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemonInPlayId))) {
            if (AttachedCardType.POKEMON_TOOL.equals(attachedCard.getAttachedCardType())) {
                return true;
            }
        }
        return false;
    }

    private List<UUID> evolutionTargetsFor(
            Card evolutionCard,
            List<PokemonInPlay> ownerPokemon,
            int currentTurnNumber,
            Map<UUID, Card> cardsById) {
        if (evolutionCard == null
                || (!CardCategory.STAGE_1_POKEMON.equals(evolutionCard.getCategory())
                        && !CardCategory.STAGE_2_POKEMON.equals(evolutionCard.getCategory())
                        && !CardCategory.MEGA_POKEMON.equals(evolutionCard.getCategory()))) {
            return List.of();
        }
        if (evolutionCard.getEvolvesFrom() == null || evolutionCard.getEvolvesFrom().isBlank()) {
            return List.of();
        }

        List<UUID> targetIds = new ArrayList<>();
        for (PokemonInPlay pokemon : safeList(ownerPokemon)) {
            if (pokemon != null && pokemon.getId() != null
                    && canEvolveOnto(evolutionCard, pokemon, currentTurnNumber, cardsById)) {
                targetIds.add(pokemon.getId());
            }
        }
        return List.copyOf(targetIds);
    }

    private boolean canEvolveOnto(
            Card evolutionCard,
            PokemonInPlay pokemon,
            int currentTurnNumber,
            Map<UUID, Card> cardsById) {
        Optional<PokemonEvolutionStack> topStackResult =
                pokemonEvolutionStackStateService.findTopByPokemonInPlayId(pokemon.getId());
        if (topStackResult.isEmpty()) {
            return false;
        }
        PokemonEvolutionStack topStack = topStackResult.get();
        Integer createdAtTurn = topStack.getCreatedAtTurn();
        if (createdAtTurn != null && createdAtTurn.intValue() == currentTurnNumber) {
            return false;
        }
        Integer stackOrder = topStack.getStackOrder();
        if (stackOrder == null) {
            return false;
        }
        GameCardInstance topCardInstance = topStack.getGameCardInstance();
        if (topCardInstance == null) {
            return false;
        }
        Card topCard = cardForId(topCardInstance.getCardId(), cardsById);
        if (topCard == null || topCard.getName() == null) {
            return false;
        }
        if (!evolutionCard.getEvolvesFrom().equalsIgnoreCase(topCard.getName())) {
            return false;
        }
        if (CardCategory.MEGA_POKEMON.equals(evolutionCard.getCategory())) {
            return stackOrder.intValue() == 0 && CardCategory.POKEMON_EX.equals(topCard.getCategory());
        }
        if (CardCategory.STAGE_1_POKEMON.equals(evolutionCard.getCategory())) {
            return stackOrder.intValue() == 0 && topCard.isBasicStage();
        }
        if (CardCategory.STAGE_2_POKEMON.equals(evolutionCard.getCategory())) {
            return stackOrder.intValue() == 1
                    && CardCategory.STAGE_1_POKEMON.equals(topCard.getCategory());
        }
        return false;
    }

    private VisibleCardDto toVisibleCard(
            GameCardInstance cardInstance,
            Map<UUID, Card> cardsById,
            boolean playable,
            GameActionType suggestedAction,
            String disabledReason,
            List<UUID> validTargetPokemonInPlayIds) {
        Card card = cardForId(cardInstance.getCardId(), cardsById);
        if (card == null) {
            return new VisibleCardDto(
                    cardInstance.getId(),
                    cardInstance.getCardId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Boolean.TRUE.equals(cardInstance.getFaceDown()),
                    playable,
                    suggestedAction,
                    disabledReason,
                    validTargetPokemonInPlayIds);
        }

        return new VisibleCardDto(
                cardInstance.getId(),
                card.getId(),
                card.getName(),
                card.getExternalId(),
                card.getSetCode(),
                card.getNumber(),
                card.getSupertype(),
                card.getCategory(),
                card.getSubtype(),
                card.getImageSmallUrl(),
                card.getImageLargeUrl(),
                card.getHp(),
                Boolean.TRUE.equals(cardInstance.getFaceDown()),
                playable,
                suggestedAction,
                disabledReason,
                validTargetPokemonInPlayIds);
    }

    private Card cardForId(UUID cardId, Map<UUID, Card> cardsById) {
        if (cardId == null) {
            return null;
        }
        Card cachedCard = cardsById.get(cardId);
        if (cachedCard != null) {
            return cachedCard;
        }

        Card card = findCardOrNull(cardId);
        if (card != null) {
            cardsById.put(cardId, card);
        }
        return card;
    }

    private List<GameCardInstance> filterCardsByOwner(List<GameCardInstance> cardInstances, UUID ownerUserId) {
        List<GameCardInstance> ownCards = new ArrayList<>();
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            if (ownerUserId.equals(cardInstance.getOwnerUserId())) {
                ownCards.add(cardInstance);
            }
        }
        ownCards.sort(Comparator.comparing(GameCardInstance::getZone)
                .thenComparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)));
        return List.copyOf(ownCards);
    }

    private List<GameCardInstance> filterCardsByZone(List<GameCardInstance> cardInstances, CardZone zone) {
        List<GameCardInstance> zoneCards = new ArrayList<>();
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            if (zone.equals(cardInstance.getZone())) {
                zoneCards.add(cardInstance);
            }
        }
        zoneCards.sort(Comparator.comparing(GameCardInstance::getZonePosition, Comparator.nullsLast(Integer::compareTo)));
        return List.copyOf(zoneCards);
    }

    private int countZoneCards(List<GameCardInstance> cardInstances, CardZone zone) {
        int count = 0;
        for (GameCardInstance cardInstance : safeList(cardInstances)) {
            if (zone.equals(cardInstance.getZone())) {
                count++;
            }
        }
        return count;
    }

    private boolean isStandardTrainer(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }
        return CardCategory.ITEM_TRAINER.equals(card.getCategory())
                || CardCategory.SUPPORTER_TRAINER.equals(card.getCategory());
    }

    private boolean canReceiveEnergy(Game game, UUID playerId, List<GameActionType> availableActions) {
        return ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                && playerId.equals(game.getActivePlayerId())
                && availableActions.contains(GameActionType.ATTACH_ENERGY);
    }

    private boolean canReceiveTrainer(
            Game game,
            UUID playerId,
            PokemonInPlay pokemon,
            List<VisibleCardDto> attachedTrainerCards,
            List<GameActionType> availableActions) {
        return ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                && playerId.equals(game.getActivePlayerId())
                && availableActions.contains(GameActionType.PLAY_TRAINER)
                && pokemon.getId() != null
                && attachedTrainerCards.isEmpty();
    }

    private boolean canRetreatTo(Game game, UUID playerId, PokemonInPlay pokemon, List<GameActionType> availableActions) {
        return ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                && playerId.equals(game.getActivePlayerId())
                && availableActions.contains(GameActionType.RETREAT)
                && pokemon.getSlotPosition() != null
                && pokemon.getSlotPosition() > 0;
    }

    private boolean hasPendingPromotionFor(Game game, UUID playerId) {
        ResolutionStateDto resolutionState = resolutionStateView(game.getResolutionState());
        return resolutionState.hasPendingPromotion() && playerId.equals(resolutionState.playerToPromoteId());
    }

    private List<UUID> energyTargetPokemonIds(
            Game game,
            UUID playerId,
            List<VisiblePokemonDto> allVisiblePokemon,
            List<GameActionType> availableActions) {
        if (!canReceiveEnergy(game, playerId, availableActions)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (VisiblePokemonDto pokemon : allVisiblePokemon) {
            if (pokemon != null && pokemon.pokemonInPlayId() != null) {
                targetIds.add(pokemon.pokemonInPlayId());
            }
        }
        return List.copyOf(targetIds);
    }

    private List<UUID> genericTrainerTargetPokemonIds(
            Game game,
            UUID playerId,
            List<VisiblePokemonDto> allVisiblePokemon,
            List<GameActionType> availableActions) {
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                || !playerId.equals(game.getActivePlayerId())
                || !availableActions.contains(GameActionType.PLAY_TRAINER)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (VisiblePokemonDto pokemon : allVisiblePokemon) {
            if (pokemon != null
                    && pokemon.pokemonInPlayId() != null
                    && pokemon.damageCounters() != null
                    && pokemon.damageCounters() > 0) {
                targetIds.add(pokemon.pokemonInPlayId());
            }
        }
        return List.copyOf(targetIds);
    }

    private List<UUID> trainerToolTargetPokemonIds(
            Game game,
            UUID playerId,
            List<VisiblePokemonDto> allVisiblePokemon,
            List<GameActionType> availableActions) {
        if (!ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus.ACTIVE.equals(game.getStatus())
                || !playerId.equals(game.getActivePlayerId())
                || !availableActions.contains(GameActionType.PLAY_TRAINER)) {
            return List.of();
        }
        List<UUID> targetIds = new ArrayList<>();
        for (VisiblePokemonDto pokemon : allVisiblePokemon) {
            if (pokemon != null
                    && pokemon.pokemonInPlayId() != null
                    && pokemon.attachedTrainerCards().isEmpty()) {
                targetIds.add(pokemon.pokemonInPlayId());
            }
        }
        return List.copyOf(targetIds);
    }

    private List<UUID> defaultAttackTargetPokemonIds(Game game, UUID attackerUserId) {
        UUID defenderUserId = opponentUserId(game, attackerUserId);
        if (defenderUserId == null) {
            return List.of();
        }

        Optional<PokemonInPlay> activePokemon = pokemonInPlayStateService.findActivePokemon(game.getId(), defenderUserId);
        if (activePokemon.isEmpty()) {
            return List.of();
        }

        return List.of(activePokemon.get().getId());
    }

    private UUID opponentUserId(Game game, UUID attackerUserId) {
        for (UUID playerId : gameParticipantStateService.findPlayerIds(game.getId())) {
            if (!playerId.equals(attackerUserId)) {
                return playerId;
            }
        }
        return null;
    }

    private int compareNullableInteger(Integer left, Integer right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }

        return Integer.compare(left, right);
    }

    private <T> List<T> safeList(List<T> source) {
        if (source == null) {
            return List.of();
        }

        return source;
    }

    private <T> Set<T> safeSet(Set<T> source) {
        if (source == null) {
            return Set.of();
        }

        return source;
    }

    private record SetupStateView(
            Map<UUID, Integer> mulliganCountByPlayer,
            Map<UUID, Boolean> pendingMulliganAcknowledgementsByPlayer,
            Map<UUID, Boolean> mulliganCurrentPlayerByPlayer,
            boolean mulliganFlowActive,
            boolean mulliganReadyForInitialSelection,
            int mulliganRoundNumber,
            Map<UUID, Boolean> setupSelectionSubmittedByPlayer,
            Map<UUID, UUID> setupActiveCardInstanceIdByPlayer,
            Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer,
            Map<UUID, Boolean> setupActiveOccupiedByPlayer,
            Map<UUID, Integer> setupBenchOccupiedCountByPlayer) {
    }

    private record HandCardPlayability(
            boolean playable,
            GameActionType suggestedAction,
            String disabledReason,
            List<UUID> validTargetPokemonInPlayIds) {

        static HandCardPlayability playable(GameActionType suggestedAction, List<UUID> targetIds) {
            return new HandCardPlayability(
                    true,
                    suggestedAction,
                    null,
                    targetIds == null ? List.of() : List.copyOf(targetIds));
        }

        static HandCardPlayability notPlayable(String reason) {
            return new HandCardPlayability(false, null, reason, List.of());
        }
    }

    private record AttackTargetMode(String target, boolean targetsOwnPokemon) {
    }
}
