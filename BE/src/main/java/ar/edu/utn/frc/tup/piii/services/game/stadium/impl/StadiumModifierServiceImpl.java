package ar.edu.utn.frc.tup.piii.services.game.stadium.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StadiumModifierServiceImpl implements StadiumModifierService {

    private static final String FAIRY_GARDEN_ID = "xy1-117";
    private static final String SHADOW_CIRCLE_ID = "xy1-126";
    private static final String FAIRY_ENERGY_TYPE = "Fairy";
    private static final String DARKNESS_ENERGY_TYPE = "Darkness";
    // Fallback names: some imported Energy cards (e.g. real XY1 "Fairy Energy" / "Darkness Energy"
    // rows from the pokemontcg.io payload) come through with a null pokemonType because the source
    // API only reliably populates "types" for Pokemon cards. These are the stable English card
    // names (not translated UI text) used elsewhere in the codebase (see FairyTransferAbilityHandler,
    // PassiveAbilityServiceImpl) as the established fallback for this exact gap.
    private static final String FAIRY_ENERGY_NAME = "Fairy Energy";
    private static final String DARKNESS_ENERGY_NAME = "Darkness Energy";

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;

    @Override
    public boolean isRetreatFree(UUID gameId, PokemonInPlay activePokemon) {
        return activeStadiumIs(gameId, FAIRY_GARDEN_ID)
                && hasAttachedEnergyType(activePokemon, FAIRY_ENERGY_TYPE, FAIRY_ENERGY_NAME);
    }

    @Override
    public boolean isWeaknessNegated(UUID gameId, PokemonInPlay defenderPokemon) {
        return activeStadiumIs(gameId, SHADOW_CIRCLE_ID)
                && hasAttachedEnergyType(defenderPokemon, DARKNESS_ENERGY_TYPE, DARKNESS_ENERGY_NAME);
    }

    private boolean activeStadiumIs(UUID gameId, String externalId) {
        return findActiveStadiumCard(gameId)
                .map(card -> externalId.equals(card.getExternalId()))
                .orElse(false);
    }

    private Optional<Card> findActiveStadiumCard(UUID gameId) {
        return gameCardInstanceStateService.findByGameId(gameId)
                .stream()
                .filter(instance -> CardZone.STADIUM.equals(instance.getZone()))
                .map(instance -> cardService.getCardEntityById(instance.getCardId()))
                .filter(card -> card != null)
                .findFirst();
    }

    private boolean hasAttachedEnergyType(PokemonInPlay pokemon, String energyType, String energyCardName) {
        if (pokemon == null || pokemon.getId() == null) {
            return false;
        }
        for (PokemonAttachedCard attachedCard : pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())) {
            if (!isEnergy(attachedCard)) {
                continue;
            }
            Card card = cardService.getCardEntityById(attachedCard.getGameCardInstance().getCardId());
            if (providesEnergyType(card, energyType, energyCardName)) {
                return true;
            }
        }
        return false;
    }

    private boolean providesEnergyType(Card card, String energyType, String energyCardName) {
        if (card == null) {
            return false;
        }
        if (card.getPokemonType() != null) {
            return energyType.equalsIgnoreCase(card.getPokemonType());
        }
        // pokemonType is missing on this card row: fall back to the stable English card name.
        return card.getName() != null && energyCardName.equalsIgnoreCase(card.getName());
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard != null
                && (AttachedCardType.BASIC_ENERGY.equals(attachedCard.getAttachedCardType())
                || AttachedCardType.SPECIAL_ENERGY.equals(attachedCard.getAttachedCardType()));
    }
}
