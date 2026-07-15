package ar.edu.utn.frc.tup.piii.services.game.attack;

public interface AttackEffect {

    boolean supports(AttackEffectOperation operation);

    AttackEffectResult apply(AttackEffectContext context);
}
