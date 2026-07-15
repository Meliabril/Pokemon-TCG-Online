package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
 * Models attacks like Skarmory-EX's Tailspin Piledriver: deals bonus damage
 * if the defending Pokemon already has damage counters on it before this
 * attack resolves.
 */
@Service
@RequiredArgsConstructor
public class ConditionalDefenderAlreadyDamagedAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "CONDITIONAL_DEFENDER_ALREADY_DAMAGED";

    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay defenderPokemon = context.targetPokemon();
        Integer damageCounters = defenderPokemon.getDamageCounters();
        boolean conditionMet = damageCounters != null && damageCounters > 0;
        int damageModifier = conditionMet ? context.operation().amount() : 0;

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "conditionMet", conditionMet,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", defenderPokemon.getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }
}
