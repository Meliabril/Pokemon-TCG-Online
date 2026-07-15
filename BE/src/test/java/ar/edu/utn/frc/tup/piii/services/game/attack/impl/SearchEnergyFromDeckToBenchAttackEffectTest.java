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
class SearchEnergyFromDeckToBenchAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAttachTwoFairyEnergyCardsFromDeckToFirstBenchPokemonAndShuffleDeck() {
        SearchEnergyFromDeckToBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay firstBench = pokemon(1);
        PokemonInPlay secondBench = pokemon(2);
        GameCardInstance firstFairyEnergy = deckCard(UUID.randomUUID(), 1);
        GameCardInstance darknessEnergy = deckCard(UUID.randomUUID(), 2);
        GameCardInstance secondFairyEnergy = deckCard(UUID.randomUUID(), 3);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId))
                .thenReturn(List.of(secondBench, firstBench));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(firstFairyEnergy, darknessEnergy, secondFairyEnergy));
        when(cardService.getCardEntityById(firstFairyEnergy.getCardId())).thenReturn(energy("Fairy"));
        when(cardService.getCardEntityById(darknessEnergy.getCardId())).thenReturn(energy("Darkness"));
        when(cardService.getCardEntityById(secondFairyEnergy.getCardId())).thenReturn(energy("Fairy"));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(4);
        when(gameRandomService.shuffledCopy(List.of(darknessEnergy))).thenReturn(List.of(darknessEnergy));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).hasSize(1);
        assertThat(firstFairyEnergy.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(firstFairyEnergy.getZonePosition()).isEqualTo(4);
        assertThat(secondFairyEnergy.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(secondFairyEnergy.getZonePosition()).isEqualTo(5);
        assertThat(darknessEnergy.getZone()).isEqualTo(CardZone.DECK);

        ArgumentCaptor<PokemonAttachedCard> attachedCardCaptor = ArgumentCaptor.forClass(PokemonAttachedCard.class);
        verify(pokemonAttachedCardStateService, org.mockito.Mockito.times(2)).save(attachedCardCaptor.capture());
        assertThat(attachedCardCaptor.getAllValues().get(0).getPokemonInPlay()).isEqualTo(firstBench);
        assertThat(attachedCardCaptor.getAllValues().get(1).getPokemonInPlay()).isEqualTo(secondBench);
        verify(gameCardInstanceStateService).flush();
        verify(gameCardInstanceStateService, org.mockito.Mockito.times(2)).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldEmitAttachedTargetsForFrontendAnimations() {
        SearchEnergyFromDeckToBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        PokemonInPlay bench = pokemon(1);
        GameCardInstance fairyEnergy = deckCard(UUID.randomUUID(), 1);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId)).thenReturn(List.of(bench));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(fairyEnergy));
        when(cardService.getCardEntityById(fairyEnergy.getCardId())).thenReturn(energy("Fairy"));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(0);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        effect.apply(context(gameId, attackerUserId));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("effectType")).isEqualTo("SEARCH_ENERGY_FROM_DECK_TO_BENCH");
        assertThat(payload.get("attachedCount")).isEqualTo(1);
        assertThat(payload.get("attachedPokemonInPlayIds")).isEqualTo(List.of(bench.getId().toString()));
        assertThat(payload.get("attachedCardInstanceIds")).isEqualTo(List.of(fairyEnergy.getId().toString()));
    }

    @Test
    void shouldNotSearchDeckWhenNoBenchPokemonCanReceiveEnergy() {
        SearchEnergyFromDeckToBenchAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId)).thenReturn(List.of());
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        effect.apply(context(gameId, attackerUserId));

        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), any());
        verify(pokemonAttachedCardStateService, never()).save(any());
        verify(gameCardInstanceStateService, never()).nextZonePosition(any(), any(), any());
        verify(gameCardInstanceStateService, never()).flush();
    }

    private SearchEnergyFromDeckToBenchAttackEffect effect() {
        return new SearchEnergyFromDeckToBenchAttackEffect(
                gameCardInstanceStateService,
                pokemonAttachedCardStateService,
                pokemonInPlayStateService,
                cardService,
                gameRandomService,
                gameEventFactory);
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
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "SEARCH_ENERGY_FROM_DECK_TO_BENCH",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                2,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                "Fairy");
    }

    private PokemonInPlay pokemon(int slotPosition) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setSlotPosition(slotPosition);
        return pokemon;
    }

    private GameCardInstance deckCard(UUID cardId, int zonePosition) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setCardId(cardId);
        card.setZone(CardZone.DECK);
        card.setZonePosition(zonePosition);
        card.setFaceDown(true);
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
