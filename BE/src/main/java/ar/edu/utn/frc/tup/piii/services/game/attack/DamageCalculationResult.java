package ar.edu.utn.frc.tup.piii.services.game.attack;

public record DamageCalculationResult(
        int baseDamage,
        int afterAttackerModifiers,
        int afterWeakness,
        int afterResistance,
        int finalDamage,
        int damageCounters) {
}
