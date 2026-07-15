package ar.edu.utn.frc.tup.piii.services.game.state.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardPlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardPokemonDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.BoardZoneSummaryDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardActionHintsDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleAbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleCardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePlayerBoardDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisiblePokemonDto;
import ar.edu.utn.frc.tup.piii.dtos.game.VisibleZoneDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class GameStateVisibilitySanitizer {

    public GameStateDto sanitizeForViewer(GameStateDto state, UUID viewerUserId) {
        if (state == null || viewerUserId == null) {
            return state;
        }

        Map<UUID, PlayerStateDto> visiblePlayers = new HashMap<>();
        for (Map.Entry<UUID, PlayerStateDto> entry : state.players().entrySet()) {
            UUID playerId = entry.getKey();
            PlayerStateDto playerState = entry.getValue();
            PlayerStateDto visiblePlayerState;
            if (playerId.equals(viewerUserId)) {
                visiblePlayerState = playerState;
            } else {
                visiblePlayerState = playerState.toBuilder()
                        .cardIdsInHand(List.of())
                        .cardInstanceIdsInHand(List.of())
                        .initialActiveCardInstanceId(null)
                        .initialBenchCardInstanceIds(List.of())
                        .build();
            }
            visiblePlayers.put(playerId, visiblePlayerState);
        }

        Map<UUID, CardZone> visibleZones = new HashMap<>();
        Map<UUID, UUID> visibleOwners = new HashMap<>();
        for (Map.Entry<UUID, CardZone> entry : state.board().zoneByCardReferenceId().entrySet()) {
            UUID referenceId = entry.getKey();
            CardZone zone = entry.getValue();
            UUID ownerUserId = state.board().ownerByCardReferenceId().get(referenceId);
            if (CardZone.HAND.equals(zone) && ownerUserId != null && !ownerUserId.equals(viewerUserId)) {
                continue;
            }
            if (CardZone.DECK.equals(zone) || CardZone.PRIZE.equals(zone)) {
                continue;
            }

            visibleZones.put(referenceId, zone);
            if (ownerUserId != null) {
                visibleOwners.put(referenceId, ownerUserId);
            }
        }

        BoardStateDto visibleBoard = state.board().toBuilder()
                .zoneByCardReferenceId(visibleZones)
                .ownerByCardReferenceId(visibleOwners)
                .view(sanitizeVisibleBoard(state, viewerUserId))
                .build();

        return state.toBuilder()
                .players(visiblePlayers)
                .boardPlayers(visibleBoardPlayers(state, viewerUserId))
                .board(visibleBoard)
                .resolution(visibleResolution(state.resolution(), viewerUserId))
                .build();
    }

    private ResolutionStateDto visibleResolution(ResolutionStateDto resolutionState, UUID viewerUserId) {
        if (resolutionState == null || !resolutionState.hasPendingAttackChoice()) {
            return resolutionState;
        }
        if (viewerUserId.equals(resolutionState.pendingChoicePlayerId())) {
            return resolutionState;
        }
        return resolutionState.toBuilder()
                .pendingChoicePayload(Map.of())
                .build();
    }

    private VisibleBoardDto sanitizeVisibleBoard(GameStateDto state, UUID viewerUserId) {
        VisibleBoardDto sourceView = state.board().view();
        if (sourceView == null) {
            return VisibleBoardDto.empty();
        }

        boolean hideOpponentSetup = GameStatus.SETUP.equals(state.status());
        List<VisiblePlayerBoardDto> sanitizedPlayers = new ArrayList<>();
        for (VisiblePlayerBoardDto playerBoard : sourceView.players()) {
            boolean localPlayer = playerBoard.playerId().equals(viewerUserId);
            VisibleZoneDto visibleHand = playerBoard.hand();
            VisiblePokemonDto activePokemon = playerBoard.activePokemon();
            List<VisiblePokemonDto> benchPokemon = playerBoard.benchPokemon();
            boolean setupSelectionSubmitted = playerBoard.setupSelectionSubmitted();

            VisibleZoneDto visibleDeck = playerBoard.deck();
            if (!localPlayer) {
                visibleHand = new VisibleZoneDto(playerBoard.hand().zone(), playerBoard.hand().count(), List.of());
                visibleDeck = new VisibleZoneDto(playerBoard.deck().zone(), playerBoard.deck().count(), List.of());
                activePokemon = sanitizePokemonAbilityDeckOptions(activePokemon);
                benchPokemon = sanitizePokemonAbilityDeckOptions(benchPokemon);
                if (hideOpponentSetup) {
                    activePokemon = null;
                    benchPokemon = List.of();
                }
            }

            sanitizedPlayers.add(new VisiblePlayerBoardDto(
                    playerBoard.playerId(),
                    playerBoard.playerOrder(),
                    playerBoard.connected(),
                    localPlayer,
                    playerBoard.mulliganCount(),
                    playerBoard.mulliganNoticePending(),
                    playerBoard.mulliganFlowActive(),
                    playerBoard.mulliganReadyForInitialSelection(),
                    playerBoard.mulliganRoundNumber(),
                    setupSelectionSubmitted,
                    playerBoard.setupActiveOccupied(),
                    playerBoard.setupBenchOccupiedCount(),
                    visibleHand,
                    visibleDeck,
                    playerBoard.prize(),
                    playerBoard.discard(),
                    activePokemon,
                    benchPokemon));
        }

        return new VisibleBoardDto(
                viewerUserId,
                List.copyOf(sanitizedPlayers),
                sourceView.stadium(),
                buildActionHintsForViewer(sanitizedPlayers, viewerUserId));
    }

    private VisiblePokemonDto sanitizePokemonAbilityDeckOptions(VisiblePokemonDto pokemon) {
        if (pokemon == null) {
            return null;
        }
        return new VisiblePokemonDto(
                pokemon.pokemonInPlayId(),
                pokemon.ownerPlayerId(),
                pokemon.slotPosition(),
                pokemon.activeCard(),
                pokemon.evolutionStack(),
                pokemon.attachedEnergyCards(),
                pokemon.attachedTrainerCards(),
                pokemon.damageCounters(),
                pokemon.specialConditions(),
                pokemon.attacks(),
                pokemon.abilities().stream()
                        .map(ability -> new VisibleAbilityDto(
                                ability.abilityId(),
                                ability.name(),
                                ability.activationType(),
                                ability.enabled(),
                                ability.disabledReason(),
                                List.of()))
                        .toList(),
                pokemon.canReceiveEnergy(),
                pokemon.canReceiveTrainer(),
                pokemon.canRetreatTo(),
                pokemon.canPromote(),
                pokemon.visualEffects());
    }

    private List<VisiblePokemonDto> sanitizePokemonAbilityDeckOptions(List<VisiblePokemonDto> pokemon) {
        return pokemon.stream()
                .map(this::sanitizePokemonAbilityDeckOptions)
                .toList();
    }

    private VisibleBoardActionHintsDto buildActionHintsForViewer(
            List<VisiblePlayerBoardDto> playerBoards,
            UUID viewerUserId) {
        VisiblePlayerBoardDto localPlayerBoard = null;
        for (VisiblePlayerBoardDto playerBoard : playerBoards) {
            if (playerBoard.playerId().equals(viewerUserId)) {
                localPlayerBoard = playerBoard;
                break;
            }
        }
        if (localPlayerBoard == null) {
            return VisibleBoardActionHintsDto.empty();
        }

        List<UUID> attachEnergyTargets = new ArrayList<>();
        List<UUID> retreatTargets = new ArrayList<>();
        List<UUID> promoteTargets = new ArrayList<>();

        collectPokemonHints(localPlayerBoard.activePokemon(), attachEnergyTargets, retreatTargets, promoteTargets);
        for (VisiblePokemonDto benchPokemon : localPlayerBoard.benchPokemon()) {
            collectPokemonHints(benchPokemon, attachEnergyTargets, retreatTargets, promoteTargets);
        }

        Set<UUID> trainerTargets = new HashSet<>();
        Set<UUID> trainerToolTargets = new HashSet<>();
        for (VisibleCardDto handCard : localPlayerBoard.hand().cards()) {
            if (handCard == null || handCard.validTargetPokemonInPlayIds().isEmpty()) {
                continue;
            }
            if (CardCategory.POKEMON_TOOL_TRAINER.equals(handCard.category())) {
                trainerToolTargets.addAll(handCard.validTargetPokemonInPlayIds());
            } else {
                trainerTargets.addAll(handCard.validTargetPokemonInPlayIds());
            }
        }

        return new VisibleBoardActionHintsDto(
                List.copyOf(attachEnergyTargets),
                List.copyOf(retreatTargets),
                List.copyOf(promoteTargets),
                List.copyOf(trainerTargets),
                List.copyOf(trainerToolTargets));
    }

    private void collectPokemonHints(
            VisiblePokemonDto pokemon,
            List<UUID> attachEnergyTargets,
            List<UUID> retreatTargets,
            List<UUID> promoteTargets) {
        if (pokemon == null || pokemon.pokemonInPlayId() == null) {
            return;
        }
        if (pokemon.canReceiveEnergy()) {
            attachEnergyTargets.add(pokemon.pokemonInPlayId());
        }
        if (pokemon.canRetreatTo()) {
            retreatTargets.add(pokemon.pokemonInPlayId());
        }
        if (pokemon.canPromote()) {
            promoteTargets.add(pokemon.pokemonInPlayId());
        }
    }

    private Map<UUID, BoardPlayerStateDto> visibleBoardPlayers(GameStateDto state, UUID viewerUserId) {
        Map<UUID, BoardPlayerStateDto> visibleBoardPlayers = new HashMap<>();
        for (Map.Entry<UUID, BoardPlayerStateDto> entry : state.boardPlayers().entrySet()) {
            UUID playerId = entry.getKey();
            BoardPlayerStateDto playerState = entry.getValue();
            if (playerState == null) {
                visibleBoardPlayers.put(playerId, BoardPlayerStateDto.empty(playerId));
                continue;
            }

            visibleBoardPlayers.put(playerId, visibleBoardPlayer(state, viewerUserId, playerId, playerState));
        }

        return Map.copyOf(visibleBoardPlayers);
    }

    private BoardPlayerStateDto visibleBoardPlayer(
            GameStateDto state,
            UUID viewerUserId,
            UUID playerId,
            BoardPlayerStateDto playerState) {
        boolean ownPlayer = playerId.equals(viewerUserId);
        BoardZoneSummaryDto visibleHand = playerState.hand();
        if (!ownPlayer) {
            visibleHand = BoardZoneSummaryDto.hidden(CardZone.HAND, playerState.hand().count());
        }

        BoardPokemonDto activePokemon = playerState.activePokemon();
        List<BoardPokemonDto> benchPokemon = playerState.benchPokemon();
        if (!ownPlayer && GameStatus.SETUP.equals(state.status())) {
            activePokemon = hiddenPokemon(activePokemon, CardZone.ACTIVE);
            benchPokemon = hiddenPokemonList(benchPokemon, CardZone.BENCH);
        }

        return new BoardPlayerStateDto(
                playerId,
                visibleHand,
                BoardZoneSummaryDto.hidden(CardZone.DECK, playerState.deck().count()),
                BoardZoneSummaryDto.hidden(CardZone.PRIZE, playerState.prizes().count()),
                playerState.discard(),
                playerState.stadium(),
                activePokemon,
                benchPokemon);
    }

    private BoardPokemonDto hiddenPokemon(BoardPokemonDto pokemon, CardZone zone) {
        if (pokemon == null) {
            return null;
        }

        return new BoardPokemonDto(
                null,
                null,
                pokemon.slotPosition(),
                null,
                null,
                BoardCardDto.hidden(zone),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private List<BoardPokemonDto> hiddenPokemonList(List<BoardPokemonDto> pokemon, CardZone zone) {
        List<BoardPokemonDto> hiddenPokemon = new ArrayList<>();
        for (BoardPokemonDto item : pokemon) {
            hiddenPokemon.add(hiddenPokemon(item, zone));
        }

        return List.copyOf(hiddenPokemon);
    }
}
