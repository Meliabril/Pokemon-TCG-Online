package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AttachedEnergyCounter {

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;

    public int countAll(PokemonInPlay pokemon) {
        if (pokemon == null || pokemon.getId() == null) {
            return 0;
        }

        int energyCount = 0;
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId());
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (isEnergyAttachment(attachedCard)) {
                energyCount++;
            }
        }
        return energyCount;
    }

    private boolean isEnergyAttachment(PokemonAttachedCard attachedCard) {
        if (attachedCard == null || attachedCard.getGameCardInstance() == null) {
            return false;
        }

        Card card = cardService.getCardEntityById(attachedCard.getGameCardInstance().getCardId());
        return isEnergyCard(card);
    }

    public int countByType(PokemonInPlay pokemon, String energyType) {
        if (pokemon == null || pokemon.getId() == null || energyType == null || energyType.isBlank()) {
            return 0;
        }

        int energyCount = 0;
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId());
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (isMatchingEnergy(attachedCard, energyType)) {
                energyCount++;
            }
        }
        return energyCount;
    }

    public int countDistinctBasicEnergyTypes(PokemonInPlay pokemon) {
        if (pokemon == null || pokemon.getId() == null) {
            return 0;
        }

        Set<String> energyTypes = new LinkedHashSet<>();
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId());
        for (PokemonAttachedCard attachedCard : attachedCards) {
            String energyType = basicEnergyType(attachedCard);
            if (energyType != null && !energyType.isBlank()) {
                energyTypes.add(energyType.toUpperCase());
            }
        }
        return energyTypes.size();
    }

    private boolean isMatchingEnergy(PokemonAttachedCard attachedCard, String energyType) {
        if (attachedCard == null) {
            return false;
        }

        GameCardInstance cardInstance = attachedCard.getGameCardInstance();
        if (cardInstance == null || cardInstance.getCardId() == null) {
            return false;
        }

        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (!isEnergyCard(card)) {
            return false;
        }

        if (card.getPokemonType() != null && card.getPokemonType().equalsIgnoreCase(energyType)) {
            return true;
        }

        String expectedEnergyName = energyType + " Energy";
        return card.getName() != null && card.getName().equalsIgnoreCase(expectedEnergyName);
    }

    private String basicEnergyType(PokemonAttachedCard attachedCard) {
        if (attachedCard == null) {
            return null;
        }

        GameCardInstance cardInstance = attachedCard.getGameCardInstance();
        if (cardInstance == null || cardInstance.getCardId() == null) {
            return null;
        }

        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card == null || !CardCategory.BASIC_ENERGY.equals(card.getCategory())) {
            return null;
        }

        if (card.getPokemonType() != null && !card.getPokemonType().isBlank()) {
            return card.getPokemonType();
        }

        if (card.getName() != null && card.getName().endsWith(" Energy")) {
            return card.getName().replace(" Energy", "");
        }
        return null;
    }

    private boolean isEnergyCard(Card card) {
        if (card == null || card.getCategory() == null) {
            return false;
        }

        return CardCategory.BASIC_ENERGY.equals(card.getCategory())
                || CardCategory.SPECIAL_ENERGY.equals(card.getCategory());
    }
}
