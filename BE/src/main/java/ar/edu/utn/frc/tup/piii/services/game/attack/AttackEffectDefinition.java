package ar.edu.utn.frc.tup.piii.services.game.attack;

import java.util.List;

public record AttackEffectDefinition(
        String cardExternalId,
        Integer attackOrder,
        String attackName,
        boolean allowsBenchTarget,
        List<AttackEffectOperation> operations) {

    public AttackEffectDefinition {
        if (operations == null) {
            operations = List.of();
        } else {
            operations = List.copyOf(operations);
        }
    }

    public AttackEffectDefinition(
            String cardExternalId,
            String attackName,
            boolean allowsBenchTarget,
            List<AttackEffectOperation> operations) {
        this(cardExternalId, null, attackName, allowsBenchTarget, operations);
    }

    public static AttackEffectDefinition empty() {
        return new AttackEffectDefinition(null, null, null, false, List.of());
    }

    public boolean isEmpty() {
        return cardExternalId == null && attackOrder == null && attackName == null && operations.isEmpty() && !allowsBenchTarget;
    }
}
