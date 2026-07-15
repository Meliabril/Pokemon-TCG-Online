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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomOpponentHandCardRevealShuffleAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldRevealRandomHandCardPrivatelyAndShuffleItIntoDeck() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance handCard = cardInstance(defenderUserId, CardZone.HAND, 1);
        GameCardInstance deckCard = cardInstance(defenderUserId, CardZone.DECK, 1);
        GameEventDto privateEvent = event(gameId, true);
        GameEventDto publicEvent = event(gameId, false);

        RandomOpponentHandCardRevealShuffleAttackEffect effect = new RandomOpponentHandCardRevealShuffleAttackEffect(
                gameCardInstanceStateService, gameRandomService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.HAND))
                .thenReturn(List.of(handCard));
        when(gameRandomService.chooseOne(List.of(handCard))).thenReturn(handCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(gameRandomService.shuffledCopy(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap(), eq(attackerUserId)))
                .thenReturn(privateEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap()))
                .thenReturn(publicEvent);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, defenderUserId));

        assertThat(handCard.getZone()).isEqualTo(CardZone.DECK);
        assertThat(handCard.getZonePosition()).isEqualTo(2);
        verify(gameCardInstanceStateService).resequenceZone(gameId, defenderUserId, CardZone.HAND);
        assertRevealPayloadsIncludeRevealedCard(gameId, attackerUserId, handCard);
        assertThat(result.events()).containsExactly(privateEvent, publicEvent);
    }

    @Test
    void shouldDoNothingWhenOpponentHandIsEmpty() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        RandomOpponentHandCardRevealShuffleAttackEffect effect = new RandomOpponentHandCardRevealShuffleAttackEffect(
                gameCardInstanceStateService, gameRandomService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.HAND))
                .thenReturn(List.of());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, defenderUserId));

        assertThat(result.events()).isEmpty();
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
                "RANDOM_OPPONENT_HAND_CARD_REVEAL_SHUFFLE",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
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

    private GameEventDto event(UUID gameId, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, privateEvent, Instant.now(), Map.of());
    }

    private void assertRevealPayloadsIncludeRevealedCard(
            UUID gameId,
            UUID attackerUserId,
            GameCardInstance revealedCard) {
        ArgumentCaptor<Map<String, Object>> privatePayloadCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Object>> publicPayloadCaptor = ArgumentCaptor.forClass(Map.class);

        verify(gameEventFactory).privateEvent(
                eq(gameId),
                eq(GameEventType.ATTACK_EFFECT_RESOLVED),
                eq(5),
                privatePayloadCaptor.capture(),
                eq(attackerUserId));
        verify(gameEventFactory).publicEvent(
                eq(gameId),
                eq(GameEventType.ATTACK_EFFECT_RESOLVED),
                eq(5),
                publicPayloadCaptor.capture());

        assertThat(privatePayloadCaptor.getValue())
                .containsEntry("revealedCardId", revealedCard.getCardId().toString())
                .containsEntry("revealedCardInstanceId", revealedCard.getId().toString());
        assertThat(publicPayloadCaptor.getValue())
                .containsEntry("revealedCardId", revealedCard.getCardId().toString());
    }
}
