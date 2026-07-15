package ar.edu.utn.frc.tup.piii.services.game.stadium.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StadiumModifierServiceImplTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @InjectMocks
    private StadiumModifierServiceImpl service;

    @Test
    void isRetreatFreeReturnsFalseWhenPokemonIsNull() {
        assertThat(service.isRetreatFree(UUID.randomUUID(), null)).isFalse();
    }

    @Test
    void isRetreatFreeReturnsFalseWhenFairyGardenIsNotActive() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of());

        assertThat(service.isRetreatFree(gameId, pokemon)).isFalse();
    }

    @Test
    void isRetreatFreeReturnsFalseWhenPokemonHasNoFairyEnergy() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-117"));
        PokemonAttachedCard fireEnergy = attachedEnergy(energyCard("Fire"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-117"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(fireEnergy));
        when(cardService.getCardEntityById(fireEnergy.getGameCardInstance().getCardId())).thenReturn(energyCard("Fire"));

        assertThat(service.isRetreatFree(gameId, pokemon)).isFalse();
    }

    @Test
    void isRetreatFreeReturnsTrueWhenFairyGardenAndFairyEnergyArePresent() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-117"));
        PokemonAttachedCard fairyEnergy = attachedEnergy(energyCard("Fairy"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-117"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(fairyEnergy));
        when(cardService.getCardEntityById(fairyEnergy.getGameCardInstance().getCardId())).thenReturn(energyCard("Fairy"));

        assertThat(service.isRetreatFree(gameId, pokemon)).isTrue();
    }

    @Test
    void isWeaknessNegatedReturnsFalseWhenShadowCircleIsNotActive() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of());

        assertThat(service.isWeaknessNegated(gameId, pokemon)).isFalse();
    }

    @Test
    void isWeaknessNegatedReturnsFalseWhenDefenderHasNoDarknessEnergy() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-126"));
        PokemonAttachedCard psychicEnergy = attachedEnergy(energyCard("Psychic"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-126"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(psychicEnergy));
        when(cardService.getCardEntityById(psychicEnergy.getGameCardInstance().getCardId())).thenReturn(energyCard("Psychic"));

        assertThat(service.isWeaknessNegated(gameId, pokemon)).isFalse();
    }

    @Test
    void isWeaknessNegatedReturnsTrueWhenShadowCircleAndDarknessEnergyArePresent() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-126"));
        PokemonAttachedCard darknessEnergy = attachedEnergy(energyCard("Darkness"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-126"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(darknessEnergy));
        when(cardService.getCardEntityById(darknessEnergy.getGameCardInstance().getCardId())).thenReturn(energyCard("Darkness"));

        assertThat(service.isWeaknessNegated(gameId, pokemon)).isTrue();
    }

    @Test
    void isWeaknessNegatedIgnoresDarknessEnergyWhenWrongStadiumIsActive() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-117"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-117"));

        assertThat(service.isWeaknessNegated(gameId, pokemon)).isFalse();
    }

    @Test
    void isWeaknessNegatedIgnoresAttackerDarknessEnergyWhenDefenderHasNone() {
        // Case C from the bug report: the attacker having Darkness Energy must not matter.
        // Here we only ever ask the service about the defender's own Pokemon, so a defender
        // with no Darkness Energy must stay vulnerable to weakness, regardless of what the
        // attacker has attached (the attacker is never even passed to this service).
        UUID gameId = UUID.randomUUID();
        PokemonInPlay defenderPokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-126"));

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-126"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defenderPokemon.getId())).thenReturn(List.of());

        assertThat(service.isWeaknessNegated(gameId, defenderPokemon)).isFalse();
    }

    @Test
    void isRetreatFreeIgnoresNonEnergyAttachedCardsLikeTools() {
        // A Pokemon Tool must never count toward Fairy Garden's check, even if its underlying
        // card row happens to carry pokemonType "Fairy" for unrelated reasons.
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-117"));
        Card toolCard = energyCard("Fairy");
        PokemonAttachedCard tool = attachedEnergy(toolCard);
        tool.setAttachedCardType(AttachedCardType.POKEMON_TOOL);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-117"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(tool));

        assertThat(service.isRetreatFree(gameId, pokemon)).isFalse();
    }

    @Test
    void isRetreatFreeFallsBackToCardNameWhenPokemonTypeIsMissing() {
        // Real imported "Fairy Energy" rows can have a null pokemonType because the source API
        // only reliably sends "types" for Pokemon cards. The stable English card name is the
        // documented fallback (matches FairyTransferAbilityHandler / PassiveAbilityServiceImpl).
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-117"));
        Card fairyEnergyNoType = new Card();
        fairyEnergyNoType.setId(UUID.randomUUID());
        fairyEnergyNoType.setName("Fairy Energy");
        fairyEnergyNoType.setPokemonType(null);
        PokemonAttachedCard fairyEnergy = attachedEnergy(fairyEnergyNoType);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-117"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(fairyEnergy));
        when(cardService.getCardEntityById(fairyEnergy.getGameCardInstance().getCardId())).thenReturn(fairyEnergyNoType);

        assertThat(service.isRetreatFree(gameId, pokemon)).isTrue();
    }

    @Test
    void isWeaknessNegatedFallsBackToCardNameWhenPokemonTypeIsMissing() {
        UUID gameId = UUID.randomUUID();
        PokemonInPlay pokemon = pokemonInPlay();
        GameCardInstance stadiumInstance = stadiumInstance(stadiumCard("xy1-126"));
        Card darknessEnergyNoType = new Card();
        darknessEnergyNoType.setId(UUID.randomUUID());
        darknessEnergyNoType.setName("Darkness Energy");
        darknessEnergyNoType.setPokemonType(null);
        PokemonAttachedCard darknessEnergy = attachedEnergy(darknessEnergyNoType);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(stadiumInstance));
        when(cardService.getCardEntityById(stadiumInstance.getCardId())).thenReturn(stadiumCard("xy1-126"));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())).thenReturn(List.of(darknessEnergy));
        when(cardService.getCardEntityById(darknessEnergy.getGameCardInstance().getCardId())).thenReturn(darknessEnergyNoType);

        assertThat(service.isWeaknessNegated(gameId, pokemon)).isTrue();
    }

    private PokemonInPlay pokemonInPlay() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        return pokemon;
    }

    private Card stadiumCard(String externalId) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(externalId);
        return card;
    }

    private Card energyCard(String pokemonType) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setPokemonType(pokemonType);
        return card;
    }

    private GameCardInstance stadiumInstance(Card card) {
        GameCardInstance instance = new GameCardInstance();
        instance.setId(UUID.randomUUID());
        instance.setCardId(card.getId());
        instance.setZone(CardZone.STADIUM);
        return instance;
    }

    private PokemonAttachedCard attachedEnergy(Card card) {
        GameCardInstance instance = new GameCardInstance();
        instance.setId(UUID.randomUUID());
        instance.setCardId(card.getId());
        instance.setZone(CardZone.ATTACHED);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setId(UUID.randomUUID());
        attachedCard.setGameCardInstance(instance);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        return attachedCard;
    }
}
