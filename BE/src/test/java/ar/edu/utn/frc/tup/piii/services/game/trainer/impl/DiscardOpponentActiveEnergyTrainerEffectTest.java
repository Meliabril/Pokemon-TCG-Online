package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscardOpponentActiveEnergyTrainerEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private GameEventFactory gameEventFactory;

    // ─── supports ────────────────────────────────────────────────────────────

    @Test
    void supports_nullCard_returnsFalse() {
        assertThat(effect().supports(null)).isFalse();
    }

    @Test
    void supports_pokemonCard_returnsFalse() {
        assertThat(effect().supports(card(CardCategory.BASIC_POKEMON, null))).isFalse();
    }

    @Test
    void supports_itemTrainerWrongType_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-999");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_supporterTrainerTeamFlareGrunt_returnsTrue() {
        // xy1-129 = Team Flare Grunt: DISCARD_OPPONENT_ACTIVE_ENERGY
        Card card = card(CardCategory.SUPPORTER_TRAINER, "xy1-129");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_discardsOpponentActiveEnergy() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();

        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-129");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());

        GameCardInstance energyCardInstance = instance(opponentUserId, UUID.randomUUID(), CardZone.ATTACHED, 1);
        PokemonAttachedCard attachedEnergy = attachedCard(AttachedCardType.BASIC_ENERGY, energyCardInstance);
        GameEventDto publicEvent = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.TRAINER_PLAYED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentUserId))
                .thenReturn(Optional.of(activePokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId()))
                .thenReturn(List.of(attachedEnergy));
        when(gameCardInstanceStateService.nextZonePosition(gameId, opponentUserId, CardZone.DISCARD)).thenReturn(3);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), anyInt(), any()))
                .thenReturn(publicEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, opponentUserId, trainerCard, trainerInstance));

        assertThat(energyCardInstance.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(energyCardInstance.getFaceDown()).isFalse();
        assertThat(energyCardInstance.getZonePosition()).isEqualTo(3);
        assertThat(result.effectData()).containsEntry("effectType", "DISCARD_OPPONENT_ACTIVE_ENERGY");
        assertThat(result.emittedEvents()).containsExactly(publicEvent);

        verify(pokemonAttachedCardStateService).delete(attachedEnergy);
        verify(gameCardInstanceStateService).save(energyCardInstance);
        verify(gameCardInstanceStateService).resequenceZone(gameId, opponentUserId, CardZone.ATTACHED);
    }

    @Test
    void apply_opponentHasNoActivePokemon_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-129");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> effect().apply(context(gameId, actorUserId, opponentUserId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("no Active Pokemon");
    }

    @Test
    void apply_opponentActivePokemonHasNoEnergy_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-129");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());

        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentUserId))
                .thenReturn(Optional.of(activePokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> effect().apply(context(gameId, actorUserId, opponentUserId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("no attached Energy");
    }

    @Test
    void apply_specialEnergyDiscarded() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();

        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-129");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        PokemonInPlay activePokemon = new PokemonInPlay();
        activePokemon.setId(UUID.randomUUID());

        GameCardInstance specialEnergyInstance = instance(opponentUserId, UUID.randomUUID(), CardZone.ATTACHED, 1);
        PokemonAttachedCard specialEnergy = attachedCard(AttachedCardType.SPECIAL_ENERGY, specialEnergyInstance);
        GameEventDto publicEvent = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.TRAINER_PLAYED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findActivePokemon(gameId, opponentUserId))
                .thenReturn(Optional.of(activePokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId()))
                .thenReturn(List.of(specialEnergy));
        when(gameCardInstanceStateService.nextZonePosition(gameId, opponentUserId, CardZone.DISCARD)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.TRAINER_PLAYED), anyInt(), any()))
                .thenReturn(publicEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, opponentUserId, trainerCard, trainerInstance));

        assertThat(specialEnergyInstance.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(result.emittedEvents()).containsExactly(publicEvent);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private DiscardOpponentActiveEnergyTrainerEffect effect() {
        return new DiscardOpponentActiveEnergyTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                gameCardInstanceStateService,
                gameEventFactory);
    }

    private Card card(CardCategory category, String externalId) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setExternalId(externalId);
        return card;
    }

    private GameCardInstance instance(UUID ownerUserId, UUID cardId, CardZone zone, int position) {
        GameCardInstance inst = new GameCardInstance();
        inst.setId(UUID.randomUUID());
        inst.setOwnerUserId(ownerUserId);
        inst.setCardId(cardId);
        inst.setZone(zone);
        inst.setZonePosition(position);
        inst.setFaceDown(false);
        return inst;
    }

    private PokemonAttachedCard attachedCard(AttachedCardType type, GameCardInstance cardInstance) {
        PokemonAttachedCard attached = new PokemonAttachedCard();
        attached.setId(UUID.randomUUID());
        attached.setAttachedCardType(type);
        attached.setGameCardInstance(cardInstance);
        return attached;
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, UUID opponentUserId,
                                         Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(actorUserId, opponentUserId),
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("cardId", trainerCard.getId().toString()));
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
