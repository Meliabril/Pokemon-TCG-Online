package ar.edu.utn.frc.tup.piii.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttackTranslationDto(
        int order,
        String displayName,
        List<String> displayCost,
        String displayText) {
}
