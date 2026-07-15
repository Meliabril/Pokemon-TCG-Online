package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackLockService;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsConditionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageProtectionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.MentalPanicService;
import ar.edu.utn.frc.tup.piii.services.game.attack.OutgoingDamageReductionService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatLockService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.SupporterLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BetweenTurnsResolutionServiceImpl implements BetweenTurnsResolutionService {

    private final GameParticipantStateService gameParticipantStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final SpecialConditionStateService specialConditionStateService;
    private final BetweenTurnsConditionService betweenTurnsConditionService;
    private final CombatResolutionService combatResolutionService;
    private final DamageProtectionService damageProtectionService;
    private final AttackLockService attackLockService;
    private final MentalPanicService mentalPanicService;
    private final OutgoingDamageReductionService outgoingDamageReductionService;
    private final SupporterLockService supporterLockService;
    private final RetreatLockService retreatLockService;

    @Override
    public BetweenTurnsResolutionResult resolveBetweenTurns(
            UUID gameId,
            UUID endingPlayerId,
            UUID nextActivePlayerId,
            int currentTurnNumber,
            int nextTurnNumber,
            int stateVersion) {
        List<GameEventDto> events = new ArrayList<>();
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);

        for (GameParticipant participant : participants) {
            supporterLockService.expireLock(participant, currentTurnNumber);

            PokemonInPlay activePokemon = pokemonInPlayStateService
                    .findActivePokemon(gameId, participant.getUserId())
                    .orElse(null);
            if (activePokemon == null) {
                continue;
            }

            damageProtectionService.expireProtection(activePokemon, currentTurnNumber);
            attackLockService.expireLock(activePokemon, currentTurnNumber);
            mentalPanicService.expireLock(activePokemon, currentTurnNumber);
            outgoingDamageReductionService.expireReduction(activePokemon, currentTurnNumber);
            retreatLockService.expireLock(activePokemon, currentTurnNumber);

            CombatResolutionService.CombatResolutionResult poisonResult = resolveCondition(
                    gameId,
                    participant.getUserId(),
                    endingPlayerId,
                    activePokemon,
                    nextActivePlayerId,
                    nextTurnNumber,
                    currentTurnNumber,
                    stateVersion,
                    SpecialConditionType.POISONED,
                    events);
            if (poisonResult.gameFinished() || poisonResult.promotionPending()) {
                return result(gameId, participants, events, poisonResult);
            }

            CombatResolutionService.CombatResolutionResult burnResult = resolveCondition(
                    gameId,
                    participant.getUserId(),
                    endingPlayerId,
                    activePokemon,
                    nextActivePlayerId,
                    nextTurnNumber,
                    currentTurnNumber,
                    stateVersion,
                    SpecialConditionType.BURNED,
                    events);
            if (burnResult.gameFinished() || burnResult.promotionPending()) {
                return result(gameId, participants, events, burnResult);
            }

            resolveCondition(
                    gameId,
                    participant.getUserId(),
                    endingPlayerId,
                    activePokemon,
                    nextActivePlayerId,
                    nextTurnNumber,
                    currentTurnNumber,
                    stateVersion,
                    SpecialConditionType.ASLEEP,
                    events);
            resolveCondition(
                    gameId,
                    participant.getUserId(),
                    endingPlayerId,
                    activePokemon,
                    nextActivePlayerId,
                    nextTurnNumber,
                    currentTurnNumber,
                    stateVersion,
                    SpecialConditionType.PARALYZED,
                    events);
        }

        return new BetweenTurnsResolutionResult(
                List.copyOf(events),
                activeConditionsByPlayer(gameId, participants),
                benchCountsByPlayer(gameId, participants),
                false,
                false,
                null);
    }

    @Override
    public Map<UUID, List<SpecialConditionType>> snapshotActiveConditions(UUID gameId, List<UUID> playerIds) {
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);
        Map<UUID, List<SpecialConditionType>> conditionsByPlayer = activeConditionsByPlayer(gameId, participants);
        Map<UUID, List<SpecialConditionType>> orderedConditions = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            List<SpecialConditionType> conditions = conditionsByPlayer.get(playerId);
            if (conditions == null) {
                orderedConditions.put(playerId, List.of());
            } else {
                orderedConditions.put(playerId, conditions);
            }
        }
        return Map.copyOf(orderedConditions);
    }

    @Override
    public Map<UUID, Integer> snapshotBenchCounts(UUID gameId, List<UUID> playerIds) {
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);
        Map<UUID, Integer> benchCounts = benchCountsByPlayer(gameId, participants);
        Map<UUID, Integer> orderedBenchCounts = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            Integer benchCount = benchCounts.get(playerId);
            if (benchCount == null) {
                orderedBenchCounts.put(playerId, 0);
            } else {
                orderedBenchCounts.put(playerId, benchCount);
            }
        }
        return Map.copyOf(orderedBenchCounts);
    }

    private CombatResolutionService.CombatResolutionResult resolveCondition(
            UUID gameId,
            UUID damagedPlayerId,
            UUID rewardPlayerId,
            PokemonInPlay activePokemon,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int currentTurnNumber,
            int stateVersion,
            SpecialConditionType conditionType,
            List<GameEventDto> events) {
        BetweenTurnsConditionService.BetweenTurnsConditionResult conditionResult = betweenTurnsConditionService.resolveConditions(
                gameId,
                activePokemon,
                damagedPlayerId,
                rewardPlayerId,
                conditionType,
                currentTurnNumber,
                stateVersion);
        events.addAll(conditionResult.events());
        if (!conditionResult.damageApplied()) {
            return CombatResolutionService.CombatResolutionResult.noKnockout();
        }

        return combatResolutionService.resolveKnockoutIfNeeded(
                gameId,
                damagedPlayerId,
                rewardPlayerId,
                activePokemon,
                nextActivePlayerId,
                nextTurnNumber,
                stateVersion,
                conditionResult.knockoutReason());
    }

    private BetweenTurnsResolutionResult result(
            UUID gameId,
            List<GameParticipant> participants,
            List<GameEventDto> events,
            CombatResolutionService.CombatResolutionResult combatResult) {
        events.addAll(combatResult.events());
        return new BetweenTurnsResolutionResult(
                List.copyOf(events),
                activeConditionsByPlayer(gameId, participants),
                benchCountsByPlayer(gameId, participants),
                combatResult.gameFinished(),
                combatResult.promotionPending(),
                combatResult.winnerUserId());
    }

    private Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer(UUID gameId, List<GameParticipant> participants) {
        return specialConditionStateService.activeConditionsByPlayer(
                gameId,
                playerIds(participants),
                pokemonInPlayStateService);
    }

    private Map<UUID, Integer> benchCountsByPlayer(UUID gameId, List<GameParticipant> participants) {
        return pokemonInPlayStateService.benchCountByPlayer(gameId, playerIds(participants));
    }

    private List<UUID> playerIds(List<GameParticipant> participants) {
        List<UUID> playerIds = new ArrayList<>();
        for (GameParticipant participant : participants) {
            playerIds.add(participant.getUserId());
        }
        return List.copyOf(playerIds);
    }
}
