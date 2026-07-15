package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;

import java.util.List;
import java.util.UUID;

public record VisiblePokemonDto(
        UUID pokemonInPlayId,
        UUID ownerPlayerId,
        Integer slotPosition,
        VisibleCardDto activeCard,
        List<VisibleCardDto> evolutionStack,
        List<VisibleCardDto> attachedEnergyCards,
        List<VisibleCardDto> attachedTrainerCards,
        Integer damageCounters,
        List<SpecialConditionType> specialConditions,
        List<VisibleAttackDto> attacks,
        List<VisibleAbilityDto> abilities,
        boolean canReceiveEnergy,
        boolean canReceiveTrainer,
        boolean canRetreatTo,
        boolean canPromote,
        List<VisiblePokemonEffectDto> visualEffects) {

    public VisiblePokemonDto {
        evolutionStack = evolutionStack == null ? List.of() : List.copyOf(evolutionStack);
        attachedEnergyCards = attachedEnergyCards == null ? List.of() : List.copyOf(attachedEnergyCards);
        attachedTrainerCards = attachedTrainerCards == null ? List.of() : List.copyOf(attachedTrainerCards);
        specialConditions = specialConditions == null ? List.of() : List.copyOf(specialConditions);
        attacks = attacks == null ? List.of() : List.copyOf(attacks);
        abilities = abilities == null ? List.of() : List.copyOf(abilities);
        visualEffects = visualEffects == null ? List.of() : List.copyOf(visualEffects);
    }
}
