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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LookOpponentDeckTopCardAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldRevealTopDeckCardPrivatelyAndRequireChoice() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        GameCardInstance topCard = cardInstance(defenderUserId);
        GameEventDto privateEvent = event(gameId, true);
        GameEventDto publicEvent = event(gameId, false);

        LookOpponentDeckTopCardAttackEffect effect =
                new LookOpponentDeckTopCardAttackEffect(gameCardInstanceStateService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of(topCard));
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap(), eq(attackerUserId)))
                .thenReturn(privateEvent);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), anyMap()))
                .thenReturn(publicEvent);

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, defenderUserId));

        assertThat(result.choiceRequired()).isTrue();
        assertThat(result.choiceType()).isEqualTo(LookOpponentDeckTopCardAttackEffect.CHOICE_TYPE);
        assertThat(result.events()).containsExactly(privateEvent, publicEvent);
    }

    @Test
    void shouldDoNothingWhenOpponentDeckIsEmpty() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        UUID defenderUserId = UUID.randomUUID();
        LookOpponentDeckTopCardAttackEffect effect =
                new LookOpponentDeckTopCardAttackEffect(gameCardInstanceStateService, gameEventFactory);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, defenderUserId, CardZone.DECK))
                .thenReturn(List.of());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId, defenderUserId));

        assertThat(result.choiceRequired()).isFalse();
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
                "LOOK_OPPONENT_DECK_TOP_CARD_OPTIONAL_SHUFFLE",
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

    private GameCardInstance cardInstance(UUID ownerUserId) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(UUID.randomUUID());
        cardInstance.setZone(CardZone.DECK);
        cardInstance.setZonePosition(1);
        cardInstance.setFaceDown(true);
        return cardInstance;
    }

    private GameEventDto event(UUID gameId, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, privateEvent, Instant.now(), Map.of());
    }
}
