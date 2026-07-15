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
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchPokemonFromDeckTrainerEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private CardService cardService;
    @Mock
    private GameRandomService gameRandomService;
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
    void supports_itemTrainerSuperBall_returnsTrue() {
        // xy1-118 = Super Ball: SEARCH_POKEMON_FROM_DECK
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-118");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_noPokemonInDeck_returnsZeroFound() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-118");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance energyInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        Card energyCard = cardWithSupertype(CardSupertype.ENERGY);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(new java.util.ArrayList<>(List.of(energyInDeck)));
        when(cardService.getCardEntityById(energyInDeck.getCardId())).thenReturn(energyCard);
        when(gameRandomService.shuffledCopy(any())).thenAnswer(inv -> inv.getArgument(0));
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("effectType", "SEARCH_POKEMON_FROM_DECK");
        assertThat(result.effectData()).containsEntry("cardsFound", 0);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);

        // No Pokemon was taken: opponent-visible event must not claim a card was taken,
        // but the deck-shuffle confirmation must still be signaled
        ArgumentCaptor<Map<String, Object>> publicPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), publicPayloadCaptor.capture());
        assertThat(publicPayloadCaptor.getValue()).containsEntry("cardsTaken", 0);
        assertThat(publicPayloadCaptor.getValue()).doesNotContainKey("takenCardId");
        assertThat(publicPayloadCaptor.getValue()).containsEntry("deckShuffled", true);
    }

    @Test
    void apply_pokemonFoundInDeck_movesToHand() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-118");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        Card pokemonCard = cardWithSupertype(CardSupertype.POKEMON);
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        // First call gathers the top-N look-at-deck cards; the second call happens inside
        // shuffleDeck() after the chosen Pokemon has already moved out of the DECK zone.
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(new java.util.ArrayList<>(List.of(pokemonInDeck)), new java.util.ArrayList<>());
        when(cardService.getCardEntityById(pokemonInDeck.getCardId())).thenReturn(pokemonCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND)).thenReturn(2);
        when(gameRandomService.shuffledCopy(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(pokemonInDeck.getZone()).isEqualTo(CardZone.HAND);
        assertThat(pokemonInDeck.getFaceDown()).isFalse();
        assertThat(pokemonInDeck.getZonePosition()).isEqualTo(2);
        assertThat(result.effectData()).containsEntry("cardsFound", 1);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);

        verify(gameCardInstanceStateService).save(pokemonInDeck);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);

        // The chosen Pokemon's identity must be revealed to the opponent (not just a count),
        // and the deck-shuffle confirmation must be signaled
        ArgumentCaptor<Map<String, Object>> publicPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), publicPayloadCaptor.capture());
        assertThat(publicPayloadCaptor.getValue()).containsEntry("takenCardId", pokemonInDeck.getCardId().toString());
        assertThat(publicPayloadCaptor.getValue()).containsEntry("cardsTaken", 1);
        assertThat(publicPayloadCaptor.getValue()).containsEntry("deckShuffled", true);
    }

    @Test
    void preview_returnsPokemonOptionsFromTopOfDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-118");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        Card pokemonCard = cardWithSupertype(CardSupertype.POKEMON);
        pokemonCard.setName("Pikachu");
        GameCardInstance energyInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 2);
        Card energyCard = cardWithSupertype(CardSupertype.ENERGY);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(new java.util.ArrayList<>(List.of(pokemonInDeck, energyInDeck)));
        when(cardService.getCardEntityById(pokemonInDeck.getCardId())).thenReturn(pokemonCard);
        when(cardService.getCardEntityById(energyInDeck.getCardId())).thenReturn(energyCard);

        Map<String, Object> preview = effect().preview(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(preview.get("cardsLookedAt")).isEqualTo(2);
        List<Map<String, Object>> pokemonOptions = (List<Map<String, Object>>) preview.get("pokemonOptions");
        assertThat(pokemonOptions).hasSize(1);
        assertThat(pokemonOptions.get(0)).containsEntry("cardInstanceId", pokemonInDeck.getId().toString());
        assertThat(pokemonOptions.get(0)).containsEntry("name", "Pikachu");
    }

    @Test
    void apply_emptyDeck_returnsZeroFound() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-118");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(new java.util.ArrayList<>());

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("cardsFound", 0);
        assertThat(result.emittedEvents()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private SearchPokemonFromDeckTrainerEffect effect() {
        return new SearchPokemonFromDeckTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                gameCardInstanceStateService,
                cardService,
                gameRandomService,
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

    private Card cardWithSupertype(CardSupertype supertype) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(supertype);
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

    private GameEventDto event(UUID gameId, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.CARD_DRAWN, 7,
                privateEvent, Instant.now(), Map.of());
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, 3, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6,
                Map.of("cardId", trainerCard.getId().toString()));
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
