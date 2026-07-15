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
class SearchAnyCardFromDeckAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldMoveChosenCardFromDeckToHandAndShuffleRemainingDeck() {
        SearchAnyCardFromDeckAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance firstCard = deckCard(UUID.randomUUID(), 1);
        GameCardInstance chosenCard = deckCard(UUID.randomUUID(), 2);
        GameCardInstance thirdCard = deckCard(UUID.randomUUID(), 3);
        List<GameCardInstance> deckCards = List.of(firstCard, chosenCard, thirdCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(deckCards);
        when(gameRandomService.chooseOne(deckCards)).thenReturn(chosenCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(4);
        when(gameRandomService.shuffledCopy(List.of(firstCard, thirdCard))).thenReturn(List.of(thirdCard, firstCard));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).hasSize(1);
        assertThat(chosenCard.getZone()).isEqualTo(CardZone.HAND);
        assertThat(chosenCard.getZonePosition()).isEqualTo(4);
        assertThat(chosenCard.getFaceDown()).isFalse();
        assertThat(thirdCard.getZonePosition()).isEqualTo(1);
        assertThat(firstCard.getZonePosition()).isEqualTo(2);
        verify(gameCardInstanceStateService).save(chosenCard);
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.HAND);
        verify(gameCardInstanceStateService).flush();
    }

    @Test
    void shouldOnlyEmitEventWhenDeckIsEmpty() {
        SearchAnyCardFromDeckAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of());
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).save(any());
        verify(gameRandomService, never()).shuffledCopy(any());
    }

    private SearchAnyCardFromDeckAttackEffect effect() {
        return new SearchAnyCardFromDeckAttackEffect(gameCardInstanceStateService, gameRandomService, gameEventFactory);
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
                "SEARCH_ANY_CARD_FROM_DECK",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
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

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
