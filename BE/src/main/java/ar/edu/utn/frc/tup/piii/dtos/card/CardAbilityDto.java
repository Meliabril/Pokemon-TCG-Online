package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming;

public record CardAbilityDto(
        String id,
        AbilityCode code,
        String name,
        String type,
        String displayName,
        String displayType,
        String text,
        String displayText,
        AbilityActivationType activation,
        AbilityTiming timing,
        boolean oncePerTurn,
        boolean implemented) {
}
