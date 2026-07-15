package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityActivationType;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityTiming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbilityDefinition(
        AbilityCode code,
        String name,
        String displayName,
        String text,
        String displayText,
        AbilityActivationType activation,
        AbilityTiming timing,
        boolean oncePerTurn) {
}
