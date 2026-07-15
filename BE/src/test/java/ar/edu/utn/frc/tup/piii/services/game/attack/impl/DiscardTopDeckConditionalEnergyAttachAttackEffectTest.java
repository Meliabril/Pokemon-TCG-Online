package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardTopDeckConditionalEnergyAttachAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAttachTopCardWhenItIsFightingEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance topCard = cardInstance(attackerUserId, cardId, 1);
        Card fightingEnergy = new Card();
        fightingEnergy.setCategory(CardCategory.BASIC_ENERGY);
        fightingEnergy.setPokemonType("Fighting");

        DiscardTopDeckConditionalEnergyAttachAttackEffect effect = new DiscardTopDeckConditionalEnergyAttachAttackEffect(
                gameCardInstanceStateService, pokemonAttachedCardStateService, cardService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(topCard));
        when(cardService.getCardEntityById(cardId)).thenReturn(fightingEnergy);
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(3);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap()))
                .thenReturn(event(gameId));

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(topCard.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(topCard.getZonePosition()).isEqualTo(3);
        org.mockito.Mockito.verify(pokemonAttachedCardStateService).save(org.mockito.Mockito.argThat(
                attached -> attached.getAttachedCardType() == AttachedCardType.BASIC_ENERGY
                        && attached.getGameCardInstance() == topCard));
        org.mockito.Mockito.verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DECK);
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldDiscardTopCardWhenItIsNotFightingEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GameCardInstance topCard = cardInstance(attackerUserId, cardId, 1);
        Card waterEnergy = new Card();
        waterEnergy.setCategory(CardCategory.BASIC_ENERGY);
        waterEnergy.setPokemonType("Water");

        DiscardTopDeckConditionalEnergyAttachAttackEffect effect = new DiscardTopDeckConditionalEnergyAttachAttackEffect(
                gameCardInstanceStateService, pokemonAttachedCardStateService, cardService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(topCard));
        when(cardService.getCardEntityById(cardId)).thenReturn(waterEnergy);
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.DISCARD)).thenReturn(2);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap()))
                .thenReturn(event(gameId));

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(topCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(topCard.getZonePosition()).isEqualTo(2);
        org.mockito.Mockito.verify(pokemonAttachedCardStateService, org.mockito.Mockito.never()).save(org.mockito.Mockito.any());
        assertThat(result.events()).hasSize(1);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId) {
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                UUID.randomUUID(),
                attackerPokemon,
                null,
                null,
                null,
                null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "DISCARD_TOP_DECK_CONDITIONAL_ENERGY_ATTACH",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 5, 1, 0);
    }

    private GameCardInstance cardInstance(UUID ownerUserId, UUID cardId, int position) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(CardZone.DECK);
        cardInstance.setZonePosition(position);
        cardInstance.setFaceDown(true);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, false, Instant.now(), Map.of());
    }
}
