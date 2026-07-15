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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RandomBenchDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "RANDOM_BENCH_DAMAGE";
    private static final int DEFAULT_TARGET_COUNT = 2;

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
        UUID defenderUserId = context.resolutionContext().defenderUserId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        int targetCount = context.operation().coinCount() > 0 ? context.operation().coinCount() : DEFAULT_TARGET_COUNT;
        int damageAmount = context.operation().amount();

        List<PokemonInPlay> allDefenderPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(), defenderUserId);

        List<PokemonInPlay> benchPokemon = allDefenderPokemon.stream()
                .filter(p -> p.getSlotPosition() != null && p.getSlotPosition() > 0)
                .collect(Collectors.toList());

        List<PokemonInPlay> targets = selectTargets(allDefenderPokemon, benchPokemon, targetCount);

        List<GameEventDto> events = new ArrayList<>();
        PokemonInPlay primaryTarget = context.targetPokemon();

        for (PokemonInPlay target : targets) {
            int damageCounters = damageApplicationService.applyDamage(target, damageAmount);
            events.add(gameEventFactory.publicEvent(
                    context.resolutionContext().gameId(),
                    GameEventType.DAMAGE_APPLIED,
                    context.stateVersion(),
                    Map.of(
                            "defenderPokemonInPlayId", target.getId().toString(),
                            "damage", damageAmount,
                            "damageCounters", damageCounters,
                            "reason", EFFECT_TYPE)));

            if (isPrimaryTarget(primaryTarget, target)) {
                continue;
            }

            CombatResolutionService.CombatResolutionResult knockoutResult = combatResolutionService.resolveKnockoutIfNeeded(
                    context.resolutionContext().gameId(),
                    defenderUserId,
                    attackerUserId,
                    target,
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

    private List<PokemonInPlay> selectTargets(
            List<PokemonInPlay> allDefenderPokemon,
            List<PokemonInPlay> benchPokemon,
            int targetCount) {
        if (benchPokemon.isEmpty()) {
            return allDefenderPokemon.stream()
                    .filter(p -> p.getSlotPosition() != null && p.getSlotPosition() == 0)
                    .limit(1)
                    .collect(Collectors.toList());
        }

        List<PokemonInPlay> shuffled = new ArrayList<>(benchPokemon);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(targetCount, shuffled.size()));
    }

    private boolean isPrimaryTarget(PokemonInPlay primaryTarget, PokemonInPlay pokemon) {
        return primaryTarget != null
                && primaryTarget.getId() != null
                && primaryTarget.getId().equals(pokemon.getId());
    }
}
