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
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
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
class ReturnBenchedPokemonToHandTrainerEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    @Mock
    private SpecialConditionStateService specialConditionStateService;
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
    void supports_itemTrainer_returnsFalse() {
        assertThat(effect().supports(card(CardCategory.ITEM_TRAINER, null))).isFalse();
    }

    @Test
    void supports_supporterWrongType_returnsFalse() {
        Card card = card(CardCategory.SUPPORTER_TRAINER, "xy1-999");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_supporterTrainerCassius_returnsTrue() {
        // xy1-115 = Cassius: RETURN_BENCHED_POKEMON_TO_HAND
        Card card = card(CardCategory.SUPPORTER_TRAINER, "xy1-115");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_returnsBenchedPokemonWithAttachmentsToHand() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();

        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-115");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonCardInstance = instance(actorUserId, UUID.randomUUID(), CardZone.BENCH, 1);
        PokemonInPlay benchPokemon = pokemon(targetPokemonInPlayId, actorUserId, 1, pokemonCardInstance);

        GameCardInstance evolvedCardInstance = instance(actorUserId, UUID.randomUUID(), CardZone.BENCH, 2);
        PokemonEvolutionStack stackEntry = evolutionStack(benchPokemon, evolvedCardInstance);

        GameCardInstance energyCardInstance = instance(actorUserId, UUID.randomUUID(), CardZone.ATTACHED, 1);
        PokemonAttachedCard attachedCard = attachedCard(AttachedCardType.BASIC_ENERGY, energyCardInstance);

        GameEventDto publicEvent = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.TRAINER_PLAYED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(benchPokemon));
        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(List.of(stackEntry));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(List.of(attachedCard));
        org.mockito.Mockito.lenient().when(gameCardInstanceStateService.nextZonePosition(eq(gameId), eq(actorUserId), eq(CardZone.DECK)))
                .thenReturn(2, 3, 4);
        org.mockito.Mockito.lenient().when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(eq(gameId), eq(actorUserId), eq(CardZone.DECK)))
                .thenReturn(new java.util.ArrayList<>(List.of(evolvedCardInstance, energyCardInstance, pokemonCardInstance)));

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance));

        assertThat(evolvedCardInstance.getZone()).isEqualTo(CardZone.DECK);
        assertThat(evolvedCardInstance.getFaceDown()).isTrue();
        assertThat(energyCardInstance.getZone()).isEqualTo(CardZone.DECK);
        assertThat(pokemonCardInstance.getZone()).isEqualTo(CardZone.DECK);

        assertThat(result.effectData()).containsEntry("effectType", "RETURN_BENCHED_POKEMON_TO_HAND");
        assertThat(result.emittedEvents()).isEmpty();

        verify(pokemonEvolutionStackStateService).delete(stackEntry);
        verify(pokemonAttachedCardStateService).delete(attachedCard);
        verify(specialConditionStateService).deleteByPokemonInPlayId(targetPokemonInPlayId);
        verify(pokemonInPlayStateService).delete(benchPokemon);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.ATTACHED);
    }

    @Test
    void apply_targetPokemonNotFound_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-115");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> effect().apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void apply_targetIsActivePokemon_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.SUPPORTER_TRAINER, "xy1-115");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonCardInstance = instance(actorUserId, UUID.randomUUID(), CardZone.ACTIVE, 0);
        PokemonInPlay activePokemon = pokemon(targetPokemonInPlayId, actorUserId, 0, pokemonCardInstance);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(activePokemon));

        assertThatThrownBy(() -> effect().apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Benched");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ReturnBenchedPokemonToHandTrainerEffect effect() {
        return new ReturnBenchedPokemonToHandTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                pokemonInPlayStateService,
                pokemonAttachedCardStateService,
                pokemonEvolutionStackStateService,
                specialConditionStateService,
                gameCardInstanceStateService);
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
        inst.setFaceDown(zone == CardZone.DECK);
        return inst;
    }

    private PokemonInPlay pokemon(UUID id, UUID ownerUserId, int slotPosition, GameCardInstance activeCard) {
        PokemonInPlay p = new PokemonInPlay();
        p.setId(id);
        p.setOwnerUserId(ownerUserId);
        p.setSlotPosition(slotPosition);
        p.setActiveCardInstance(activeCard);
        return p;
    }

    private PokemonEvolutionStack evolutionStack(PokemonInPlay pokemon, GameCardInstance cardInstance) {
        PokemonEvolutionStack stack = new PokemonEvolutionStack();
        stack.setId(UUID.randomUUID());
        stack.setPokemonInPlay(pokemon);
        stack.setGameCardInstance(cardInstance);
        stack.setStackOrder(0);
        return stack;
    }

    private PokemonAttachedCard attachedCard(AttachedCardType type, GameCardInstance cardInstance) {
        PokemonAttachedCard attached = new PokemonAttachedCard();
        attached.setId(UUID.randomUUID());
        attached.setAttachedCardType(type);
        attached.setGameCardInstance(cardInstance);
        return attached;
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, UUID targetPokemonInPlayId,
                                         Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("targetPokemonInPlayId", targetPokemonInPlayId.toString()));
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
