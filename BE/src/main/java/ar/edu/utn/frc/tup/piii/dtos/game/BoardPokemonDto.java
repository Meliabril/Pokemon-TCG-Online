package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;

import java.util.List;
import java.util.UUID;

public record BoardPokemonDto(
        UUID pokemonInPlayId,
        UUID ownerUserId,
        Integer slotPosition,
        Integer damageCounters,
        Integer enteredPlayTurn,
        BoardCardDto activeCard,
        List<BoardCardDto> evolutionStack,
        List<BoardCardDto> attachedCards,
        List<SpecialConditionType> specialConditions,
        List<BoardAttackDto> attacks,
        List<BoardAbilityDto> abilities) {

    public BoardPokemonDto {
        if (evolutionStack == null) {
            evolutionStack = List.of();
        } else {
            evolutionStack = List.copyOf(evolutionStack);
        }
        if (attachedCards == null) {
            attachedCards = List.of();
        } else {
            attachedCards = List.copyOf(attachedCards);
        }
        if (specialConditions == null) {
            specialConditions = List.of();
        } else {
            specialConditions = List.copyOf(specialConditions);
        }
        if (attacks == null) {
            attacks = List.of();
        } else {
            attacks = List.copyOf(attacks);
        }
        if (abilities == null) {
            abilities = List.of();
        } else {
            abilities = List.copyOf(abilities);
        }
    }
}
