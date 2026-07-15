package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
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
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
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
class RevivePokemonFromDiscardTrainerEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private CardService cardService;
    @Mock
    private GameEventFactory gameEventFactory;

    // ─── supports ────────────────────────────────────────────────────────────

    @Test
    void supports_nullCard_returnsFalse() {
        assertThat(effect().supports(null)).isFalse();
    }

    @Test
    void supports_stadiumCard_returnsFalse() {
        assertThat(effect().supports(card(CardCategory.STADIUM_TRAINER, null))).isFalse();
    }

    @Test
    void supports_itemTrainerWrongType_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-999");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_itemTrainerMaxRevive_returnsTrue() {
        // xy1-120 = Max Revive: REVIVE_POKEMON_FROM_DISCARD
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-120");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_revivesAnyStagePokemon_placesItFaceDownOnTopOfDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID pokemonInstanceId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-120");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonInstance = instance(actorUserId, UUID.randomUUID(), CardZone.DISCARD, 1);
        pokemonInstance.setId(pokemonInstanceId);
        // Max Revive must NOT be limited to Basic Pokemon: a Stage 2 Pokemon is a valid target.
        Card stage2Card = pokemonCard("Stage 2");

        GameCardInstance deckCard1 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        GameCardInstance deckCard2 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 2);

        GameEventDto publicEvent = event(gameId);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(pokemonInstanceId, gameId, actorUserId))
                .thenReturn(Optional.of(pokemonInstance));
        when(cardService.getCardEntityById(pokemonInstance.getCardId())).thenReturn(stage2Card);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard1, deckCard2));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_PLAYED), anyInt(), any()))
                .thenReturn(publicEvent);

        TrainerEffectResult result = effect().apply(
                context(gameId, actorUserId, pokemonInstanceId, trainerCard, trainerInstance));

        assertThat(pokemonInstance.getZone()).isEqualTo(CardZone.DECK);
        assertThat(pokemonInstance.getZonePosition()).isEqualTo(1);
        assertThat(pokemonInstance.getFaceDown()).isTrue();
        assertThat(deckCard1.getZonePosition()).isEqualTo(2);
        assertThat(deckCard2.getZonePosition()).isEqualTo(3);
        assertThat(result.effectData()).containsEntry("effectType", "REVIVE_POKEMON_FROM_DISCARD");
        assertThat(result.emittedEvents()).containsExactly(publicEvent);

        verify(gameCardInstanceStateService).save(pokemonInstance);
        verify(gameCardInstanceStateService).saveAll(List.of(deckCard1, deckCard2));
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.DISCARD);
    }

    @Test
    void apply_pokemonNotFound_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID pokemonInstanceId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-120");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(pokemonInstanceId, gameId, actorUserId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, pokemonInstanceId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not found in your discard pile");
    }

    @Test
    void apply_pokemonNotInDiscardZone_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID pokemonInstanceId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-120");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonInHand = instance(actorUserId, UUID.randomUUID(), CardZone.HAND, 3);
        pokemonInHand.setId(pokemonInstanceId);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(pokemonInstanceId, gameId, actorUserId))
                .thenReturn(Optional.of(pokemonInHand));

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, pokemonInstanceId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("must be in the discard pile");
    }

    @Test
    void apply_cardIsNotAPokemon_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID pokemonInstanceId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-120");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance instanceInDiscard = instance(actorUserId, UUID.randomUUID(), CardZone.DISCARD, 1);
        instanceInDiscard.setId(pokemonInstanceId);
        Card energyCard = new Card();
        energyCard.setSupertype(CardSupertype.ENERGY);

        when(gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(pokemonInstanceId, gameId, actorUserId))
                .thenReturn(Optional.of(instanceInDiscard));
        when(cardService.getCardEntityById(instanceInDiscard.getCardId())).thenReturn(energyCard);

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, pokemonInstanceId, trainerCard, trainerInstance)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("Only Pokemon cards");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private RevivePokemonFromDiscardTrainerEffect effect() {
        return new RevivePokemonFromDiscardTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                gameCardInstanceStateService,
                cardService,
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

    private Card pokemonCard(String subtype) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.POKEMON);
        card.setSubtype(subtype);
        return card;
    }

    private GameCardInstance instance(UUID ownerUserId, UUID cardId, CardZone zone, int position) {
        GameCardInstance inst = new GameCardInstance();
        inst.setId(UUID.randomUUID());
        inst.setOwnerUserId(ownerUserId);
        inst.setCardId(cardId);
        inst.setZone(zone);
        inst.setZonePosition(position);
        inst.setFaceDown(zone == CardZone.DECK || zone == CardZone.DISCARD);
        return inst;
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.CARD_PLAYED, 7,
                false, Instant.now(), Map.of());
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, UUID pokemonInstanceId,
                                          Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("targetCardInstanceId", pokemonInstanceId.toString()));
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
