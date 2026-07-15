package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.effect.DiscardCardEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardEnergyAttackEffectTest {

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private DiscardCardEffectService discardCardEffectService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private CardService cardService;

    @Test
    void shouldOnlyDiscardEnergyMatchingRequestedType() {
        DiscardEnergyAttackEffect effect = new DiscardEnergyAttackEffect(
                pokemonAttachedCardStateService, discardCardEffectService, gameEventFactory, cardService);

        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defenderPokemon.setId(UUID.randomUUID());
        UUID ownerUserId = UUID.randomUUID();
        defenderPokemon.setOwnerUserId(ownerUserId);

        PokemonAttachedCard darknessEnergy = attachedEnergy(defenderPokemon, "Darkness");
        PokemonAttachedCard waterEnergy = attachedEnergy(defenderPokemon, "Water");

        GameCardInstance discardedInstance = new GameCardInstance();
        discardedInstance.setId(UUID.randomUUID());
        discardedInstance.setCardId(darknessEnergy.getGameCardInstance().getCardId());
        discardedInstance.setZone(CardZone.DISCARD);

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defenderPokemon.getId()))
                .thenReturn(List.of(waterEnergy, darknessEnergy));
        when(cardService.getCardEntityById(waterEnergy.getGameCardInstance().getCardId())).thenReturn(energy("Water"));
        when(cardService.getCardEntityById(darknessEnergy.getGameCardInstance().getCardId())).thenReturn(energy("Darkness"));
        when(discardCardEffectService.discardAttachedCard(any(), eq(ownerUserId), eq(darknessEnergy)))
                .thenReturn(discardedInstance);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(defenderPokemon, "Darkness"));

        assertThat(result.events()).hasSize(1);
        // Water energy was not discarded; darkness energy was delegated to discardCardEffectService
        assertThat(waterEnergy.getGameCardInstance().getZone()).isEqualTo(CardZone.ATTACHED);
    }

    private AttackEffectContext context(PokemonInPlay defenderPokemon, String energyType) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new PokemonInPlay(), null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "DISCARD_ENERGY",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                1,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                energyType);
        return new AttackEffectContext(resolutionContext, defenderPokemon, operation, Map.of(), 1, 1, 0);
    }

    private PokemonAttachedCard attachedEnergy(PokemonInPlay pokemon, String label) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setCardId(UUID.randomUUID());
        cardInstance.setZone(CardZone.ATTACHED);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(pokemon);
        attachedCard.setGameCardInstance(cardInstance);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        return attachedCard;
    }

    private Card energy(String pokemonType) {
        Card card = new Card();
        card.setCategory(CardCategory.BASIC_ENERGY);
        card.setPokemonType(pokemonType);
        return card;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
