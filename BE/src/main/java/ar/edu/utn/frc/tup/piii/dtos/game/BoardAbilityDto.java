package ar.edu.utn.frc.tup.piii.dtos.game;

public record BoardAbilityDto(
        String abilityId,
        String name,
        String activationType,
        boolean available,
        String disabledReason) {
}
