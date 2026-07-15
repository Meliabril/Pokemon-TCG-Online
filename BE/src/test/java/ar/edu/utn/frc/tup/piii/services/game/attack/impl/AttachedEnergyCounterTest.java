package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachedEnergyCounterTest {

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @Test
    void shouldCountDistinctBasicEnergyTypesOnly() {
        AttachedEnergyCounter counter = new AttachedEnergyCounter(pokemonAttachedCardStateService, cardService);
        PokemonInPlay pokemon = pokemon();
        PokemonAttachedCard waterByType = attachedCard();
        PokemonAttachedCard waterByName = attachedCard();
        PokemonAttachedCard fireByType = attachedCard();
        PokemonAttachedCard specialEnergy = attachedCard();

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId()))
                .thenReturn(List.of(waterByType, waterByName, fireByType, specialEnergy));
        when(cardService.getCardEntityById(waterByType.getGameCardInstance().getCardId()))
                .thenReturn(energy(CardCategory.BASIC_ENERGY, "Water", "Water Energy"));
        when(cardService.getCardEntityById(waterByName.getGameCardInstance().getCardId()))
                .thenReturn(energy(CardCategory.BASIC_ENERGY, null, "Water Energy"));
        when(cardService.getCardEntityById(fireByType.getGameCardInstance().getCardId()))
                .thenReturn(energy(CardCategory.BASIC_ENERGY, "Fire", "Fire Energy"));
        when(cardService.getCardEntityById(specialEnergy.getGameCardInstance().getCardId()))
                .thenReturn(energy(CardCategory.SPECIAL_ENERGY, null, "Double Colorless Energy"));

        int result = counter.countDistinctBasicEnergyTypes(pokemon);

        assertThat(result).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroWithoutLookingUpCardsWhenPokemonIsMissing() {
        AttachedEnergyCounter counter = new AttachedEnergyCounter(pokemonAttachedCardStateService, cardService);

        assertThat(counter.countDistinctBasicEnergyTypes(null)).isZero();
        verifyNoInteractions(pokemonAttachedCardStateService, cardService);
    }

    private PokemonInPlay pokemon() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setOwnerUserId(UUID.randomUUID());
        return pokemon;
    }

    private PokemonAttachedCard attachedCard() {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setCardId(UUID.randomUUID());

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setId(UUID.randomUUID());
        attachedCard.setGameCardInstance(cardInstance);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        return attachedCard;
    }

    private Card energy(CardCategory category, String pokemonType, String name) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setCategory(category);
        card.setPokemonType(pokemonType);
        card.setName(name);
        return card;
    }
}
