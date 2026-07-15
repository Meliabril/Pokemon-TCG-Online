package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;




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
import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionExecutionResult;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetreatServiceImplTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private StadiumModifierService stadiumModifierService;

    @Test
    void shouldDiscardFirstAttachedEnergiesPromoteBenchAndClearConditions() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID activePokemonInPlayId = UUID.randomUUID();
        UUID benchPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemonInPlay(
                activePokemonInPlayId,
                actorUserId,
                activeCardInstance(actorUserId, activeCardId),
                0);
        PokemonInPlay benchPokemon = pokemonInPlay(
                benchPokemonInPlayId,
                actorUserId,
                activeCardInstance(actorUserId, UUID.randomUUID()),
                1);
        PokemonAttachedCard firstEnergy = attachedEnergy(actorUserId, UUID.randomUUID());
        PokemonAttachedCard secondEnergy = attachedEnergy(actorUserId, UUID.randomUUID());
        PokemonAttachedCard thirdEnergy = attachedEnergy(actorUserId, UUID.randomUUID());
        Card activeCard = pokemonCard(activeCardId, 2);
        GameEventDto retreatEvent = event(gameId);
        RetreatServiceImpl service = service();

        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.of(activePokemon));
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(benchPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(benchPokemon));
        when(cardService.getCardEntityById(activeCardId)).thenReturn(activeCard);
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemonInPlayId))
                .thenReturn(List.of(firstEnergy, secondEnergy, thirdEnergy));
        when(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD))
                .thenReturn(1, 2);
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(1);
        when(pokemonInPlayStateService.countBenchPokemon(gameId, opponentUserId)).thenReturn(0);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.RETREAT_DONE), eq(8), anyMap()))
                .thenReturn(retreatEvent);

        GameActionExecutionResult result = service.retreat(context(
                gameId,
                actorUserId,
                opponentUserId,
                benchPokemonInPlayId));

        assertThat(result.gameState().turn().retreatedThisTurn()).isTrue();
        assertThat(GameStateTestFactory.activePokemonConditionsByPlayer(result.gameState()).get(actorUserId)).isEmpty();
        assertThat(firstEnergy.getGameCardInstance().getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(firstEnergy.getGameCardInstance().getZonePosition()).isEqualTo(1);
        assertThat(secondEnergy.getGameCardInstance().getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(secondEnergy.getGameCardInstance().getZonePosition()).isEqualTo(2);
        assertThat(thirdEnergy.getGameCardInstance().getZone()).isEqualTo(CardZone.ATTACHED);
        assertThat(result.emittedEvents()).containsExactly(retreatEvent);
        verify(pokemonAttachedCardStateService).delete(firstEnergy);
        verify(pokemonAttachedCardStateService).delete(secondEnergy);
        verify(pokemonAttachedCardStateService, never()).delete(thirdEnergy);
        verify(specialConditionStateService).deleteByPokemonInPlayId(activePokemonInPlayId);
        verify(gameCardInstanceStateService).swapActiveWithBench(
                activePokemon.getActiveCardInstance().getId(),
                benchPokemon.getActiveCardInstance().getId(),
                1);
        verify(pokemonInPlayStateService).swapActiveWithBench(activePokemonInPlayId, benchPokemonInPlayId, 1);
    }

    @Test
    void shouldRetreatWithoutDiscardingEnergyWhenFairyGardenApplies() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID opponentUserId = UUID.randomUUID();
        UUID activeCardId = UUID.randomUUID();
        UUID activePokemonInPlayId = UUID.randomUUID();
        UUID benchPokemonInPlayId = UUID.randomUUID();
        PokemonInPlay activePokemon = pokemonInPlay(
                activePokemonInPlayId,
                actorUserId,
                activeCardInstance(actorUserId, activeCardId),
                0);
        PokemonInPlay benchPokemon = pokemonInPlay(
                benchPokemonInPlayId,
                actorUserId,
                activeCardInstance(actorUserId, UUID.randomUUID()),
                1);
        PokemonAttachedCard energy = attachedEnergy(actorUserId, UUID.randomUUID());
        Card activeCard = pokemonCard(activeCardId, 3);
        GameEventDto retreatEvent = event(gameId);
        RetreatServiceImpl service = service();

        when(pokemonInPlayStateService.findActivePokemon(gameId, actorUserId)).thenReturn(Optional.of(activePokemon));
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(benchPokemonInPlayId, gameId, actorUserId))
                .thenReturn(Optional.of(benchPokemon));
        when(cardService.getCardEntityById(activeCardId)).thenReturn(activeCard);
        when(stadiumModifierService.isRetreatFree(gameId, activePokemon)).thenReturn(true);
        when(pokemonInPlayStateService.countBenchPokemon(gameId, actorUserId)).thenReturn(1);
        when(pokemonInPlayStateService.countBenchPokemon(gameId, opponentUserId)).thenReturn(0);
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.RETREAT_DONE), eq(8), anyMap()))
                .thenReturn(retreatEvent);

        GameActionExecutionResult result = service.retreat(context(
                gameId,
                actorUserId,
                opponentUserId,
                benchPokemonInPlayId));

        assertThat(result.gameState().turn().retreatedThisTurn()).isTrue();
        assertThat(energy.getGameCardInstance().getZone()).isEqualTo(CardZone.ATTACHED);
        verify(pokemonAttachedCardStateService, never()).delete(energy);
    }

    private RetreatServiceImpl service() {
        return new RetreatServiceImpl(
                new GameActionPayloadReaderImpl(),
                new AvailableActionsFactoryImpl(),
                new RetreatCostPaymentServiceImpl(pokemonAttachedCardStateService, gameCardInstanceStateService),
                pokemonInPlayStateService,
                gameCardInstanceStateService,
                cardService,
                specialConditionStateService,
                gameEventFactory,
                stadiumModifierService);
    }

    private GameActionContext context(
            UUID gameId,
            UUID actorUserId,
            UUID opponentUserId,
            UUID benchPokemonInPlayId) {
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                3,
                7,
                actorUserId,
                List.of(actorUserId, opponentUserId),
                false,
                false,
                false,
                Map.of(actorUserId, 1, opponentUserId, 0),
                Map.of(actorUserId, List.of(SpecialConditionType.BURNED), opponentUserId, List.of()),
                3,
                actorUserId,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(GameActionType.RETREAT),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.RETREAT,
                7,
                Map.<String, Object>of("targetPokemonInPlayId", benchPokemonInPlayId.toString()));
        return new GameActionContext(gameId, actorUserId, request, state);
    }

    private PokemonInPlay pokemonInPlay(
            UUID pokemonInPlayId,
            UUID ownerUserId,
            GameCardInstance activeCardInstance,
            int slotPosition) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(pokemonInPlayId);
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCardInstance);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setEnteredPlayTurn(1);
        pokemonInPlay.setDamageCounters(0);
        return pokemonInPlay;
    }

    private GameCardInstance activeCardInstance(UUID ownerUserId, UUID cardId) {
        return cardInstance(ownerUserId, cardId, CardZone.ACTIVE);
    }

    private PokemonAttachedCard attachedEnergy(UUID ownerUserId, UUID cardId) {
        GameCardInstance energyInstance = cardInstance(ownerUserId, cardId, CardZone.ATTACHED);
        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setId(UUID.randomUUID());
        attachedCard.setGameCardInstance(energyInstance);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        return attachedCard;
    }

    private GameCardInstance cardInstance(UUID ownerUserId, UUID cardId, CardZone zone) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(cardId);
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(1);
        cardInstance.setFaceDown(false);
        return cardInstance;
    }

    private Card pokemonCard(UUID cardId, int retreatCost) {
        Card card = new Card();
        card.setId(cardId);
        card.setExternalId("test-pokemon-" + cardId);
        card.setSupertype(CardSupertype.POKEMON);
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setRetreatCost(retreatCost);
        card.setRawJson("{}");
        return card;
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(
                UUID.randomUUID(),
                gameId,
                GameEventType.RETREAT_DONE,
                8,
                false,
                Instant.parse("2026-05-24T12:00:01Z"),
                Map.of());
    }
}
