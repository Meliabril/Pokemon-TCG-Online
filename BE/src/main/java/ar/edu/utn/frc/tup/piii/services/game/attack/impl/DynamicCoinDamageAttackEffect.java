package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DynamicCoinDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DYNAMIC_COIN_DAMAGE";
    private static final String ATTACKER_DAMAGE_COUNTERS = "ATTACKER_DAMAGE_COUNTERS";
    private static final String ATTACKER_ATTACHED_ENERGY_TYPE = "ATTACKER_ATTACHED_ENERGY_TYPE";

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final AttachedEnergyCounter attachedEnergyCounter;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int coinCount = resolveCoinCount(context);
        List<String> coinResults = new ArrayList<>();
        int headsCount = 0;
        for (int i = 0; i < coinCount; i++) {
            boolean heads = gameRandomService.flipCoin();
            coinResults.add(heads ? "HEADS" : "TAILS");
            if (heads) {
                headsCount++;
            }
        }

        int totalDamage = headsCount * context.operation().amount();
        int damageModifier = totalDamage - importedBaseDamage(context);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "coinCount", coinCount,
                        "coinResults", List.copyOf(coinResults),
                        "headsCount", headsCount,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private int resolveCoinCount(AttackEffectContext context) {
        String source = context.operation().dynamicCoinCountSource();
        if (ATTACKER_DAMAGE_COUNTERS.equals(source)) {
            return attackerDamageCounters(context);
        }
        if (ATTACKER_ATTACHED_ENERGY_TYPE.equals(source)) {
            return attackerAttachedEnergyCount(context);
        }

        throw new InvalidGameActionException("Unsupported dynamic coin count source: " + source);
    }

    private int attackerDamageCounters(AttackEffectContext context) {
        PokemonInPlay attacker = context.resolutionContext().attackerPokemon();
        if (attacker == null || attacker.getDamageCounters() == null) {
            return 0;
        }
        return attacker.getDamageCounters();
    }

    private int attackerAttachedEnergyCount(AttackEffectContext context) {
        PokemonInPlay attacker = context.resolutionContext().attackerPokemon();
        return attachedEnergyCounter.countByType(attacker, context.operation().energyType());
    }

    private int importedBaseDamage(AttackEffectContext context) {
        Attack attack = context.resolutionContext().selectedAttack();
        if (attack == null || attack.getBaseDamage() == null) {
            return 0;
        }
        return attack.getBaseDamage();
    }
}
