package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.List;

public record VisibleAbilityDto(
        String abilityId,
        String name,
        String activationType,
        boolean enabled,
        String disabledReason,
        List<VisibleCardDto> deckCardOptions) {
    public VisibleAbilityDto {
        deckCardOptions = deckCardOptions == null ? List.of() : List.copyOf(deckCardOptions);
    }
}
