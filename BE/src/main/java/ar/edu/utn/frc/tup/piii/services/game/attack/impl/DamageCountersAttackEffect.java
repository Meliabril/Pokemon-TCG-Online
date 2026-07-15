package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.effect.impl.DamageCounterEffectServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DamageCountersAttackEffect implements AttackEffect {

    private static final String DAMAGE_COUNTERS = "DAMAGE_COUNTERS";
    private static final String DAMAGE_COUNTERS_UNTIL_REMAINING_HP = "DAMAGE_COUNTERS_UNTIL_REMAINING_HP";
    private static final String ALL_OPPONENT_TARGET = "ALL_OPPONENT";
    private static final String BOTH_ACTIVE_TARGET = "BOTH_ACTIVE";
    private static final int DAMAGE_PER_COUNTER = 10;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final DamageCounterEffectService damageCounterEffectService;
    private final CardService cardService;

    public DamageCountersAttackEffect(
            PokemonInPlayStateService pokemonInPlayStateService,
            DamageApplicationService damageApplicationService,
            CardService cardService,
            GameEventFactory gameEventFactory,
            CombatResolutionService combatResolutionService) {
        this(pokemonInPlayStateService, new DamageCounterEffectServiceImpl(damageApplicationService, combatResolutionService, gameEventFactory), cardService);
    }

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null
                && (DAMAGE_COUNTERS.equals(operation.type())
                || DAMAGE_COUNTERS_UNTIL_REMAINING_HP.equals(operation.type()));
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        List<GameEventDto> events = new ArrayList<>();
        PokemonInPlay primaryTarget = context.targetPokemon();
        for (PokemonInPlay pokemon : targets(context)) {
            int damage = damageFor(context, pokemon);
            if (damage <= 0) {
                continue;
            }
            boolean primaryTargetHit = isPrimaryTarget(primaryTarget, pokemon);
            DamageCounterEffectService.DamageCounterResult result = damageCounterEffectService.placeDamageCounters(
                    context.resolutionContext().gameId(),
                    pokemon,
                    damage / DAMAGE_PER_COUNTER,
                    rewardPlayerIdFor(context, pokemon),
                    context.resolutionContext().defenderUserId(),
                    context.nextTurnNumber(),
                    context.stateVersion(),
                    context.operation().type(),
                    !primaryTargetHit);
            events.addAll(result.events());

            if (primaryTargetHit) {
                // The attack's primary target has its knockout resolved by AttackServiceImpl
                // right after this effect returns (it already owns that target's prize/promotion
                // handling) - resolving it here too would double-process the same knockout.
                continue;
            }
            if (result.gameFinished() || result.promotionPending()) {
                return AttackEffectResult.knockoutOutcome(
                        events,
                        result.gameFinished(),
                        result.promotionPending(),
                        result.winnerUserId());
            }
        }
        return new AttackEffectResult(0, false, events);
    }

    private boolean isPrimaryTarget(PokemonInPlay primaryTarget, PokemonInPlay pokemon) {
        return primaryTarget != null
                && primaryTarget.getId() != null
                && primaryTarget.getId().equals(pokemon.getId());
    }

    private UUID rewardPlayerIdFor(AttackEffectContext context, PokemonInPlay pokemon) {
        // Most damage-counter effects only ever touch the opponent's Pokemon (ALL_OPPONENT), in
        // which case the attacker is always the one rewarded. BOTH_ACTIVE effects (e.g. Spirit
        // Scream) can also knock out the attacker's own active Pokemon - in that case the prize
        // must go the other way, to whichever player does not own the knocked-out Pokemon.
        if (pokemon.getOwnerUserId().equals(context.resolutionContext().attackerUserId())) {
            return context.resolutionContext().defenderUserId();
        }
        return context.resolutionContext().attackerUserId();
    }

    private List<PokemonInPlay> targets(AttackEffectContext context) {
        if (ALL_OPPONENT_TARGET.equals(context.operation().target())) {
            return pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                    context.resolutionContext().gameId(),
                    context.resolutionContext().defenderUserId());
        }
        if (BOTH_ACTIVE_TARGET.equals(context.operation().target())) {
            return List.of(
                    context.resolutionContext().attackerPokemon(),
                    context.resolutionContext().defenderPokemon());
        }
        return List.of(context.targetPokemon());
    }

    private int damageFor(AttackEffectContext context, PokemonInPlay pokemon) {
        if (DAMAGE_COUNTERS_UNTIL_REMAINING_HP.equals(context.operation().type())) {
            Card card = cardService.getCardEntityById(pokemon.getActiveCardInstance().getCardId());
            int currentDamage = pokemon.getDamageCounters() == null ? 0 : pokemon.getDamageCounters() * DAMAGE_PER_COUNTER;
            int remainingHp = card.getHp() - currentDamage;
            return Math.max(0, remainingHp - context.operation().amount());
        }
        return context.operation().amount() * DAMAGE_PER_COUNTER;
    }
}
