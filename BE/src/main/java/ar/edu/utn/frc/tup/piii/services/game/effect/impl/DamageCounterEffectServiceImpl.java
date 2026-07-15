package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DamageCounterEffectServiceImpl implements DamageCounterEffectService {

    private static final int DAMAGE_PER_COUNTER = 10;

    private final DamageApplicationService damageApplicationService;
    private final CombatResolutionService combatResolutionService;
    private final GameEventFactory gameEventFactory;

    @Override
    public DamageCounterResult placeDamageCounters(
            UUID gameId,
            PokemonInPlay targetPokemon,
            int counters,
            UUID rewardPlayerId,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason) {
        return placeDamageCounters(
                gameId,
                targetPokemon,
                counters,
                rewardPlayerId,
                nextActivePlayerId,
                nextTurnNumber,
                stateVersion,
                reason,
                true);
    }

    @Override
    public DamageCounterResult placeDamageCounters(
            UUID gameId,
            PokemonInPlay targetPokemon,
            int counters,
            UUID rewardPlayerId,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            String reason,
            boolean resolveKnockout) {
        if (targetPokemon == null || counters <= 0) {
            return new DamageCounterResult(0, 0, false, false, null, List.of());
        }

        int damage = counters * DAMAGE_PER_COUNTER;
        int resultingCounters = damageApplicationService.applyDamage(targetPokemon, damage);
        List<GameEventDto> events = new ArrayList<>();
        events.add(gameEventFactory.publicEvent(
                gameId,
                GameEventType.DAMAGE_APPLIED,
                stateVersion,
                Map.of(
                        "defenderPokemonInPlayId", targetPokemon.getId().toString(),
                        "damage", damage,
                        "damageCounters", resultingCounters,
                        "reason", reason == null ? "DAMAGE_COUNTERS" : reason)));

        CombatResolutionService.CombatResolutionResult combatResult = CombatResolutionService.CombatResolutionResult.noKnockout();
        if (resolveKnockout) {
            combatResult = combatResolutionService.resolveKnockoutIfNeeded(
                    gameId,
                    targetPokemon.getOwnerUserId(),
                    rewardPlayerId,
                    targetPokemon,
                    nextActivePlayerId,
                    nextTurnNumber,
                    stateVersion,
                    reason);
            events.addAll(combatResult.events());
        }
        return new DamageCounterResult(
                damage,
                resultingCounters,
                combatResult.gameFinished(),
                combatResult.promotionPending(),
                combatResult.winnerUserId(),
                List.copyOf(events));
    }
}
