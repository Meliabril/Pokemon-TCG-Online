package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachEnergyFromDiscardToBenchAttackEffectTest {

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @Test
    void shouldAttachWaterEnergyFromDiscardToBenchForEachHeads() {
        AttachEnergyFromDiscardToBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay firstBench = pokemon(1);
        PokemonInPlay secondBench = pokemon(2);
        GameCardInstance firstWaterEnergy = discardCard(UUID.randomUUID(), 1);
        GameCardInstance fightingEnergy = discardCard(UUID.randomUUID(), 2);
        GameCardInstance secondWaterEnergy = discardCard(UUID.randomUUID(), 3);
        when(gameRandomService.flipCoin()).thenReturn(true, false, true);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(secondBench, firstBench));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DISCARD))
                .thenReturn(List.of(firstWaterEnergy, fightingEnergy, secondWaterEnergy));
        when(cardService.getCardEntityById(firstWaterEnergy.getCardId())).thenReturn(energy("Water"));
        when(cardService.getCardEntityById(fightingEnergy.getCardId())).thenReturn(energy("Fighting"));
        when(cardService.getCardEntityById(secondWaterEnergy.getCardId())).thenReturn(energy("Water"));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(3);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, operation("Water", 3, 0)));

        assertThat(result.events()).hasSize(1);
        assertThat(firstWaterEnergy.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(firstWaterEnergy.getZonePosition()).isEqualTo(3);
        assertThat(secondWaterEnergy.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(secondWaterEnergy.getZonePosition()).isEqualTo(4);
        assertThat(fightingEnergy.getZone()).isEqualTo(CardZone.DISCARD);

        ArgumentCaptor<PokemonAttachedCard> captor = ArgumentCaptor.forClass(PokemonAttachedCard.class);
        verify(pokemonAttachedCardStateService, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPokemonInPlay()).isEqualTo(firstBench);
        assertThat(captor.getAllValues().get(1).getPokemonInPlay()).isEqualTo(secondBench);
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.DISCARD);
    }

    @Test
    void shouldNotAttachEnergyWhenThereAreNoHeads() {
        AttachEnergyFromDiscardToBenchAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(false, false, false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(
                context(UUID.randomUUID(), UUID.randomUUID(), operation("Water", 3, 0)));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), any());
        verify(pokemonAttachedCardStateService, never()).save(any());
    }

    @Test
    void shouldAttachFixedAmountWithoutCoinFlipsForOblivionWing() {
        AttachEnergyFromDiscardToBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay bench = pokemon(1);
        GameCardInstance darknessEnergy = discardCard(UUID.randomUUID(), 1);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(bench));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DISCARD))
                .thenReturn(List.of(darknessEnergy));
        when(cardService.getCardEntityById(darknessEnergy.getCardId())).thenReturn(energy("Darkness"));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(0);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(
                context(gameId, attackerUserId, operation("Darkness", 0, 1)));

        assertThat(result.events()).hasSize(1);
        assertThat(darknessEnergy.getZone()).isEqualTo(CardZone.ATTACHED);
        verify(gameRandomService, never()).flipCoin();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersInEventPayload() {
        AttachEnergyFromDiscardToBenchAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(false, false, false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectContext context = context(UUID.randomUUID(), UUID.randomUUID(), operation("Water", 3, 0));
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().attackerPokemon().getId().toString());
    }

    private AttachEnergyFromDiscardToBenchAttackEffect effect() {
        return new AttachEnergyFromDiscardToBenchAttackEffect(
                gameRandomService,
                gameEventFactory,
                gameCardInstanceStateService,
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                cardService);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId, AttackEffectOperation operation) {
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
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation(String energyType, int coinCount, int amount) {
        return new AttackEffectOperation(
                "ATTACH_ENERGY_FROM_DISCARD_TO_BENCH",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                amount,
                null,
                CoinRequirement.NONE,
                false,
                coinCount,
                null,
                null,
                energyType);
    }

    private PokemonInPlay pokemon(int slotPosition) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setSlotPosition(slotPosition);
        return pokemon;
    }

    private GameCardInstance discardCard(UUID cardId, int zonePosition) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setCardId(cardId);
        card.setZone(CardZone.DISCARD);
        card.setZonePosition(zonePosition);
        card.setFaceDown(false);
        return card;
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
