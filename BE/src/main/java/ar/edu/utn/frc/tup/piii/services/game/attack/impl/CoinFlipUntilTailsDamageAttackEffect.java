package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
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
public class CoinFlipUntilTailsDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "COIN_FLIP_UNTIL_TAILS_DAMAGE";
    private static final int MAX_FLIPS = 100;

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        List<String> coinResults = new ArrayList<>();
        int headsCount = 0;
        boolean heads = gameRandomService.flipCoin();
        while (heads && coinResults.size() < MAX_FLIPS) {
            coinResults.add("HEADS");
            headsCount++;
            heads = gameRandomService.flipCoin();
        }
        coinResults.add("TAILS");

        int damageModifier = headsCount * context.operation().amount();

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "coinResults", List.copyOf(coinResults),
                        "headsCount", headsCount,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }
}
