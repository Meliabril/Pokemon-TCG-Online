package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.card.CardAbilityDto;
import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;

import java.util.List;
import java.util.Optional;

public interface AbilityCatalogService {

    List<AbilityDefinition> abilitiesFor(String cardExternalId);

    Optional<AbilityDefinition> find(String cardExternalId, AbilityCode abilityCode);

    default List<CardAbilityDto> cardAbilitiesFor(String cardExternalId) {
        return abilitiesFor(cardExternalId).stream()
                .map(ability -> new CardAbilityDto(
                        ability.code().name(),
                        ability.code(),
                        ability.name(),
                        "Ability",
                        ability.displayName(),
                        "Habilidad",
                        ability.text(),
                        ability.displayText(),
                        ability.activation(),
                        ability.timing(),
                        ability.oncePerTurn(),
                        true))
                .toList();
    }
}
