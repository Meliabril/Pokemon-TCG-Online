package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;




import ar.edu.utn.frc.tup.piii.services.game.attack.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.board.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.query.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.state.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.impl.*;
import ar.edu.utn.frc.tup.piii.services.game.attack.*;
import ar.edu.utn.frc.tup.piii.services.game.board.*;
import ar.edu.utn.frc.tup.piii.services.game.energy.*;
import ar.edu.utn.frc.tup.piii.services.game.engine.*;
import ar.edu.utn.frc.tup.piii.services.game.evolution.*;
import ar.edu.utn.frc.tup.piii.services.game.outcome.*;
import ar.edu.utn.frc.tup.piii.services.game.presence.*;
import ar.edu.utn.frc.tup.piii.services.game.query.*;
import ar.edu.utn.frc.tup.piii.services.game.retreat.*;
import ar.edu.utn.frc.tup.piii.services.game.setup.*;
import ar.edu.utn.frc.tup.piii.services.game.state.*;
import ar.edu.utn.frc.tup.piii.services.game.trainer.*;
import ar.edu.utn.frc.tup.piii.services.game.turn.*;
import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealDamageTrainerEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void shouldSupportItemOrSupporterWithHealDamageMetadata() {
        HealDamageTrainerEffect effect = effect();

        assertThat(effect.supports(card(CardCategory.ITEM_TRAINER, healRawJson(30)))).isTrue();
        assertThat(effect.supports(card(CardCategory.SUPPORTER_TRAINER, healRawJson(30)))).isTrue();
        assertThat(effect.supports(card(CardCategory.ITEM_TRAINER, "{}"))).isFalse();
        assertThat(effect.supports(card(CardCategory.BASIC_POKEMON, healRawJson(30)))).isFalse();
    }

    @Test
    void shouldHealConfiguredDamageFromOwnPokemon() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonInPlayId);
        targetPokemon.setOwnerUserId(actorUserId);
        targetPokemon.setDamageCounters(5);
        HealDamageTrainerEffect effect = effect();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, targetPokemonInPlayId, healRawJson(30)));

        assertThat(targetPokemon.getDamageCounters()).isEqualTo(2);
        assertThat(result.effectData()).containsEntry("effectType", "HEAL_DAMAGE");
        assertThat(result.effectData()).containsEntry("healedDamage", 30);
        assertThat(result.effectData()).containsEntry("remainingDamageCounters", 2);
        assertThat(result.emittedEvents()).isEmpty();
        verify(pokemonInPlayStateService).save(targetPokemon);
    }

    @Test
    void shouldRejectMissingTargetPokemon() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        HealDamageTrainerEffect effect = effect();

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(applyCall(effect, context(gameId, actorUserId, targetPokemonInPlayId, healRawJson(30))))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessage("Target Pokemon was not found for this Trainer effect");
    }

    private HealDamageTrainerEffect effect() {
        return new HealDamageTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                pokemonInPlayStateService);
    }

    private ThrowingCallable applyCall(HealDamageTrainerEffect effect, TrainerEffectContext context) {
        return new ThrowingCallable() {
            @Override
            public void call() {
                effect.apply(context);
            }
        };
    }

    private TrainerEffectContext context(
            UUID gameId,
            UUID actorUserId,
            UUID targetPokemonInPlayId,
            String rawJson) {
        Card trainerCard = card(CardCategory.ITEM_TRAINER, rawJson);
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                3,
                6,
                actorUserId,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                6,
                Map.<String, Object>of(
                        "cardId", trainerCard.getId().toString(),
                        "targetPokemonInPlayId", targetPokemonInPlayId.toString()));
        GameCardInstance trainerInstance = new GameCardInstance();
        trainerInstance.setId(UUID.randomUUID());
        trainerInstance.setOwnerUserId(actorUserId);
        trainerInstance.setCardId(trainerCard.getId());
        trainerInstance.setZone(CardZone.HAND);
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }

    private Card card(CardCategory category, String rawJson) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId("test-card-" + card.getId());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setRawJson(rawJson);
        return card;
    }

    private String healRawJson(int amount) {
        return "{\"engineEffect\":{\"type\":\"HEAL_DAMAGE\",\"amount\":" + amount + "}}";
    }
}
