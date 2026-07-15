package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * Models attacks like Inkay's Puncture: this attack's damage isn't affected
 * by Resistance, so the Resistance step is skipped during damage calculation.
 */
@Service
public class IgnoreResistanceAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "IGNORE_RESISTANCE";

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        return AttackEffectResult.ignoringResistance(List.of());
    }
}
