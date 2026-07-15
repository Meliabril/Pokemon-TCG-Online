package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpponentCoinTailsHandDiscardAttackEffectTest {

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Test
    void shouldRequireDefenderChoiceOfferingExactlyOneCardPerTailsAmongOwnHand() {
        OpponentCoinTailsHandDiscardAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance firstHandCard = handCard(1);
        GameCardInstance secondHandCard = handCard(2);
        GameCardInstance thirdHandCard = handCard(3);
        when(gameRandomService.flipCoin()).thenReturn(false, true, false, false);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.HAND))
                .thenReturn(List.of(firstHandCard, secondHandCard, thirdHandCard));
        lenient().when(cardService.getCardEntityById(any())).thenReturn(cardEntity());
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, defenderUserId));

        assertThat(result.damageModifier()).isZero();
        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo(OpponentCoinTailsHandDiscardAttackEffect.SELECT_HAND_CARDS_TO_DISCARD);
        assertThat(result.choicePlayerId()).isEqualTo(defenderUserId);
        assertThat(result.choicePayload().get("discardCount")).isEqualTo(3);
        assertThat(result.choicePayload().get("cards")).asList().hasSize(3);
        assertThat(result.choicePayload().get("cards")).asList().first().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKeys("imageSmallUrl", "imageLargeUrl");

        // The hand must not be touched yet - the defender still has to choose which cards to
        // discard; AttackChoiceServiceImpl applies the actual discard once they confirm.
        verify(gameCardInstanceStateService, never()).saveAll(any());
        verify(gameRandomService, never()).shuffledCopy(any());
    }

    @Test
    void shouldCapDiscardCountAtHandSizeWhenTailsExceedHandSize() {
        OpponentCoinTailsHandDiscardAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance onlyHandCard = handCard(1);
        when(gameRandomService.flipCoin()).thenReturn(false, false, false, false);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.HAND))
                .thenReturn(List.of(onlyHandCard));
        lenient().when(cardService.getCardEntityById(any())).thenReturn(cardEntity());
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, defenderUserId));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choicePayload().get("discardCount")).isEqualTo(1);
        assertThat(result.choicePayload().get("cards")).asList().hasSize(1);
    }

    @Test
    void shouldNotRequireChoiceWhenAllCoinsAreHeads() {
        OpponentCoinTailsHandDiscardAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, true, true, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(result.choiceRequired()).isFalse();
        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).findByGameIdAndOwnerUserIdAndZone(any(), any(), any());
        verify(gameCardInstanceStateService, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeActorAndPokemonIdentifiersFromDefenderInEventPayload() {
        OpponentCoinTailsHandDiscardAttackEffect effect = effect();
        when(gameRandomService.flipCoin()).thenReturn(true, true, true, true);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectContext context = context(UUID.randomUUID(), UUID.randomUUID());
        effect.apply(context);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("actorPlayerId")).isEqualTo(context.resolutionContext().defenderUserId().toString());
        assertThat(payload.get("pokemonInPlayId")).isEqualTo(context.resolutionContext().defenderPokemon().getId().toString());
        assertThat(payload.containsKey("flippingPlayerId")).isFalse();
    }

    private OpponentCoinTailsHandDiscardAttackEffect effect() {
        return new OpponentCoinTailsHandDiscardAttackEffect(
                gameRandomService,
                gameEventFactory,
                gameCardInstanceStateService,
                cardService);
    }

    private AttackEffectContext context(UUID gameId, UUID defenderUserId) {
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defenderPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                UUID.randomUUID(),
                defenderUserId,
                null,
                defenderPokemon,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "OPPONENT_COIN_TAILS_HAND_DISCARD",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                4,
                null);
    }

    private GameCardInstance handCard(int zonePosition) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setCardId(UUID.randomUUID());
        card.setZone(CardZone.HAND);
        card.setZonePosition(zonePosition);
        card.setFaceDown(true);
        return card;
    }

    private Card cardEntity() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId("xy1-1");
        card.setName("Test Card");
        card.setImageSmallUrl("small.png");
        card.setImageLargeUrl("large.png");
        return card;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
