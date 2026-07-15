package ar.edu.utn.frc.tup.piii.dtos.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TypeValueTranslationDto(
        String type,
        String value) {
}
