package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.services.game.attack.BetweenTurnsConditionService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BetweenTurnsConditionServiceImpl implements BetweenTurnsConditionService {

    private static final int POISON_DAMAGE = 10;
    private static final int BURN_DAMAGE = 20;

    private final SpecialConditionStateService specialConditionStateService;
    private final DamageApplicationService damageApplicationService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public BetweenTurnsConditionResult resolveConditions(
            UUID gameId,
            PokemonInPlay activePokemon,
            UUID activePokemonOwnerId,
            UUID endingPlayerId,
            SpecialConditionType conditionType,
            int currentTurnNumber,
            int stateVersion) {
        if (conditionType == null || activePokemon == null) {
            return noEffect();
        }

        List<SpecialCondition> conditions = specialConditionStateService.findByPokemonInPlayId(activePokemon.getId());
        switch (conditionType) {
            case POISONED:
                return resolvePoison(gameId, activePokemon, stateVersion, conditions);
            case BURNED:
                return resolveBurn(gameId, activePokemon, stateVersion, conditions);
            case ASLEEP:
                return resolveAsleep(gameId, activePokemon, stateVersion, conditions);
            case PARALYZED:
                return resolveParalyzed(
                        gameId,
                        activePokemon,
                        activePokemonOwnerId,
                        endingPlayerId,
                        currentTurnNumber,
                        stateVersion,
                        conditions);
            default:
                return noEffect();
        }
    }

    private BetweenTurnsConditionResult resolvePoison(
            UUID gameId,
            PokemonInPlay activePokemon,
            int stateVersion,
            List<SpecialCondition> conditions) {
        if (!hasCondition(conditions, SpecialConditionType.POISONED)) {
            return noEffect();
        }

        List<GameEventDto> events = new ArrayList<>();
        applyBetweenTurnsDamage(gameId, activePokemon, POISON_DAMAGE, stateVersion, "POISONED", events);
        return new BetweenTurnsConditionResult(
                List.copyOf(events),
                true,
                "BETWEEN_TURNS_POISONED");
    }

    private BetweenTurnsConditionResult resolveBurn(
            UUID gameId,
            PokemonInPlay activePokemon,
            int stateVersion,
            List<SpecialCondition> conditions) {
        if (!hasCondition(conditions, SpecialConditionType.BURNED)) {
            return noEffect();
        }

        List<GameEventDto> events = new ArrayList<>();
        boolean heads = gameRandomService.flipCoin();
        events.add(statusCheckEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.BURNED, coinResult(heads)));
        if (heads) {
            return new BetweenTurnsConditionResult(List.copyOf(events), false, null);
        }

        applyBetweenTurnsDamage(gameId, activePokemon, BURN_DAMAGE, stateVersion, "BURNED", events);
        return new BetweenTurnsConditionResult(
                List.copyOf(events),
                true,
                "BETWEEN_TURNS_BURNED");
    }

    private BetweenTurnsConditionResult resolveAsleep(
            UUID gameId,
            PokemonInPlay activePokemon,
            int stateVersion,
            List<SpecialCondition> conditions) {
        SpecialCondition asleepCondition = condition(conditions, SpecialConditionType.ASLEEP);
        if (asleepCondition == null) {
            return noEffect();
        }

        List<GameEventDto> events = new ArrayList<>();
        boolean heads = gameRandomService.flipCoin();
        events.add(statusCheckEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.ASLEEP, coinResult(heads)));
        if (heads) {
            specialConditionStateService.delete(asleepCondition);
            events.add(statusResolvedEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.ASLEEP));
        }

        return new BetweenTurnsConditionResult(List.copyOf(events), false, null);
    }

    private BetweenTurnsConditionResult resolveParalyzed(
            UUID gameId,
            PokemonInPlay activePokemon,
            UUID activePokemonOwnerId,
            UUID endingPlayerId,
            int currentTurnNumber,
            int stateVersion,
            List<SpecialCondition> conditions) {
        SpecialCondition paralyzedCondition = condition(conditions, SpecialConditionType.PARALYZED);
        if (paralyzedCondition == null) {
            return noEffect();
        }
        if (!activePokemonOwnerId.equals(endingPlayerId)) {
            return noEffect();
        }
        if (paralyzedCondition.getAppliedTurn() >= currentTurnNumber) {
            return noEffect();
        }

        specialConditionStateService.delete(paralyzedCondition);
        return new BetweenTurnsConditionResult(
                List.of(statusResolvedEvent(gameId, stateVersion, activePokemon.getId(), SpecialConditionType.PARALYZED)),
                false,
                null);
    }

    private void applyBetweenTurnsDamage(
            UUID gameId,
            PokemonInPlay activePokemon,
            int damage,
            int stateVersion,
            String reason,
            List<GameEventDto> events) {
        int damageCounters = damageApplicationService.applyDamage(activePokemon, damage);
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", activePokemon.getId().toString(),
                        "damage", damage,
                        "damageCounters", damageCounters,
                        "reason", reason)));
    }

    private boolean hasCondition(List<SpecialCondition> conditions, SpecialConditionType conditionType) {
        return condition(conditions, conditionType) != null;
    }

    private SpecialCondition condition(List<SpecialCondition> conditions, SpecialConditionType conditionType) {
        for (SpecialCondition condition : conditions) {
            if (conditionType.equals(condition.getConditionType())) {
                return condition;
            }
        }

        return null;
    }

    private GameEventDto statusCheckEvent(
            UUID gameId,
            int stateVersion,
            UUID pokemonInPlayId,
            SpecialConditionType conditionType,
            String result) {
        return gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                stateVersion,
                Map.of(
                        "effectType", "BETWEEN_TURNS_STATUS_CHECK",
                        "pokemonInPlayId", pokemonInPlayId.toString(),
                        "conditionType", conditionType.name(),
                        "result", result));
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

    private String coinResult(boolean heads) {
        if (heads) {
            return "HEADS";
        }

        return "TAILS";
    }

    private BetweenTurnsConditionResult noEffect() {
        return new BetweenTurnsConditionResult(List.<GameEventDto>of(), false, null);
    }
}
