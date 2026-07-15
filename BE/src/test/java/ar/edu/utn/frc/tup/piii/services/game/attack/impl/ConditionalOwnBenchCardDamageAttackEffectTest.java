package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

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
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalOwnBenchCardDamageAttackEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldAddDamageWhenExpectedCardIsOnOwnBench() {
        ConditionalOwnBenchCardDamageAttackEffect effect = effect();
        AttackEffectContext context = context();
        PokemonInPlay active = pokemon(0, null);
        UUID lunatoneCardId = UUID.randomUUID();
        PokemonInPlay bench = pokemon(1, lunatoneCardId);
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId()))
                .thenReturn(List.of(active, bench));
        when(cardService.getCardEntityById(lunatoneCardId)).thenReturn(card("Lunatone"));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context);

        assertThat(result.damageModifier()).isEqualTo(30);
    }

    @Test
    void shouldNotAddDamageWhenExpectedCardIsMissingFromOwnBench() {
        ConditionalOwnBenchCardDamageAttackEffect effect = effect();
        AttackEffectContext context = context();
        UUID otherCardId = UUID.randomUUID();
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId()))
                .thenReturn(List.of(pokemon(0, null), pokemon(1, otherCardId)));
        when(cardService.getCardEntityById(otherCardId)).thenReturn(card("Solrock"));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context);

        assertThat(result.damageModifier()).isZero();
    }

    private ConditionalOwnBenchCardDamageAttackEffect effect() {
        return new ConditionalOwnBenchCardDamageAttackEffect(
                pokemonInPlayStateService,
                cardService,
                gameEventFactory);
    }

    private AttackEffectContext context() {
        AttackEffectOperation operation = new AttackEffectOperation(
                "CONDITIONAL_OWN_BENCH_CARD_DAMAGE",
                AttackEffectPhase.BEFORE_DAMAGE,
                "DEFENDER",
                30,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                null,
                null,
                "Lunatone");
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                pokemon(0, null),
                null,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation, Map.of(), 1, 1, 0);
    }

    private PokemonInPlay pokemon(int slotPosition, UUID cardId) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setSlotPosition(slotPosition);
        if (cardId != null) {
            GameCardInstance activeCardInstance = new GameCardInstance();
            activeCardInstance.setCardId(cardId);
            pokemon.setActiveCardInstance(activeCardInstance);
        }
        return pokemon;
    }

    private Card card(String name) {
        Card card = new Card();
        card.setName(name);
        return card;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
