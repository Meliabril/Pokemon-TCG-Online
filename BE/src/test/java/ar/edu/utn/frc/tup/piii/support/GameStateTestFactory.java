package ar.edu.utn.frc.tup.piii.support;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GameStateTestFactory {

    private GameStateTestFactory() {
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                playerIds,
                false,
                false,
                false,
                Map.of(),
                Map.of(),
                turnNumber,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static Map<UUID, Integer> benchCountByPlayer(GameStateDto state) {
        Map<UUID, Integer> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().benchPokemonCount());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer(GameStateDto state) {
        Map<UUID, List<SpecialConditionType>> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().activePokemonConditions());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, List<UUID>> cardsInHandByPlayer(GameStateDto state) {
        Map<UUID, List<UUID>> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().cardIdsInHand());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, Set<UUID>> affordableAttacksByPlayer(GameStateDto state) {
        Map<UUID, Set<UUID>> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().affordableAttackIds());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, Integer> mulliganCountByPlayer(GameStateDto state) {
        Map<UUID, Integer> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().mulliganCount());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, Boolean> mulliganNoticePendingByPlayer(GameStateDto state) {
        Map<UUID, Boolean> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().mulliganNoticePending());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, Boolean> mulliganCurrentPlayerByPlayer(GameStateDto state) {
        Map<UUID, Boolean> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().mulliganCurrentPlayer());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, Boolean> setupSelectionSubmittedByPlayer(GameStateDto state) {
        Map<UUID, Boolean> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().initialPokemonSelectionSubmitted());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, UUID> setupActiveCardInstanceIdByPlayer(GameStateDto state) {
        Map<UUID, UUID> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            UUID activeCardInstanceId = entry.getValue().initialActiveCardInstanceId();
            if (activeCardInstanceId != null) {
                valuesByPlayer.put(entry.getKey(), activeCardInstanceId);
            }
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer(GameStateDto state) {
        Map<UUID, List<UUID>> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().initialBenchCardInstanceIds());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer(GameStateDto state) {
        Map<UUID, List<UUID>> valuesByPlayer = new HashMap<>();
        if (state == null) {
            return Map.of();
        }

        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            valuesByPlayer.put(entry.getKey(), entry.getValue().cardInstanceIdsInHand());
        }

        return Map.copyOf(valuesByPlayer);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                playerIds,
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                benchCountByPlayer,
                activePokemonConditionsByPlayer,
                turnNumber,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Integer currentTurnNumber,
            UUID playerWhoWentFirstId,
            Map<UUID, Integer> pokemonEnteredPlayTurnMap,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                playerIds,
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                benchCountByPlayer,
                activePokemonConditionsByPlayer,
                currentTurnNumber,
                playerWhoWentFirstId,
                pokemonEnteredPlayTurnMap,
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Integer currentTurnNumber,
            UUID playerWhoWentFirstId,
            Map<UUID, Integer> pokemonEnteredPlayTurnMap,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                playerIds,
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                benchCountByPlayer,
                activePokemonConditionsByPlayer,
                currentTurnNumber,
                playerWhoWentFirstId,
                pokemonEnteredPlayTurnMap,
                cardsInHandByPlayer,
                affordableAttacksByPlayer,
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                defaultPlayerIds(activePlayerId),
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                Map.of(),
                Map.of(),
                turnNumber,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                defaultPlayerIds(activePlayerId),
                false,
                false,
                false,
                Map.of(),
                Map.of(),
                turnNumber,
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Integer currentTurnNumber,
            UUID playerWhoWentFirstId,
            Map<UUID, Integer> pokemonEnteredPlayTurnMap,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            Set<UUID> processedClientActionIds,
            Map<UUID, String> cardZones,
            Map<UUID, UUID> cardOwners,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        return state(
                gameId,
                status,
                currentPhase,
                turnNumber,
                stateVersion,
                activePlayerId,
                playerIds,
                energyAttachedThisTurn,
                supporterPlayedThisTurn,
                retreatedThisTurn,
                benchCountByPlayer,
                activePokemonConditionsByPlayer,
                currentTurnNumber,
                playerWhoWentFirstId,
                pokemonEnteredPlayTurnMap,
                cardsInHandByPlayer,
                affordableAttacksByPlayer,
                processedClientActionIds,
                cardZones,
                cardOwners,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                availableActions,
                updatedAt);
    }

    public static GameStateDto state(
            UUID gameId,
            GameStatus status,
            TurnPhase currentPhase,
            int turnNumber,
            int stateVersion,
            UUID activePlayerId,
            List<UUID> playerIds,
            boolean energyAttachedThisTurn,
            boolean supporterPlayedThisTurn,
            boolean retreatedThisTurn,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Integer currentTurnNumber,
            UUID playerWhoWentFirstId,
            Map<UUID, Integer> pokemonEnteredPlayTurnMap,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            Set<UUID> processedClientActionIds,
            Map<UUID, String> cardZones,
            Map<UUID, UUID> cardOwners,
            Map<UUID, Integer> mulliganCountByPlayer,
            Map<UUID, Boolean> setupSelectionSubmittedByPlayer,
            Map<UUID, UUID> setupActiveCardInstanceIdByPlayer,
            Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer,
            Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer,
            List<GameActionType> availableActions,
            Instant updatedAt) {
        int resolvedTurnNumber = turnNumber;
        if (currentTurnNumber != null) {
            resolvedTurnNumber = currentTurnNumber.intValue();
        }

        return GameStateDto.builder()
                .gameId(gameId)
                .status(status)
                .stateVersion(stateVersion)
                .playerIds(resolvePlayerIds(activePlayerId, playerIds, benchCountByPlayer, cardsInHandByPlayer))
                .players(players(
                        activePlayerId,
                        playerIds,
                        benchCountByPlayer,
                        activePokemonConditionsByPlayer,
                        cardsInHandByPlayer,
                        cardsInHandInstanceIdsByPlayer,
                        affordableAttacksByPlayer,
                        mulliganCountByPlayer,
                        setupSelectionSubmittedByPlayer,
                        setupActiveCardInstanceIdByPlayer,
                        setupBenchCardInstanceIdsByPlayer))
                .turn(TurnContextDto.builder()
                        .currentPhase(currentPhase)
                        .turnNumber(resolvedTurnNumber)
                        .activePlayerId(activePlayerId)
                        .playerWhoWentFirstId(playerWhoWentFirstId)
                        .energyAttachedThisTurn(energyAttachedThisTurn)
                        .supporterPlayedThisTurn(supporterPlayedThisTurn)
                        .retreatedThisTurn(retreatedThisTurn)
                        .build())
                .board(BoardStateDto.builder()
                        .enteredPlayTurnByPokemonInPlayId(pokemonEnteredPlayTurnMap)
                        .zoneByCardReferenceId(cardZones(cardZones))
                        .ownerByCardReferenceId(cardOwners)
                        .build())
                .actions(ActionStateDto.builder()
                        .availableActions(availableActions)
                        .processedClientActionIds(processedClientActionIds)
                        .build())
                .updatedAt(updatedAt)
                .build();
    }

    private static Map<UUID, PlayerStateDto> players(
            UUID activePlayerId,
            List<UUID> playerIds,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<SpecialConditionType>> activePokemonConditionsByPlayer,
            Map<UUID, List<UUID>> cardsInHandByPlayer,
            Map<UUID, List<UUID>> cardsInHandInstanceIdsByPlayer,
            Map<UUID, Set<UUID>> affordableAttacksByPlayer,
            Map<UUID, Integer> mulliganCountByPlayer,
            Map<UUID, Boolean> setupSelectionSubmittedByPlayer,
            Map<UUID, UUID> setupActiveCardInstanceIdByPlayer,
            Map<UUID, List<UUID>> setupBenchCardInstanceIdsByPlayer) {
        List<UUID> resolvedPlayerIds = resolvePlayerIds(activePlayerId, playerIds, benchCountByPlayer, cardsInHandByPlayer);
        Map<UUID, PlayerStateDto> players = new HashMap<>();
        for (UUID playerId : resolvedPlayerIds) {
            int benchPokemonCount = 0;
            if (benchCountByPlayer != null && benchCountByPlayer.get(playerId) != null) {
                benchPokemonCount = benchCountByPlayer.get(playerId).intValue();
            }

            int mulliganCount = 0;
            if (mulliganCountByPlayer != null && mulliganCountByPlayer.get(playerId) != null) {
                mulliganCount = mulliganCountByPlayer.get(playerId).intValue();
            }

            boolean selectionSubmitted = false;
            if (setupSelectionSubmittedByPlayer != null && setupSelectionSubmittedByPlayer.get(playerId) != null) {
                selectionSubmitted = setupSelectionSubmittedByPlayer.get(playerId).booleanValue();
            }

            List<SpecialConditionType> activeConditions = conditions(activePokemonConditionsByPlayer, playerId);
            List<UUID> handCardIds = uuidList(cardsInHandByPlayer, playerId);
            List<UUID> handInstanceIds = uuidList(cardsInHandInstanceIdsByPlayer, playerId);
            Set<UUID> affordableAttackIds = uuidSet(affordableAttacksByPlayer, playerId);
            UUID initialActiveCardInstanceId = null;
            if (setupActiveCardInstanceIdByPlayer != null) {
                initialActiveCardInstanceId = setupActiveCardInstanceIdByPlayer.get(playerId);
            }
            List<UUID> initialBenchCardInstanceIds = uuidList(setupBenchCardInstanceIdsByPlayer, playerId);

            PlayerStateDto playerState = PlayerStateDto.builder()
                    .benchPokemonCount(benchPokemonCount)
                    .activePokemonConditions(activeConditions)
                    .cardIdsInHand(handCardIds)
                    .cardInstanceIdsInHand(handInstanceIds)
                    .affordableAttackIds(affordableAttackIds)
                    .mulliganCount(mulliganCount)
                    .initialPokemonSelectionSubmitted(selectionSubmitted)
                    .initialActiveCardInstanceId(initialActiveCardInstanceId)
                    .initialBenchCardInstanceIds(initialBenchCardInstanceIds)
                    .build();
            players.put(playerId, playerState);
        }

        return players;
    }

    private static List<UUID> resolvePlayerIds(
            UUID activePlayerId,
            List<UUID> playerIds,
            Map<UUID, Integer> benchCountByPlayer,
            Map<UUID, List<UUID>> cardsInHandByPlayer) {
        List<UUID> resolvedPlayerIds = new ArrayList<>();
        if (playerIds != null) {
            for (UUID playerId : playerIds) {
                addIfMissing(resolvedPlayerIds, playerId);
            }
        }
        addIfMissing(resolvedPlayerIds, activePlayerId);
        addMapKeys(resolvedPlayerIds, benchCountByPlayer);
        addMapKeys(resolvedPlayerIds, cardsInHandByPlayer);

        return List.copyOf(resolvedPlayerIds);
    }

    private static void addMapKeys(List<UUID> playerIds, Map<UUID, ?> valuesByPlayer) {
        if (valuesByPlayer == null) {
            return;
        }

        for (UUID playerId : valuesByPlayer.keySet()) {
            addIfMissing(playerIds, playerId);
        }
    }

    private static void addIfMissing(List<UUID> playerIds, UUID playerId) {
        if (playerId == null) {
            return;
        }

        if (!playerIds.contains(playerId)) {
            playerIds.add(playerId);
        }
    }

    private static List<UUID> defaultPlayerIds(UUID activePlayerId) {
        if (activePlayerId == null) {
            return List.of();
        }

        return List.of(activePlayerId);
    }

    private static List<SpecialConditionType> conditions(
            Map<UUID, List<SpecialConditionType>> valuesByPlayer,
            UUID playerId) {
        if (valuesByPlayer == null) {
            return List.of();
        }

        List<SpecialConditionType> values = valuesByPlayer.get(playerId);
        if (values == null) {
            return List.of();
        }

        return List.copyOf(values);
    }

    private static List<UUID> uuidList(Map<UUID, List<UUID>> valuesByPlayer, UUID playerId) {
        if (valuesByPlayer == null) {
            return List.of();
        }

        List<UUID> values = valuesByPlayer.get(playerId);
        if (values == null) {
            return List.of();
        }

        return List.copyOf(values);
    }

    private static Set<UUID> uuidSet(Map<UUID, Set<UUID>> valuesByPlayer, UUID playerId) {
        if (valuesByPlayer == null) {
            return Set.of();
        }

        Set<UUID> values = valuesByPlayer.get(playerId);
        if (values == null) {
            return Set.of();
        }

        return Set.copyOf(values);
    }

    private static Map<UUID, CardZone> cardZones(Map<UUID, String> source) {
        if (source == null) {
            return Map.of();
        }

        Map<UUID, CardZone> zones = new HashMap<>();
        for (Map.Entry<UUID, String> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                CardZone zone = CardZone.valueOf(entry.getValue());
                zones.put(entry.getKey(), zone);
            }
        }

        return Map.copyOf(zones);
    }
}
