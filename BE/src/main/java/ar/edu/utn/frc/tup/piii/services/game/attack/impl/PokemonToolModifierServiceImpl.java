package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.PokemonToolModifierService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PokemonToolModifierServiceImpl implements PokemonToolModifierService {

    private static final String MUSCLE_BAND_ID = "xy1-121";
    private static final String HARD_CHARM_ID = "xy1-119";

    private static final int MUSCLE_BAND_BONUS = 20;
    private static final int HARD_CHARM_REDUCTION = -20;

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;

    @Override
    public int getOutgoingDamageBonus(PokemonInPlay attacker) {
        if (attacker == null) {
            return 0;
        }
        return attachedToolExternalIds(attacker).stream()
                .filter(MUSCLE_BAND_ID::equals)
                .findFirst()
                .map(id -> MUSCLE_BAND_BONUS)
                .orElse(0);
    }

    @Override
    public int getIncomingDamageModifier(PokemonInPlay defender) {
        if (defender == null) {
            return 0;
        }
        return attachedToolExternalIds(defender).stream()
                .filter(HARD_CHARM_ID::equals)
                .findFirst()
                .map(id -> HARD_CHARM_REDUCTION)
                .orElse(0);
    }

    private List<String> attachedToolExternalIds(PokemonInPlay pokemon) {
        return pokemonAttachedCardStateService
                .findByPokemonInPlayId(pokemon.getId())
                .stream()
                .filter(a -> AttachedCardType.POKEMON_TOOL.equals(a.getAttachedCardType()))
                .map(PokemonAttachedCard::getGameCardInstance)
                .map(gci -> cardService.getCardEntityById(gci.getCardId()))
                .map(Card::getExternalId)
                .toList();
    }
}
