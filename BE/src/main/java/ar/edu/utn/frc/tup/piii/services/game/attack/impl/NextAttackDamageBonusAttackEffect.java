package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NextAttackDamageBonusAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "NEXT_ATTACK_DAMAGE_BONUS";
    private static final int OWN_NEXT_TURN_OFFSET = 2;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        if (AttackEffectPhase.BEFORE_DAMAGE.equals(context.operation().phase())) {
            return consumeBonus(context);
        }
        return scheduleBonus(context);
    }

    private AttackEffectResult consumeBonus(AttackEffectContext context) {
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        Integer bonusTurn = attackerPokemon.getNextAttackBonusTurn();
        Integer bonusOrder = attackerPokemon.getNextAttackBonusOrder();
        Integer bonusAmount = attackerPokemon.getNextAttackBonusAmount();
        if (bonusTurn == null || bonusOrder == null || bonusAmount == null) {
            return AttackEffectResult.empty();
        }
        if (bonusTurn != context.turnNumber()
                || bonusOrder != context.resolutionContext().selectedAttack().getAttackOrder()) {
            return AttackEffectResult.empty();
        }

        attackerPokemon.setNextAttackBonusTurn(null);
        attackerPokemon.setNextAttackBonusOrder(null);
        attackerPokemon.setNextAttackBonusAmount(null);
        pokemonInPlayStateService.save(attackerPokemon);
        return new AttackEffectResult(bonusAmount, false, List.of(effectEvent(context, "consumed", bonusAmount)));
    }

    private AttackEffectResult scheduleBonus(AttackEffectContext context) {
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        int bonusTurn = context.turnNumber() + OWN_NEXT_TURN_OFFSET;
        attackerPokemon.setNextAttackBonusTurn(bonusTurn);
        attackerPokemon.setNextAttackBonusOrder(context.resolutionContext().selectedAttack().getAttackOrder());
        attackerPokemon.setNextAttackBonusAmount(context.operation().amount());
        pokemonInPlayStateService.save(attackerPokemon);
        return new AttackEffectResult(0, false, List.of(effectEvent(context, "scheduled", context.operation().amount())));
    }

    private GameEventDto effectEvent(AttackEffectContext context, String status, int amount) {
        return gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString(),
                        "attackOrder", context.resolutionContext().selectedAttack().getAttackOrder(),
                        "status", status,
                        "amount", amount));
    }
}
