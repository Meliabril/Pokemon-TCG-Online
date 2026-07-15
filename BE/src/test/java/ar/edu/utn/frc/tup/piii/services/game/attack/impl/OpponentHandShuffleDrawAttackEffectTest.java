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
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpponentHandShuffleDrawAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private DrawCardsEffectService drawCardsEffectService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldShuffleOpponentHandIntoDeckAndDrawRequestedCards() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance handCard = cardInstance(defenderUserId, CardZone.HAND, 1);
        GameCardInstance deckCard = cardInstance(defenderUserId, CardZone.DECK, 1);
        GameCardInstance drawnCard = cardInstance(defenderUserId, CardZone.HAND, 1);
        GameEventDto publicDrawEvent = event(gameId, GameEventType.CARD_DRAWN, false);
        GameEventDto privateDrawEvent = event(gameId, GameEventType.CARD_DRAWN, true);
        GameEventDto effectEvent = event(gameId, GameEventType.ATTACK_EFFECT_RESOLVED, false);

        OpponentHandShuffleDrawAttackEffect effect = new OpponentHandShuffleDrawAttackEffect(
                gameCardInstanceStateService,
                drawCardsEffectService,
                gameRandomService,
                gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.HAND))
                .thenReturn(List.of(handCard));
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(gameRandomService.shuffledCopy(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(drawCardsEffectService.drawCards(gameId, defenderUserId, 4)).thenReturn(List.of(drawnCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(5), anyMap()))
                .thenReturn(publicDrawEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(5), anyMap(), eq(defenderUserId)))
                .thenReturn(privateDrawEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap()))
                .thenReturn(effectEvent);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, defenderUserId));

        assertThat(handCard.getZone()).isEqualTo(CardZone.DECK);
        assertThat(handCard.getFaceDown()).isTrue();
        assertThat(result.events()).containsExactly(publicDrawEvent, privateDrawEvent, effectEvent);
        verify(gameCardInstanceStateService, times(2)).saveAll(List.of(deckCard, handCard));
        verify(gameCardInstanceStateService).flush();
        verify(gameCardInstanceStateService).resequenceZone(gameId, defenderUserId, CardZone.HAND);
        verify(drawCardsEffectService).drawCards(gameId, defenderUserId, 4);
        assertEffectPayload(defenderUserId);
    }

    private void assertEffectPayload(UUID defenderUserId) {
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(
                any(),
                eq(GameEventType.ATTACK_EFFECT_RESOLVED),
                eq(5),
                payloadCaptor.capture());

        assertThat(payloadCaptor.getValue())
                .containsEntry("effectType", "SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW")
                .containsEntry("opponentPlayerId", defenderUserId.toString())
                .containsEntry("shuffledHandCards", 1)
                .containsEntry("cardsDrawn", 1);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId, UUID defenderUserId) {
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                null,
                null,
                null,
                null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "SHUFFLE_OPPONENT_HAND_INTO_DECK_DRAW",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                4,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 5, 1, 0);
    }

    private GameCardInstance cardInstance(UUID ownerUserId, CardZone zone, int position) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(UUID.randomUUID());
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(position);
        cardInstance.setFaceDown(zone != CardZone.HAND);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId, GameEventType type, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, type, 5, privateEvent, Instant.now(), Map.of());
    }
}
