package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.ResolutionStateDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PromotionService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameStateQueryService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final String POKEMON_IN_PLAY_ID_KEY = "pokemonInPlayId";

    private final GameActionPayloadReader payloadReader;
    private final GameLookupService gameLookupService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final SpecialConditionStateService specialConditionStateService;
    private final GameStateQueryService gameStateQueryService;
    private final GameEventFactory gameEventFactory;
    private final AbilityUsageTracker abilityUsageTracker;

    @Override
    public GameActionExecutionResult promoteBenchPokemon(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID pokemonInPlayId = payloadReader.requiredUuid(context.request().payload(), POKEMON_IN_PLAY_ID_KEY);
        Game game = gameLookupService.getRequiredGame(context.gameId());
        ResolutionStateDto resolutionState = context.currentState().resolution();

        if (!resolutionState.hasPendingPromotion() || !actorUserId.equals(resolutionState.playerToPromoteId())) {
            throw new InvalidGameActionException("No pending promotion is available for the acting player");
        }

        if (pokemonInPlayStateService.findActivePokemon(context.gameId(), actorUserId).isPresent()) {
            throw new InvalidGameActionException("Player already has an active Pokemon");
        }

        if (pokemonInPlayStateService.countBenchPokemon(context.gameId(), actorUserId) == 0) {
            throw new InvalidGameActionException("Player has no Pokemon on the Bench");
        }

        Optional<PokemonInPlay> selectedPokemonResult = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                pokemonInPlayId,
                context.gameId(),
                actorUserId);
        if (selectedPokemonResult.isEmpty()) {
            throw new InvalidGameActionException("Promotion target was not found for the acting player");
        }

        PokemonInPlay selectedPokemon = selectedPokemonResult.get();
        if (selectedPokemon.getSlotPosition() == null || selectedPokemon.getSlotPosition() <= 0) {
            throw new InvalidGameActionException("Promotion target must be on the Bench");
        }

        promoteSelectedPokemon(context.gameId(), actorUserId, selectedPokemon);
        game.setActivePlayerId(resolutionState.nextActivePlayerId());
        game.setTurnNumber(resolutionState.nextTurnNumber());
        game.setCurrentPhase(TurnPhase.DRAW);
        abilityUsageTracker.clearTurnUsage(game);
        game.setResolutionState(Map.of());

        int newStateVersion = context.currentState().stateVersion() + 1;
        GameStateDto state = gameStateQueryService.buildVisibleState(game).toBuilder()
                .stateVersion(newStateVersion)
                .updatedAt(Instant.now())
                .build();

        List<GameEventDto> events = List.of(
                gameEventFactory.publicEvent(
                        context.gameId(),
                        GameEventType.POKEMON_PROMOTED,
                        newStateVersion,
                        Map.of("playerId", actorUserId.toString(), "pokemonInPlayId", pokemonInPlayId.toString())),
                gameEventFactory.publicEvent(
                        context.gameId(),
                        GameEventType.PHASE_CHANGED,
                        newStateVersion,
                        Map.of(
                                "phase", TurnPhase.DRAW.name(),
                                "activePlayerId", resolutionState.nextActivePlayerId().toString())),
                gameEventFactory.publicEvent(
                        context.gameId(),
                        GameEventType.TURN_STARTED,
                        newStateVersion,
                        Map.of(
                                "playerId", resolutionState.nextActivePlayerId().toString(),
                                "turnNumber", resolutionState.nextTurnNumber())));

        return new GameActionExecutionResult(state, events);
    }

    private void promoteSelectedPokemon(UUID gameId, UUID ownerUserId, PokemonInPlay selectedPokemon) {
        selectedPokemon.setSlotPosition(0);
        GameCardInstance activeCard = selectedPokemon.getActiveCardInstance();
        activeCard.setZone(CardZone.ACTIVE);
        activeCard.setZonePosition(0);
        activeCard.setFaceDown(false);
        gameCardInstanceStateService.save(activeCard);
        pokemonInPlayStateService.save(selectedPokemon);
        specialConditionStateService.deleteByPokemonInPlayId(selectedPokemon.getId());
        resequenceBench(gameId, ownerUserId, selectedPokemon.getId());
    }

    private void resequenceBench(UUID gameId, UUID ownerUserId, UUID promotedPokemonId) {
        List<PokemonInPlay> benchPokemon = new ArrayList<>();
        List<PokemonInPlay> allPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, ownerUserId);
        for (PokemonInPlay pokemonInPlay : allPokemon) {
            if (pokemonInPlay.getId().equals(promotedPokemonId)) {
                continue;
            }
            Integer slotPosition = pokemonInPlay.getSlotPosition();
            if (slotPosition != null && slotPosition > 0) {
                benchPokemon.add(pokemonInPlay);
            }
        }

        sortBySlotPosition(benchPokemon);
        int nextSlot = 1;
        for (PokemonInPlay benchPokemonSlot : benchPokemon) {
            benchPokemonSlot.setSlotPosition(nextSlot);
            GameCardInstance benchCard = benchPokemonSlot.getActiveCardInstance();
            benchCard.setZone(CardZone.BENCH);
            benchCard.setZonePosition(nextSlot);
            gameCardInstanceStateService.save(benchCard);
            pokemonInPlayStateService.save(benchPokemonSlot);
            nextSlot++;
        }
    }

    private void sortBySlotPosition(List<PokemonInPlay> benchPokemon) {
        for (int leftIndex = 0; leftIndex < benchPokemon.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < benchPokemon.size(); rightIndex++) {
                PokemonInPlay leftPokemon = benchPokemon.get(leftIndex);
                PokemonInPlay rightPokemon = benchPokemon.get(rightIndex);
                if (leftPokemon.getSlotPosition() > rightPokemon.getSlotPosition()) {
                    benchPokemon.set(leftIndex, rightPokemon);
                    benchPokemon.set(rightIndex, leftPokemon);
                }
            }
        }
    }
}
