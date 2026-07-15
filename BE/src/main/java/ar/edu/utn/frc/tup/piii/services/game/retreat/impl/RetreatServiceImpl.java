package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.ActionStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.PlayerStateDto;
import ar.edu.utn.frc.tup.piii.dtos.game.TurnContextDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.AvailableActionsFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatCostPaymentService;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatService;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetreatServiceImpl implements RetreatService {

    private static final String TARGET_POKEMON_IN_PLAY_ID_KEY = "targetPokemonInPlayId";

    private final GameActionPayloadReader payloadReader;
    private final AvailableActionsFactory availableActionsFactory;
    private final RetreatCostPaymentService retreatCostPaymentService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final CardService cardService;
    private final SpecialConditionStateService specialConditionStateService;
    private final GameEventFactory gameEventFactory;
    private final StadiumModifierService stadiumModifierService;

    @Override
    public GameActionExecutionResult retreat(GameActionContext context) {
        UUID actorUserId = context.actorUserId();
        UUID gameId = context.gameId();
        UUID targetPokemonInPlayId = payloadReader.requiredUuid(context.request().payload(), TARGET_POKEMON_IN_PLAY_ID_KEY);

        PokemonInPlay activePokemon = findActivePokemon(gameId, actorUserId);
        PokemonInPlay benchPokemon = findTargetBenchPokemon(targetPokemonInPlayId, gameId, actorUserId);
        if (benchPokemon.getSlotPosition() == null || benchPokemon.getSlotPosition() <= 0) {
            throw new InvalidGameActionException("Target Pokemon must be on the bench to retreat");
        }

        Card activeCard = cardService.getCardEntityById(activePokemon.getActiveCardInstance().getCardId());
        int retreatCost = 0;
        if (stadiumModifierService.isRetreatFree(gameId, activePokemon)) {
            retreatCost = 0;
        } else if (activeCard.getRetreatCost() != null) {
            retreatCost = activeCard.getRetreatCost();
        }
        int discardedEnergyCount = retreatCostPaymentService.payRetreatCost(gameId, actorUserId, activePokemon, retreatCost);

        int formerBenchSlot = benchPokemon.getSlotPosition();
        GameCardInstance activeCardInstance = activePokemon.getActiveCardInstance();
        GameCardInstance benchCardInstance = benchPokemon.getActiveCardInstance();
        gameCardInstanceStateService.swapActiveWithBench(
                activeCardInstance.getId(),
                benchCardInstance.getId(),
                formerBenchSlot);
        pokemonInPlayStateService.swapActiveWithBench(
                activePokemon.getId(),
                benchPokemon.getId(),
                formerBenchSlot);

        specialConditionStateService.deleteByPokemonInPlayId(activePokemon.getId());
        gameCardInstanceStateService.resequenceZone(gameId, actorUserId, CardZone.ATTACHED);

        int newStateVersion = context.currentState().stateVersion() + 1;
        TurnContextDto newTurn = context.currentState().turn().toBuilder()
                .retreatedThisTurn(true)
                .build();
        ActionStateDto newActions = context.currentState().actions().toBuilder()
                .availableActions(availableActionsFactory.mainPhaseActionsAfterRetreat(
                        context.currentState().turn().energyAttachedThisTurn()))
                .build();
        GameStateDto newState = context.currentState().toBuilder()
                .stateVersion(newStateVersion)
                .turn(newTurn)
                .players(recalculatePlayers(gameId, actorUserId, context.currentState().players()))
                .actions(newActions)
                .updatedAt(Instant.now())
                .build();

        List<GameEventDto> events = List.of(gameEventFactory.publicEvent(
                gameId,
                GameEventType.RETREAT_DONE,
                newStateVersion,
                Map.of(
                        "playerId", actorUserId.toString(),
                        "newActivePokemonInPlayId", benchPokemon.getId().toString(),
                        "benchedPokemonInPlayId", activePokemon.getId().toString(),
                        "discardedEnergyCount", discardedEnergyCount)));

        return new GameActionExecutionResult(newState, events);
    }

    private PokemonInPlay findActivePokemon(UUID gameId, UUID actorUserId) {
        Optional<PokemonInPlay> activePokemon = pokemonInPlayStateService.findActivePokemon(gameId, actorUserId);
        if (activePokemon.isEmpty()) {
            throw new InvalidGameActionException("Active Pokemon was not found for retreat");
        }

        return activePokemon.get();
    }

    private PokemonInPlay findTargetBenchPokemon(UUID targetPokemonInPlayId, UUID gameId, UUID actorUserId) {
        Optional<PokemonInPlay> benchPokemon = pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(
                targetPokemonInPlayId,
                gameId,
                actorUserId);
        if (benchPokemon.isEmpty()) {
            throw new InvalidGameActionException("Target bench Pokemon was not found");
        }

        return benchPokemon.get();
    }

    private Map<UUID, PlayerStateDto> recalculatePlayers(
            UUID gameId,
            UUID actorUserId,
            Map<UUID, PlayerStateDto> previousPlayers) {
        Map<UUID, PlayerStateDto> players = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerStateDto> entry : previousPlayers.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerStateDto playerState = entry.getValue();
            if (playerState == null) {
                playerState = PlayerStateDto.builder().build();
            }

            PlayerStateDto.Builder builder = playerState.toBuilder()
                    .benchPokemonCount(pokemonInPlayStateService.countBenchPokemon(gameId, playerId));
            if (playerId.equals(actorUserId)) {
                builder.activePokemonConditions(List.of());
            }
            players.put(playerId, builder.build());
        }
        return Map.copyOf(players);
    }
}
