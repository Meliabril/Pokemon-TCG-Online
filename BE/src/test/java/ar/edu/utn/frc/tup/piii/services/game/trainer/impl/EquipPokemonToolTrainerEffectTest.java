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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
class EquipPokemonToolTrainerEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock
    private GameEventFactory gameEventFactory;

    @InjectMocks
    private EquipPokemonToolTrainerEffect effect;

    // ─── supports ────────────────────────────────────────────────────────────

    @Test
    void supports_nullCard_returnsFalse() {
        assertThat(effect.supports(null)).isFalse();
    }

    @Test
    void supports_itemTrainer_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER);
        assertThat(effect.supports(card)).isFalse();
    }

    @Test
    void supports_supporterTrainer_returnsFalse() {
        assertThat(effect.supports(card(CardCategory.SUPPORTER_TRAINER))).isFalse();
    }

    @Test
    void supports_pokemonToolTrainer_returnsTrue() {
        assertThat(effect.supports(card(CardCategory.POKEMON_TOOL_TRAINER))).isTrue();
    }

    // ─── keepsCardInPlay ──────────────────────────────────────────────────────

    @Test
    void keepsCardInPlay_returnsTrue() {
        assertThat(effect.keepsCardInPlay()).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_equipsTool_toTargetPokemon() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();

        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        trainerCard.setExternalId("xy1-119");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemon(targetPokemonInPlayId, actorUserId);
        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ENERGY_ATTACHED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(List.of());
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.ATTACHED)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ENERGY_ATTACHED), anyInt(), any()))
                .thenReturn(event);

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance));

        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(trainerInstance.getFaceDown()).isFalse();
        assertThat(result.effectData()).containsEntry("effectType", "EQUIP_POKEMON_TOOL");
        assertThat(result.effectData()).containsEntry("targetPokemonInPlayId", targetPokemonInPlayId.toString());
        assertThat(result.emittedEvents()).containsExactly(event);

        verify(gameCardInstanceStateService).save(trainerInstance);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);
        verify(pokemonAttachedCardStateService).save(any(PokemonAttachedCard.class));
    }

    @Test
    void apply_targetPokemonNotFound_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> effect.apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Target Pokemon was not found");
    }

    @Test
    void apply_pokemonAlreadyHasTool_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemon(targetPokemonInPlayId, actorUserId);

        PokemonAttachedCard existingTool = new PokemonAttachedCard();
        existingTool.setAttachedCardType(AttachedCardType.POKEMON_TOOL);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(List.of(existingTool));

        assertThatThrownBy(() -> effect.apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("already has a Tool card attached");
    }

    @Test
    void apply_missingPayloadKey_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6, Map.of());
        TrainerEffectContext ctx = new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);

        assertThatThrownBy(() -> effect.apply(ctx))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("targetPokemonInPlayId");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Card card(CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setExternalId("test-" + UUID.randomUUID());
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

    private PokemonInPlay pokemon(UUID id, UUID ownerUserId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setId(id);
        p.setOwnerUserId(ownerUserId);
        p.setSlotPosition(1);
        return p;
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

    @Test
    void apply_payloadWithUuidObject_succeeds() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        trainerCard.setExternalId("xy1-121");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemon(targetPokemonInPlayId, actorUserId);
        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ENERGY_ATTACHED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemonInPlayId)).thenReturn(List.of());
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.ATTACHED)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ENERGY_ATTACHED), anyInt(), any()))
                .thenReturn(event);

        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("targetPokemonInPlayId", targetPokemonInPlayId));
        TrainerEffectContext ctx = new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);

        TrainerEffectResult result = effect.apply(ctx);
        assertThat(result.effectData()).containsEntry("effectType", "EQUIP_POKEMON_TOOL");
    }

    @Test
    void apply_invalidUuidStringInPayload_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("targetPokemonInPlayId", "not-a-valid-uuid"));
        TrainerEffectContext ctx = new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);

        assertThatThrownBy(() -> effect.apply(ctx))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("targetPokemonInPlayId");
    }

    @Test
    void apply_effectDataContainsCardExternalId() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID targetPokemonInPlayId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.POKEMON_TOOL_TRAINER);
        trainerCard.setExternalId("xy1-121");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);
        PokemonInPlay targetPokemon = pokemon(targetPokemonInPlayId, actorUserId);
        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ENERGY_ATTACHED, 7,
                false, Instant.now(), Map.of());

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemonInPlayId)).thenReturn(List.of());
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.ATTACHED)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ENERGY_ATTACHED), anyInt(), any()))
                .thenReturn(event);

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("cardExternalId", "xy1-121");
        assertThat(result.effectData()).containsEntry("targetPokemonInPlayId", targetPokemonInPlayId.toString());
    }
}
