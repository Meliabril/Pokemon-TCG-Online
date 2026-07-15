package ar.edu.utn.frc.tup.piii.dtos.card;

import java.util.List;
import java.util.UUID;

public record AttackDto(
        UUID id,
        String name,
        String damageText,
        Integer baseDamage,
        String effectText,
        int attackOrder,
        List<AttackCostDto> costs,
        String displayName,
        List<String> displayCost,
        String displayText,
        String displayTextEn) {
}