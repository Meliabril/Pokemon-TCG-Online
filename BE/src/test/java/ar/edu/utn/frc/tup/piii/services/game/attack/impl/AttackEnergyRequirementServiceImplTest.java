package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackEnergyRequirementServiceImplTest {

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @Test
    void shouldAllowColorlessCostWithAnyBasicEnergy() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Colorless", 1));
        UUID fireEnergyCardId = UUID.randomUUID();
        PokemonAttachedCard attachedEnergy = attachedEnergy(fireEnergyCardId, AttachedCardType.BASIC_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(attachedEnergy));
        when(cardService.getCardEntityById(fireEnergyCardId)).thenReturn(energyCard("Fire"));

        assertThat(service.hasRequiredEnergy(attacker, attack)).isTrue();
    }

    @Test
    void shouldAllowTypedAndColorlessCostWithRemainingAnyEnergy() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Fire", 1), cost("Colorless", 1));
        UUID fireEnergyCardId = UUID.randomUUID();
        UUID waterEnergyCardId = UUID.randomUUID();
        PokemonAttachedCard fireEnergy = attachedEnergy(fireEnergyCardId, AttachedCardType.BASIC_ENERGY);
        PokemonAttachedCard waterEnergy = attachedEnergy(waterEnergyCardId, AttachedCardType.BASIC_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(fireEnergy, waterEnergy));
        when(cardService.getCardEntityById(fireEnergyCardId)).thenReturn(energyCard("Fire"));
        when(cardService.getCardEntityById(waterEnergyCardId)).thenReturn(energyCard("Water"));

        assertThat(service.hasRequiredEnergy(attacker, attack)).isTrue();
    }

    @Test
    void shouldRejectTypedCostWhenOnlyDifferentBasicEnergyIsAttached() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Fire", 1));
        UUID waterEnergyCardId = UUID.randomUUID();
        PokemonAttachedCard waterEnergy = attachedEnergy(waterEnergyCardId, AttachedCardType.BASIC_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(waterEnergy));
        when(cardService.getCardEntityById(waterEnergyCardId)).thenReturn(energyCard("Water"));

        assertThat(service.hasRequiredEnergy(attacker, attack)).isFalse();
    }

    @Test
    void shouldAllowTypedCostWhenBasicEnergyHasNoPokemonTypeButMatchingName() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Fire", 1));
        UUID fireEnergyCardId = UUID.randomUUID();
        PokemonAttachedCard fireEnergy = attachedEnergy(fireEnergyCardId, AttachedCardType.BASIC_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(fireEnergy));
        when(cardService.getCardEntityById(fireEnergyCardId)).thenReturn(energyCardByName("Fire Energy"));

        assertThat(service.hasRequiredEnergy(attacker, attack)).isTrue();
    }

    @Test
    void shouldCountDoubleColorlessEnergyAsTwoWildcardEnergy() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Colorless", 2));
        UUID doubleColorlessCardId = UUID.randomUUID();
        PokemonAttachedCard doubleColorless = attachedEnergy(doubleColorlessCardId, AttachedCardType.SPECIAL_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(doubleColorless));
        when(cardService.getCardEntityById(doubleColorlessCardId)).thenReturn(doubleColorlessEnergyCard());

        assertThat(service.hasRequiredEnergy(attacker, attack)).isTrue();
    }

    @Test
    void shouldCountOtherSpecialEnergyAsSingleWildcardEnergy() {
        AttackEnergyRequirementServiceImpl service = service();
        PokemonInPlay attacker = pokemonInPlay();
        Attack attack = attack(cost("Colorless", 2));
        UUID rainbowEnergyCardId = UUID.randomUUID();
        PokemonAttachedCard rainbowEnergy = attachedEnergy(rainbowEnergyCardId, AttachedCardType.SPECIAL_ENERGY);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(rainbowEnergy));
        when(cardService.getCardEntityById(rainbowEnergyCardId)).thenReturn(specialEnergyCard("xy1-131"));

        assertThat(service.hasRequiredEnergy(attacker, attack)).isFalse();
    }

    private AttackEnergyRequirementServiceImpl service() {
        return new AttackEnergyRequirementServiceImpl(pokemonAttachedCardStateService, cardService);
    }

    private PokemonInPlay pokemonInPlay() {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(UUID.randomUUID());
        return pokemonInPlay;
    }

    private Attack attack(AttackCost... costs) {
        Attack attack = new Attack();
        attack.setCosts(new LinkedHashSet<>(List.of(costs)));
        return attack;
    }

    private AttackCost cost(String energyType, int quantity) {
        AttackCost cost = new AttackCost();
        cost.setEnergyType(energyType);
        cost.setQuantity(quantity);
        return cost;
    }

    private PokemonAttachedCard attachedEnergy(UUID cardId, AttachedCardType attachedCardType) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setCardId(cardId);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setAttachedCardType(attachedCardType);
        attachedCard.setGameCardInstance(cardInstance);
        return attachedCard;
    }

    private Card energyCard(String energyType) {
        Card card = new Card();
        card.setPokemonType(energyType);
        return card;
    }

    private Card energyCardByName(String name) {
        Card card = new Card();
        card.setName(name);
        return card;
    }

    private Card doubleColorlessEnergyCard() {
        return specialEnergyCard("xy1-130");
    }

    private Card specialEnergyCard(String externalId) {
        Card card = new Card();
        card.setExternalId(externalId);
        return card;
    }
}
