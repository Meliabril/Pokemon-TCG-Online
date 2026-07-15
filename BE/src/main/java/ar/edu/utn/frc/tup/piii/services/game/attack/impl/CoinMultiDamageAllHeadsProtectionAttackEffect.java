package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoinMultiDamageAllHeadsProtectionAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "COIN_MULTI_DAMAGE_ALL_HEADS_PROTECTION";

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final PokemonInPlayStateService pokemonInPlayStateService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        int coinCount = context.operation().coinCount();
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
        boolean allHeads = coinCount > 0 && headsCount == coinCount;
        Integer protectedUntilTurn = null;
        if (allHeads) {
            PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
            protectedUntilTurn = context.turnNumber() + 1;
            attackerPokemon.setDamageProtectionTurn(protectedUntilTurn);
            pokemonInPlayStateService.save(attackerPokemon);
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "coinResults", List.copyOf(coinResults),
                        "headsCount", headsCount,
                        "allHeads", allHeads,
                        "damageModifier", damageModifier,
                        "protectedUntilTurn", protectedUntilTurn == null ? 0 : protectedUntilTurn,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private int importedBaseDamage(AttackEffectContext context) {
        Attack attack = context.resolutionContext().selectedAttack();
        if (attack == null || attack.getBaseDamage() == null) {
            return 0;
        }
        return attack.getBaseDamage();
    }
}
