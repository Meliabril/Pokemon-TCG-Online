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
class SearchBasicEnergiesFromDeckTrainerEffectTest {

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
    void supports_stadiumCard_returnsFalse() {
        assertThat(effect().supports(card(CardCategory.STADIUM_TRAINER, null))).isFalse();
    }

    @Test
    void supports_itemTrainerWrongType_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-999");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_zeroAmount_returnsFalse() {
        Card card = card(CardCategory.ITEM_TRAINER, null);
        card.setRawJson("{\"engineEffect\":{\"type\":\"SEARCH_BASIC_ENERGIES_FROM_DECK\",\"amount\":0}}");
        assertThat(effect().supports(card)).isFalse();
    }

    @Test
    void supports_itemTrainerProfessorsLetter_returnsTrue() {
        // xy1-123 = Professor's Letter: SEARCH_BASIC_ENERGIES_FROM_DECK, amount=2
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-123");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_noEnergiesInDeck_returnsZeroFound() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-123"); // amount=2
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance pokemonInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        Card pokemonCard = new Card();
        pokemonCard.setSupertype(CardSupertype.POKEMON);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(pokemonInDeck));
        when(cardService.getCardEntityById(pokemonInDeck.getCardId())).thenReturn(pokemonCard);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("cardsFound", 0);
        assertThat(result.emittedEvents()).isEmpty();
    }

    @Test
    void apply_oneBasicEnergyInDeck_movesToHand() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-123"); // amount=2
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance energyInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        Card energyCard = basicEnergyCard();
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(energyInDeck));
        when(cardService.getCardEntityById(energyInDeck.getCardId())).thenReturn(energyCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND)).thenReturn(2);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(energyInDeck.getZone()).isEqualTo(CardZone.HAND);
        assertThat(energyInDeck.getFaceDown()).isFalse();
        assertThat(result.effectData()).containsEntry("cardsFound", 1);
        assertThat(result.emittedEvents()).containsExactly(publicEvent, privateEvent);

        verify(gameCardInstanceStateService).saveAll(List.of(energyInDeck));
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);
    }

    @Test
    void apply_moreEnergiesThanMaxAmount_takesOnlyMax() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-123"); // amount=2
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance energy1 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        GameCardInstance energy2 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 2);
        GameCardInstance energy3 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 3);
        Card energyCard = basicEnergyCard();
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(energy1, energy2, energy3));
        when(cardService.getCardEntityById(any())).thenReturn(energyCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        TrainerEffectResult result = effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(result.effectData()).containsEntry("cardsFound", 2);
        assertThat(energy1.getZone()).isEqualTo(CardZone.HAND);
        assertThat(energy2.getZone()).isEqualTo(CardZone.HAND);
        assertThat(energy3.getZone()).isEqualTo(CardZone.DECK); // not taken (max reached)
    }

    @Test
    void apply_revealsEnergyTypesAndCountsToOpponent_notJustATotal() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-123"); // amount=2
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance fireEnergyInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        GameCardInstance waterEnergyInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 2);
        Card fireEnergy = basicEnergyCard();
        fireEnergy.setName("Fire Energy");
        Card waterEnergy = basicEnergyCard();
        waterEnergy.setName("Water Energy");
        GameEventDto publicEvent = event(gameId, false);
        GameEventDto privateEvent = event(gameId, true);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(fireEnergyInDeck, waterEnergyInDeck));
        when(cardService.getCardEntityById(fireEnergyInDeck.getCardId())).thenReturn(fireEnergy);
        when(cardService.getCardEntityById(waterEnergyInDeck.getCardId())).thenReturn(waterEnergy);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.HAND)).thenReturn(1);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any()))
                .thenReturn(publicEvent);
        when(gameEventFactory.privateEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), any(), eq(actorUserId)))
                .thenReturn(privateEvent);

        effect().apply(context(gameId, actorUserId, trainerCard, trainerInstance));

        ArgumentCaptor<Map<String, Object>> publicPayloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(gameEventFactory).publicEvent(eq(gameId), eq(GameEventType.CARD_DRAWN), anyInt(), publicPayloadCaptor.capture());
        List<Map<String, Object>> revealedEnergies = (List<Map<String, Object>>) publicPayloadCaptor.getValue().get("revealedEnergies");
        assertThat(revealedEnergies).hasSize(2);
        assertThat(revealedEnergies)
                .anySatisfy(entry -> {
                    assertThat(entry.get("name")).isEqualTo("Fire Energy");
                    assertThat(entry.get("count")).isEqualTo(1);
                })
                .anySatisfy(entry -> {
                    assertThat(entry.get("name")).isEqualTo("Water Energy");
                    assertThat(entry.get("count")).isEqualTo(1);
                });
    }

    @Test
    void preview_returnsGroupedBasicEnergyOptionsWithCounts() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-123"); // amount=2
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        GameCardInstance fireEnergy1 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 1);
        GameCardInstance fireEnergy2 = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 2);
        Card fireEnergy = basicEnergyCard();
        fireEnergy.setName("Fire Energy");
        GameCardInstance pokemonInDeck = instance(actorUserId, UUID.randomUUID(), CardZone.DECK, 3);
        Card pokemonCard = new Card();
        pokemonCard.setSupertype(CardSupertype.POKEMON);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(fireEnergy1, fireEnergy2, pokemonInDeck));
        when(cardService.getCardEntityById(fireEnergy1.getCardId())).thenReturn(fireEnergy);
        when(cardService.getCardEntityById(fireEnergy2.getCardId())).thenReturn(fireEnergy);
        when(cardService.getCardEntityById(pokemonInDeck.getCardId())).thenReturn(pokemonCard);

        Map<String, Object> preview = effect().preview(context(gameId, actorUserId, trainerCard, trainerInstance));

        assertThat(preview.get("maxSelectable")).isEqualTo(2);
        List<Map<String, Object>> options = (List<Map<String, Object>>) preview.get("options");
        assertThat(options).hasSize(1);
        assertThat(options.get(0)).containsEntry("name", "Fire Energy");
        assertThat(options.get(0)).containsEntry("availableInDeck", 2);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private SearchBasicEnergiesFromDeckTrainerEffect effect() {
        return new SearchBasicEnergiesFromDeckTrainerEffect(
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

    private Card basicEnergyCard() {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.ENERGY);
        card.setSubtype("Basic");
        card.setName("Basic Energy");
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
