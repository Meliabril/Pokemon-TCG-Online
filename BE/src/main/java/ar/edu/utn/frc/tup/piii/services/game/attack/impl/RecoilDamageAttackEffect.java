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
public class RecoilDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "RECOIL_DAMAGE";

    private final DamageApplicationService damageApplicationService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        int damageCounters = damageApplicationService.applyDamage(attackerPokemon, context.operation().amount());
        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.DAMAGE_APPLIED,
                context.stateVersion(),
                Map.of(
                        "defenderPokemonInPlayId", attackerPokemon.getId().toString(),
                        "damage", context.operation().amount(),
                        "damageCounters", damageCounters,
                        "reason", EFFECT_TYPE));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
