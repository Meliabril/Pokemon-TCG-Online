package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
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
class CoinAllHeadsOpponentDeckDiscardAttackEffectTest {

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Test
    void shouldDiscardTopOpponentDeckCardsForEachAttackerDamageCounterWhenAllCoinsAreHeads() {
        CoinAllHeadsOpponentDeckDiscardAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstDeckCard = deckCard(0);
        GameCardInstance secondDeckCard = deckCard(1);
        GameCardInstance thirdDeckCard = deckCard(2);
        when(gameRandomService.flipCoin()).thenReturn(true, true);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of(firstDeckCard, secondDeckCard, thirdDeckCard));
        when(gameCardInstanceStateService.nextZonePosition(gameId, defenderUserId, CardZone.DISCARD)).thenReturn(4);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, defenderUserId, attackerWithDamageCounters(2)));

        assertThat(result.damageModifier()).isZero();
        assertThat(firstDeckCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(firstDeckCard.getZonePosition()).isEqualTo(4);
        assertThat(firstDeckCard.getFaceDown()).isFalse();
        assertThat(secondDeckCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(secondDeckCard.getZonePosition()).isEqualTo(5);
        assertThat(thirdDeckCard.getZone()).isEqualTo(CardZone.DECK);
        verify(gameCardInstanceStateService).saveAll(List.of(firstDeckCard, secondDeckCard));
        verify(gameCardInstanceStateService).resequenceZone(gameId, defenderUserId, CardZone.DECK);
    }

    @Test
    void shouldNotDiscardCardsWhenAnyCoinIsTails() {
        CoinAllHeadsOpponentDeckDiscardAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(UUID.randomUUID(), UUID.randomUUID(), attackerWithDamageCounters(3)));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), any());
        verify(gameCardInstanceStateService, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersInEventPayload() {
        CoinAllHeadsOpponentDeckDiscardAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectContext context = context(UUID.randomUUID(), UUID.randomUUID(), attackerWithDamageCounters(3));
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().attackerUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().attackerPokemon().getId().toString());
    }

    private CoinAllHeadsOpponentDeckDiscardAttackEffect effect() {
        return new CoinAllHeadsOpponentDeckDiscardAttackEffect(
                gameRandomService,
                gameEventFactory,
                gameCardInstanceStateService);
    }

    private AttackEffectContext context(UUID gameId, UUID defenderUserId, PokemonInPlay attacker) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                UUID.randomUUID(),
                defenderUserId,
                attacker,
                null,
                null,
                null,
                null);
        return new AttackEffectContext(
                resolutionContext,
                null,
                operation(),
                Map.of(),
                1,
                1,
                0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "COIN_ALL_HEADS_OPPONENT_DECK_DISCARD",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                2,
                null);
    }

    private PokemonInPlay attackerWithDamageCounters(int damageCounters) {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setId(UUID.randomUUID());
        attacker.setDamageCounters(damageCounters);
        return attacker;
    }

    private GameCardInstance deckCard(int zonePosition) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setZone(CardZone.DECK);
        card.setZonePosition(zonePosition);
        card.setFaceDown(true);
        return card;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
