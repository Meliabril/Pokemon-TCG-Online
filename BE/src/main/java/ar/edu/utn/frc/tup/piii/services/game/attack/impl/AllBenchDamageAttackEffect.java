package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AllBenchDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "ALL_BENCH_DAMAGE";
    private static final String OWN_BENCH_TARGET = "OWN_BENCH";
    private static final String OPPONENT_BENCH_TARGET = "OPPONENT_BENCH";

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final DamageApplicationService damageApplicationService;
    private final GameEventFactory gameEventFactory;
    private final CombatResolutionService combatResolutionService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID benchOwnerUserId = ownerUserId(context);
        PokemonInPlay primaryTarget = context.targetPokemon();
        List<GameEventDto> events = new ArrayList<>();
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                benchOwnerUserId)) {
            if (pokemon.getSlotPosition() == null || pokemon.getSlotPosition() <= 0) {
                continue;
            }
            int damageCounters = damageApplicationService.applyDamage(pokemon, context.operation().amount());
            events.add(gameEventFactory.publicEvent(
                    context.resolutionContext().gameId(),
                    GameEventType.DAMAGE_APPLIED,
                    context.stateVersion(),
                    Map.of(
                            "defenderPokemonInPlayId", pokemon.getId().toString(),
                            "damage", context.operation().amount(),
                            "damageCounters", damageCounters,
                            "reason", EFFECT_TYPE)));

            if (isPrimaryTarget(primaryTarget, pokemon)) {
                // Almost always false (the primary target is the active Pokemon, slot 0, while
                // this loop only ever visits slot > 0) - but some attacks allow a Benched Pokemon
                // to be selected as the primary target itself, so guard against double-processing
                // the same knockout that AttackServiceImpl already owns for the primary target.
                continue;
            }

            // Every other bench Pokemon hit here needs its own knockout check - unlike the single
            // -active flow, nothing downstream re-evaluates these. Without this, a bench Pokemon
            // reaching 0 HP would stay on the bench, never go to the discard pile, and never award
            // a prize.
            CombatResolutionService.CombatResolutionResult knockoutResult = combatResolutionService.resolveKnockoutIfNeeded(
                    context.resolutionContext().gameId(),
                    benchOwnerUserId,
                    rewardPlayerIdFor(context, benchOwnerUserId),
                    pokemon,
                    context.resolutionContext().defenderUserId(),
                    context.nextTurnNumber(),
                    context.stateVersion(),
                    EFFECT_TYPE);
            events.addAll(knockoutResult.events());
            if (knockoutResult.gameFinished() || knockoutResult.promotionPending()) {
                return AttackEffectResult.knockoutOutcome(
                        events,
                        knockoutResult.gameFinished(),
                        knockoutResult.promotionPending(),
                        knockoutResult.winnerUserId());
            }
        }
        return new AttackEffectResult(0, false, events);
    }

    private boolean isPrimaryTarget(PokemonInPlay primaryTarget, PokemonInPlay pokemon) {
        return primaryTarget != null
                && primaryTarget.getId() != null
                && primaryTarget.getId().equals(pokemon.getId());
    }

    private UUID ownerUserId(AttackEffectContext context) {
        if (OWN_BENCH_TARGET.equals(context.operation().target())) {
            return context.resolutionContext().attackerUserId();
        }
        if (OPPONENT_BENCH_TARGET.equals(context.operation().target())) {
            return context.resolutionContext().defenderUserId();
        }
        return context.resolutionContext().defenderUserId();
    }

    private UUID rewardPlayerIdFor(AttackEffectContext context, UUID benchOwnerUserId) {
        // OPPONENT_BENCH (e.g. a future "hit all of the opponent's bench" attack) rewards the
        // attacker, same as any normal knockout. OWN_BENCH (e.g. Earthquake) can knock out the
        // attacker's own bench Pokemon as a side effect of their own attack - in that case the
        // prize goes the other way, to the opponent.
        if (benchOwnerUserId.equals(context.resolutionContext().attackerUserId())) {
            return context.resolutionContext().defenderUserId();
        }
        return context.resolutionContext().attackerUserId();
    }
}
