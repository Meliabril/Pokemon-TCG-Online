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
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
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
class SearchEvolutionFromDeckTrainerEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;
    @Mock
    private SpecialConditionStateService specialConditionStateService;
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
    void supports_itemTrainerEvosoda_returnsTrue() {
        // xy1-116 = Evosoda: SEARCH_EVOLUTION_FROM_DECK
        Card card = card(CardCategory.ITEM_TRAINER, "xy1-116");
        assertThat(effect().supports(card)).isTrue();
    }

    // ─── apply: turn-based validations ─────────────────────────────────────────

    @Test
    void apply_firstTurn_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, UUID.randomUUID(), trainerCard, trainerInstance, 1)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("first turn");
    }

    @Test
    void apply_targetEnteredPlayThisTurn_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID targetPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonInPlayId);
        targetPokemon.setEnteredPlayTurn(3);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance, 3)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("same turn");
    }

    // ─── apply: evolution resolution ───────────────────────────────────────────

    @Test
    void apply_validEvolutionInDeck_evolvesTargetAndShufflesDeck() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID targetPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonInPlayId);
        targetPokemon.setSlotPosition(0); // active
        targetPokemon.setEnteredPlayTurn(1);

        Card topCard = pokemonCard("Charmander", CardCategory.BASIC_POKEMON, null);
        GameCardInstance topInstance = instance(actorUserId, topCard.getId(), CardZone.ACTIVE, 0);
        PokemonEvolutionStack topStack = new PokemonEvolutionStack();
        topStack.setGameCardInstance(topInstance);

        Card evolutionCard = pokemonCard("Charmeleon", CardCategory.STAGE_1_POKEMON, "Charmander");
        GameCardInstance evolutionInDeck = instance(actorUserId, evolutionCard.getId(), CardZone.DECK, 1);

        GameEventDto publicEvent = event(gameId, GameEventType.POKEMON_EVOLVED);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(Optional.of(topStack));
        when(cardService.getCardEntityById(topCard.getId())).thenReturn(topCard);
        // First call gathers deck candidates; the second call happens inside the deck-shuffle
        // step after the chosen evolution card has already moved out of the DECK zone.
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(evolutionInDeck), List.of());
        when(cardService.getCardEntityById(evolutionCard.getId())).thenReturn(evolutionCard);
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.EVOLUTION_STACK)).thenReturn(1);
        when(pokemonEvolutionStackStateService.nextStackOrder(targetPokemonInPlayId)).thenReturn(1);
        when(gameRandomService.shuffledCopy(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.POKEMON_EVOLVED), anyInt(), any()))
                .thenReturn(publicEvent);

        Map<String, Object> payload = Map.of(
                "targetPokemonInPlayId", targetPokemonInPlayId.toString(),
                "selectedEvolutionExternalId", evolutionCard.getExternalId());
        TrainerEffectResult result = effect().apply(
                contextWithPayload(gameId, actorUserId, trainerCard, trainerInstance, 3, payload));

        assertThat(evolutionInDeck.getZone()).isEqualTo(CardZone.ACTIVE);
        assertThat(evolutionInDeck.getFaceDown()).isFalse();
        assertThat(topInstance.getZone()).isEqualTo(CardZone.EVOLUTION_STACK);
        assertThat(result.effectData()).containsEntry("effectType", "SEARCH_EVOLUTION_FROM_DECK");
        assertThat(result.emittedEvents()).containsExactly(publicEvent);

        verify(specialConditionStateService).deleteByPokemonInPlayId(targetPokemonInPlayId);
        verify(pokemonInPlayStateService).save(targetPokemon);
        verify(pokemonEvolutionStackStateService).save(any(PokemonEvolutionStack.class));
        verify(gameCardInstanceStateService).saveAll(any());
    }

    @Test
    void apply_noValidEvolutionInDeck_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID targetPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonInPlayId);
        targetPokemon.setSlotPosition(0);
        targetPokemon.setEnteredPlayTurn(1);

        Card topCard = pokemonCard("Charmander", CardCategory.BASIC_POKEMON, null);
        GameCardInstance topInstance = instance(actorUserId, topCard.getId(), CardZone.ACTIVE, 0);
        PokemonEvolutionStack topStack = new PokemonEvolutionStack();
        topStack.setGameCardInstance(topInstance);

        Card unrelatedCard = pokemonCard("Squirtle", CardCategory.BASIC_POKEMON, null);
        GameCardInstance unrelatedInDeck = instance(actorUserId, unrelatedCard.getId(), CardZone.DECK, 1);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(Optional.of(topStack));
        when(cardService.getCardEntityById(topCard.getId())).thenReturn(topCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(unrelatedInDeck));
        when(cardService.getCardEntityById(unrelatedCard.getId())).thenReturn(unrelatedCard);

        assertThatThrownBy(() -> effect().apply(
                context(gameId, actorUserId, targetPokemonInPlayId, trainerCard, trainerInstance, 3)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("No valid evolution");
    }

    @Test
    void apply_explicitSelectionNotAValidEvolution_throwsException() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID targetPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay targetPokemon = new PokemonInPlay();
        targetPokemon.setId(targetPokemonInPlayId);
        targetPokemon.setSlotPosition(0);
        targetPokemon.setEnteredPlayTurn(1);

        Card topCard = pokemonCard("Charmander", CardCategory.BASIC_POKEMON, null);
        GameCardInstance topInstance = instance(actorUserId, topCard.getId(), CardZone.ACTIVE, 0);
        PokemonEvolutionStack topStack = new PokemonEvolutionStack();
        topStack.setGameCardInstance(topInstance);

        Card unrelatedCard = pokemonCard("Squirtle", CardCategory.BASIC_POKEMON, null);
        GameCardInstance unrelatedInDeck = instance(actorUserId, unrelatedCard.getId(), CardZone.DECK, 1);

        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(targetPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(targetPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(targetPokemonInPlayId))
                .thenReturn(Optional.of(topStack));
        when(cardService.getCardEntityById(topCard.getId())).thenReturn(topCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(unrelatedInDeck));
        when(cardService.getCardEntityById(unrelatedCard.getId())).thenReturn(unrelatedCard);

        Map<String, Object> payload = Map.of(
                "targetPokemonInPlayId", targetPokemonInPlayId.toString(),
                "selectedCardInstanceId", unrelatedInDeck.getId().toString());

        assertThatThrownBy(() -> effect().apply(
                contextWithPayload(gameId, actorUserId, trainerCard, trainerInstance, 3, payload)))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("not a valid evolution");
    }

    // ─── preview ────────────────────────────────────────────────────────────────

    @Test
    void preview_firstTurn_returnsNoOptions() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        Map<String, Object> preview = effect().preview(
                context(gameId, actorUserId, UUID.randomUUID(), trainerCard, trainerInstance, 1));

        assertThat((List<?>) preview.get("options")).isEmpty();
    }

    @Test
    void preview_returnsGroupedOptionsWithValidTargets() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay ownPokemon = new PokemonInPlay();
        ownPokemon.setId(pokemonInPlayId);
        ownPokemon.setSlotPosition(0);
        ownPokemon.setEnteredPlayTurn(1);

        Card topCard = pokemonCard("Charmander", CardCategory.BASIC_POKEMON, null);
        GameCardInstance topInstance = instance(actorUserId, topCard.getId(), CardZone.ACTIVE, 0);
        PokemonEvolutionStack topStack = new PokemonEvolutionStack();
        topStack.setGameCardInstance(topInstance);

        Card evolutionCard = pokemonCard("Charmeleon", CardCategory.STAGE_1_POKEMON, "Charmander");
        GameCardInstance evoInDeck1 = instance(actorUserId, evolutionCard.getId(), CardZone.DECK, 1);
        GameCardInstance evoInDeck2 = instance(actorUserId, evolutionCard.getId(), CardZone.DECK, 2);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, actorUserId)).thenReturn(List.of(ownPokemon));
        when(pokemonEvolutionStackStateService.findTopByPokemonInPlayId(pokemonInPlayId)).thenReturn(Optional.of(topStack));
        when(cardService.getCardEntityById(topCard.getId())).thenReturn(topCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, actorUserId, CardZone.DECK))
                .thenReturn(List.of(evoInDeck1, evoInDeck2));
        when(cardService.getCardEntityById(evolutionCard.getId())).thenReturn(evolutionCard);

        Map<String, Object> preview = effect().preview(
                context(gameId, actorUserId, pokemonInPlayId, trainerCard, trainerInstance, 3));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) preview.get("options");
        assertThat(options).hasSize(1);
        Map<String, Object> option = options.get(0);
        assertThat(option.get("cardId")).isEqualTo(evolutionCard.getId().toString());
        assertThat(option.get("externalId")).isEqualTo(evolutionCard.getExternalId());
        assertThat(option.get("count")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        List<String> validTargets = (List<String>) option.get("validTargetPokemonInPlayIds");
        assertThat(validTargets).containsExactly(pokemonInPlayId.toString());
    }

    @Test
    void preview_targetEnteredPlayThisTurn_isExcludedFromOptions() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card trainerCard = card(CardCategory.ITEM_TRAINER, "xy1-116");
        GameCardInstance trainerInstance = instance(actorUserId, trainerCard.getId(), CardZone.HAND, 1);

        UUID pokemonInPlayId = UUID.randomUUID();
        PokemonInPlay ownPokemon = new PokemonInPlay();
        ownPokemon.setId(pokemonInPlayId);
        ownPokemon.setSlotPosition(0);
        ownPokemon.setEnteredPlayTurn(3); // entered play this same turn

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, actorUserId)).thenReturn(List.of(ownPokemon));

        Map<String, Object> preview = effect().preview(
                context(gameId, actorUserId, pokemonInPlayId, trainerCard, trainerInstance, 3));

        assertThat((List<?>) preview.get("options")).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private SearchEvolutionFromDeckTrainerEffect effect() {
        return new SearchEvolutionFromDeckTrainerEffect(
                new TrainerEffectDefinitionReader(new ObjectMapper()),
                gameCardInstanceStateService,
                pokemonInPlayStateService,
                pokemonEvolutionStackStateService,
                specialConditionStateService,
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

    private Card pokemonCard(String name, CardCategory category, String evolvesFrom) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(category);
        card.setName(name);
        card.setExternalId("test-" + name.toLowerCase());
        card.setEvolvesFrom(evolvesFrom);
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

    private GameEventDto event(UUID gameId, GameEventType type) {
        return new GameEventDto(UUID.randomUUID(), gameId, type, 7, false, Instant.now(), Map.of());
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, UUID targetPokemonInPlayId,
                                          Card trainerCard, GameCardInstance trainerInstance, int turnNumber) {
        return contextWithPayload(gameId, actorUserId, trainerCard, trainerInstance, turnNumber,
                Map.of("targetPokemonInPlayId", targetPokemonInPlayId.toString()));
    }

    private TrainerEffectContext contextWithPayload(UUID gameId, UUID actorUserId, Card trainerCard,
                                                      GameCardInstance trainerInstance, int turnNumber,
                                                      Map<String, Object> payload) {
        GameStateDto state = GameStateTestFactory.state(
                gameId, GameStatus.ACTIVE, TurnPhase.MAIN, turnNumber, 6, actorUserId,
                List.of(GameActionType.PLAY_TRAINER), Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId, UUID.randomUUID(), GameActionType.PLAY_TRAINER, 6, payload);
        return new TrainerEffectContext(gameId, actorUserId, request, state, trainerInstance, trainerCard, 7);
    }
}
