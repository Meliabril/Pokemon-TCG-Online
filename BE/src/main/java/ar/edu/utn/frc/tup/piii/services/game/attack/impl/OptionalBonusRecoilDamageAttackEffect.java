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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OptionalBonusRecoilDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "OPTIONAL_BONUS_RECOIL_DAMAGE";
    private static final String USE_BONUS_DAMAGE_PAYLOAD_KEY = "useBonusDamage";

    private final DamageApplicationService damageApplicationService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        if (!Boolean.TRUE.equals(context.payload().get(USE_BONUS_DAMAGE_PAYLOAD_KEY))) {
            return AttackEffectResult.empty();
        }

        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        int amount = context.operation().amount();
        int damageCounters = damageApplicationService.applyDamage(attackerPokemon, amount);
        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.DAMAGE_APPLIED,
                context.stateVersion(),
                Map.of(
                        "defenderPokemonInPlayId", attackerPokemon.getId().toString(),
                        "damage", amount,
                        "damageCounters", damageCounters,
                        "reason", EFFECT_TYPE));
        return new AttackEffectResult(amount, false, List.of(event));
    }
}
