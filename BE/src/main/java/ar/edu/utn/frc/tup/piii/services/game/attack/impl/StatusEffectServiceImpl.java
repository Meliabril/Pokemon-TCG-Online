package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameParticipant;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import ar.edu.utn.frc.tup.piii.services.game.attack.StatusEffectService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.VictoryConditionService;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusEffectServiceImpl implements StatusEffectService {

    private static final String STATUS_CONDITIONS_KEY = "statusConditions";
    private static final String STATUS_CONDITION_KEY = "statusCondition";
    private static final EnumSet<SpecialConditionType> EXCLUSIVE_CONDITIONS = EnumSet.of(
            SpecialConditionType.ASLEEP,
            SpecialConditionType.CONFUSED,
            SpecialConditionType.PARALYZED);

    private final SpecialConditionStateService specialConditionStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameParticipantStateService gameParticipantStateService;
    private final CardService cardService;
    private final KnockoutService knockoutService;
    private final PrizeService prizeService;
    private final VictoryConditionService victoryConditionService;
    private final GameEventFactory gameEventFactory;
    private final GameLookupService gameLookupService;
    private final GameRandomService gameRandomService;

    @Override
    public AppliedConditionsResult applyAttackConditions(
            UUID gameId,
            UUID sourcePlayerId,
            PokemonInPlay targetPokemon,
            Map<String, Object> payload,
            int currentTurnNumber,
            int stateVersion) {
        List<SpecialConditionType> requestedConditions = requestedConditions(payload);
        if (requestedConditions.isEmpty()) {
            return new AppliedConditionsResult(List.of(), Map.of());
        }

        List<GameEventDto> events = new ArrayList<>();
        Map<UUID, List<SpecialConditionType>> updatedConditionsByPlayer = new LinkedHashMap<>();

        for (SpecialConditionType conditionType : requestedConditions) {
            if (EXCLUSIVE_CONDITIONS.contains(conditionType)) {
                clearExclusiveConditions(targetPokemon.getId());
            } else {
                Optional<SpecialCondition> existing = specialConditionStateService.findByPokemonInPlayIdAndConditionType(targetPokemon.getId(), conditionType);
                if (existing.isPresent()) {
                    continue;
                }
            }

            SpecialCondition specialCondition = new SpecialCondition();
            specialCondition.setPokemonInPlay(targetPokemon);
            specialCondition.setConditionType(conditionType);
            specialCondition.setAppliedTurn(currentTurnNumber);
            specialConditionStateService.save(specialCondition);

            events.add(gameEventFactory.publicEvent(
                    gameId,
                    GameEventType.STATUS_APPLIED,
                    stateVersion,
                    Map.of(
                            "playerId", sourcePlayerId.toString(),
                            "pokemonInPlayId", targetPokemon.getId().toString(),
                            "conditionType", conditionType.name())));
        }

        updatedConditionsByPlayer.put(targetPokemon.getOwnerUserId(), activeConditionTypes(targetPokemon.getId()));
        return new AppliedConditionsResult(events, Map.copyOf(updatedConditionsByPlayer));
    }

    @Override
    public ConfusionResolution resolveConfusionBeforeAttack(
            UUID gameId,
            UUID attackingPlayerId,
            PokemonInPlay attackerPokemon,
            int currentTurnNumber,
            int stateVersion) {
        boolean confused = specialConditionStateService
                .findByPokemonInPlayIdAndConditionType(attackerPokemon.getId(), SpecialConditionType.CONFUSED)
                .isPresent();
        if (!confused) {
            return ConfusionResolution.canProceed();
        }

        boolean coinWasHeads = gameRandomService.flipCoin();
        if (coinWasHeads) {
            return ConfusionResolution.canProceed();
        }

        attackerPokemon.setDamageCounters(attackerPokemon.getDamageCounters() + 3);
        pokemonInPlayStateService.save(attackerPokemon);

        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", attackerPokemon.getId().toString(),
                        "damage", 30,
                        "damageCounters", attackerPokemon.getDamageCounters(),
                        "reason", "CONFUSION_SELF_DAMAGE")));

        UUID winnerUserId = null;
        if (isKnockedOut(attackerPokemon)) {
            UUID defendingPlayerId = opponentUserId(gameId, attackingPlayerId);
            winnerUserId = defendingPlayerId;
            events.add(gameEventFactory.publicEvent(
                    gameId,
                    GameEventType.POKEMON_KNOCKED_OUT,
                    stateVersion,
                    Map.of(
                            "playerId", attackingPlayerId.toString(),
                            "pokemonInPlayId", attackerPokemon.getId().toString())));
            KnockoutService.KnockoutResult knockoutResult = knockoutService.resolveKnockout(gameId, attackingPlayerId, attackerPokemon);
            PrizeService.PrizeResult prizeResult = prizeService.takeSinglePrize(gameId, defendingPlayerId);
            events.add(gameEventFactory.publicEvent(
                    gameId,
                    GameEventType.PRIZE_TAKEN,
                    stateVersion,
                    Map.of(
                            "playerId", defendingPlayerId.toString(),
                            "cardId", prizeResult.cardId().toString(),
                            "remainingPrizeCards", prizeResult.remainingPrizeCards())));
            boolean defenderWins = victoryConditionService.attackerWinsAfterKnockout(
                    prizeResult.remainingPrizeCards(),
                    knockoutResult.hasReplacementActivePokemon());
            if (defenderWins) {
                Game game = gameLookupService.getRequiredGame(gameId);
                game.setStatus(GameStatus.FINISHED);
                game.setCurrentPhase(null);
                game.setWinnerPlayerId(defendingPlayerId);
                game.setFinishedAt(Instant.now());
                events.add(gameEventFactory.publicEvent(
                        gameId,
                        GameEventType.GAME_FINISHED,
                        stateVersion,
                        Map.of("winnerPlayerId", defendingPlayerId.toString(), "reason", "CONFUSION_SELF_KNOCKOUT")));
            }
        }

        return new ConfusionResolution(false, winnerUserId, List.copyOf(events));
    }

    @Override
    public BetweenTurnsResolution processBetweenTurns(
            UUID gameId,
            UUID endingPlayerId,
            int currentTurnNumber,
            int stateVersion) {
        List<GameEventDto> events = new ArrayList<>();
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);
        UUID winnerUserId = null;

        for (GameParticipant participant : participants) {
            PokemonInPlay activePokemon = pokemonInPlayStateService
                    .findActivePokemon(gameId, participant.getUserId())
                    .orElse(null);
            if (activePokemon == null) {
                continue;
            }

            List<SpecialCondition> conditions = specialConditionStateService.findByPokemonInPlayId(activePokemon.getId());
            if (conditions.isEmpty()) {
                continue;
            }

            boolean burned = false;
            boolean poisoned = false;
            boolean paralysisCleared = false;
            boolean asleepCleared = false;

            for (SpecialCondition condition : new ArrayList<>(conditions)) {
                switch (condition.getConditionType()) {
                    case POISONED:
                        poisoned = true;
                        break;
                    case BURNED:
                        burned = true;
                        break;
                    case ASLEEP:
                        if (gameRandomService.flipCoin()) {
                            specialConditionStateService.delete(condition);
                            asleepCleared = true;
                        }
                        break;
                    case PARALYZED:
                        if (Objects.equals(participant.getUserId(), endingPlayerId)
                                && condition.getAppliedTurn() < currentTurnNumber) {
                            specialConditionStateService.delete(condition);
                            paralysisCleared = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            int betweenTurnsDamage = 0;
            if (poisoned) {
                betweenTurnsDamage += 10;
            }
            if (burned && !gameRandomService.flipCoin()) {
                betweenTurnsDamage += 20;
            }
            if (betweenTurnsDamage > 0) {
                activePokemon.setDamageCounters(activePokemon.getDamageCounters() + (betweenTurnsDamage / 10));
                pokemonInPlayStateService.save(activePokemon);
                events.add(gameEventFactory.publicEvent(
                        gameId,
                        GameEventType.DAMAGE_APPLIED,
                        stateVersion,
                        Map.of(
                                "defenderPokemonInPlayId", activePokemon.getId().toString(),
                                "damage", betweenTurnsDamage,
                                "damageCounters", activePokemon.getDamageCounters(),
                                "reason", "BETWEEN_TURNS")));
            }

            if (paralysisCleared) {
                events.add(statusResolvedEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.PARALYZED));
            }
            if (asleepCleared) {
                events.add(statusResolvedEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.ASLEEP));
            }

            if (isKnockedOut(activePokemon)) {
                UUID defeatedPlayerId = participant.getUserId();
                UUID winningPlayerId = opponentUserId(gameId, defeatedPlayerId);
                events.add(gameEventFactory.publicEvent(
                        gameId,
                        GameEventType.POKEMON_KNOCKED_OUT,
                        stateVersion,
                        Map.of(
                                "playerId", defeatedPlayerId.toString(),
                                "pokemonInPlayId", activePokemon.getId().toString(),
                                "reason", "BETWEEN_TURNS")));
                KnockoutService.KnockoutResult knockoutResult = knockoutService.resolveKnockout(gameId, defeatedPlayerId, activePokemon);
                PrizeService.PrizeResult prizeResult = prizeService.takeSinglePrize(gameId, winningPlayerId);
                events.add(gameEventFactory.publicEvent(
                        gameId,
                        GameEventType.PRIZE_TAKEN,
                        stateVersion,
                        Map.of(
                                "playerId", winningPlayerId.toString(),
                                "cardId", prizeResult.cardId().toString(),
                                "remainingPrizeCards", prizeResult.remainingPrizeCards())));
                boolean playerWins = victoryConditionService.attackerWinsAfterKnockout(
                        prizeResult.remainingPrizeCards(),
                        knockoutResult.hasReplacementActivePokemon());
                if (playerWins) {
                    Game game = gameLookupService.getRequiredGame(gameId);
                    game.setStatus(GameStatus.FINISHED);
                    game.setCurrentPhase(null);
                    game.setWinnerPlayerId(winningPlayerId);
                    game.setFinishedAt(Instant.now());
                    winnerUserId = winningPlayerId;
                    events.add(gameEventFactory.publicEvent(
                            gameId,
                            GameEventType.GAME_FINISHED,
                            stateVersion,
                            Map.of("winnerPlayerId", winningPlayerId.toString(), "reason", "BETWEEN_TURNS")));
                    break;
                }
            }
        }

        return new BetweenTurnsResolution(
                List.copyOf(events),
                activeConditionsByPlayer(gameId, participants),
                benchCountsByPlayer(gameId, participants),
                winnerUserId);
    }

    @Override
    public Map<UUID, List<SpecialConditionType>> snapshotActiveConditions(UUID gameId, List<UUID> playerIds) {
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);
        Map<UUID, List<SpecialConditionType>> conditionsByPlayer = activeConditionsByPlayer(gameId, participants);
        Map<UUID, List<SpecialConditionType>> orderedConditions = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            orderedConditions.put(playerId, conditionsByPlayer.getOrDefault(playerId, List.of()));
        }
        return Map.copyOf(orderedConditions);
    }

    @Override
    public Map<UUID, Integer> snapshotBenchCounts(UUID gameId, List<UUID> playerIds) {
        List<GameParticipant> participants = gameParticipantStateService.findOrderedByGameId(gameId);
        Map<UUID, Integer> benchCounts = benchCountsByPlayer(gameId, participants);
        Map<UUID, Integer> orderedBenchCounts = new LinkedHashMap<>();
        for (UUID playerId : playerIds) {
            orderedBenchCounts.put(playerId, benchCounts.getOrDefault(playerId, 0));
        }
        return Map.copyOf(orderedBenchCounts);
    }

    private List<SpecialConditionType> requestedConditions(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        Object multipleConditions = payload.get(STATUS_CONDITIONS_KEY);
        if (multipleConditions instanceof Iterable<?> iterable) {
            List<SpecialConditionType> conditions = new ArrayList<>();
            for (Object entry : iterable) {
                conditions.add(toConditionType(entry));
            }
            return List.copyOf(conditions);
        }

        Object singleCondition = payload.get(STATUS_CONDITION_KEY);
        if (singleCondition != null) {
            return List.of(toConditionType(singleCondition));
        }

        return List.of();
    }

    private void clearExclusiveConditions(UUID pokemonInPlayId) {
        specialConditionStateService.clearConditions(pokemonInPlayId, EXCLUSIVE_CONDITIONS);
    }

    private List<SpecialConditionType> activeConditionTypes(UUID pokemonInPlayId) {
        return specialConditionStateService.activeConditionTypes(pokemonInPlayId);
    }

    private GameEventDto statusResolvedEvent(UUID gameId, int stateVersion, UUID pokemonInPlayId, SpecialConditionType conditionType) {
        return gameEventFactory.publicEvent(
                gameId,
                GameEventType.STATUS_APPLIED,
                stateVersion,
                Map.of(
                        "pokemonInPlayId", pokemonInPlayId.toString(),
                        "conditionType", conditionType.name(),
                        "resolved", true));
    }

    private boolean isKnockedOut(PokemonInPlay pokemonInPlay) {
        Card topCard = cardService.getCardEntityById(pokemonInPlay.getActiveCardInstance().getCardId());
        return topCard.getHp() != null && pokemonInPlay.getDamageCounters() * 10 >= topCard.getHp();
    }

    private UUID opponentUserId(UUID gameId, UUID actorUserId) {
        return gameParticipantStateService.findOpponentUserId(gameId, actorUserId);
    }

    private Map<UUID, List<SpecialConditionType>> activeConditionsByPlayer(UUID gameId, List<GameParticipant> participants) {
        return specialConditionStateService.activeConditionsByPlayer(
                gameId,
                playerIds(participants),
                pokemonInPlayStateService);
    }

    private Map<UUID, Integer> benchCountsByPlayer(UUID gameId, List<GameParticipant> participants) {
        return pokemonInPlayStateService.benchCountByPlayer(
                gameId,
                playerIds(participants));
    }

    private List<UUID> playerIds(List<GameParticipant> participants) {
        List<UUID> playerIds = new ArrayList<>();
        for (GameParticipant participant : participants) {
            playerIds.add(participant.getUserId());
        }
        return List.copyOf(playerIds);
    }

    private SpecialConditionType toConditionType(Object rawValue) {
        if (rawValue instanceof SpecialConditionType conditionType) {
            return conditionType;
        }
        if (rawValue instanceof String stringValue) {
            return SpecialConditionType.valueOf(stringValue.toUpperCase());
        }
        throw new InvalidGameActionException("Unsupported special condition value: " + rawValue);
    }
}
