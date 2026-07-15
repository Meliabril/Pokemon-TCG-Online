package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
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
class DrawCardsAttackEffectTest {

    @Mock
    private DrawCardsEffectService drawCardsEffectService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldDrawConfiguredAmountFromDeck() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance firstDeckCard = cardInstance(attackerUserId, UUID.randomUUID(), CardZone.HAND, 4);
        GameCardInstance secondDeckCard = cardInstance(attackerUserId, UUID.randomUUID(), CardZone.HAND, 5);
        GameEventDto publicDraw = event(gameId, GameEventType.CARD_DRAWN, false);
        GameEventDto privateDraw = event(gameId, GameEventType.CARD_DRAWN, true);
        GameEventDto resolved = event(gameId, GameEventType.ATTACK_EFFECT_RESOLVED, false);
        DrawCardsAttackEffect effect = new DrawCardsAttackEffect(drawCardsEffectService, gameEventFactory);

        when(drawCardsEffectService.drawCards(gameId, attackerUserId, 2))
                .thenReturn(List.of(firstDeckCard, secondDeckCard));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(7), anyMap())).thenReturn(publicDraw);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), eq(7), anyMap(), eq(attackerUserId))).thenReturn(privateDraw);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(7), anyMap())).thenReturn(resolved);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).containsExactly(publicDraw, privateDraw, resolved);
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
                "DRAW_CARDS",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                2,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 7, 1, 0);
    }

    private GameCardInstance cardInstance(UUID ownerUserId, UUID cardId, CardZone zone, int position) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(position);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId, GameEventType eventType, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, eventType, 7, privateEvent, Instant.now(), Map.of());
    }
}
