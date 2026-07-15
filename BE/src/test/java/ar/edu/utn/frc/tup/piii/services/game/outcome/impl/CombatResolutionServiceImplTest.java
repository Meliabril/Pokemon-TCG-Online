package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.CombatResolutionService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutDetectionService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.PrizeValueService;
import ar.edu.utn.frc.tup.piii.services.game.outcome.VictoryConditionService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatResolutionServiceImplTest {

    @Mock
    private KnockoutDetectionService knockoutDetectionService;

    @Mock
    private KnockoutService knockoutService;

    @Mock
    private PrizeService prizeService;

    @Mock
    private PrizeValueService prizeValueService;

    @Mock
    private VictoryConditionService victoryConditionService;

    @Mock
    private CardService cardService;

    @Mock
    private GameLookupService gameLookupService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Test
    void shouldNotRequirePromotionWhenKnockedOutActiveWasAlreadyReplacedByAttackEffect() {
        UUID gameId = UUID.randomUUID();
        UUID knockedOutOwnerUserId = UUID.randomUUID();
        UUID rewardPlayerId = UUID.randomUUID();
        PokemonInPlay knockedOutPokemon = pokemon(0);
        PokemonInPlay replacementActivePokemon = pokemon(0);
        Card knockedOutTopCard = new Card();
        PrizeService.PrizeCardsResult prizeResult =
                new PrizeService.PrizeCardsResult(List.of(UUID.randomUUID()), 5);
        CombatResolutionServiceImpl service = service();

        when(knockoutDetectionService.isKnockedOut(knockedOutPokemon)).thenReturn(true);
        when(cardService.getCardEntityById(knockedOutPokemon.getActiveCardInstance().getCardId()))
                .thenReturn(knockedOutTopCard);
        when(prizeValueService.prizeCardsFor(knockedOutTopCard)).thenReturn(1);
        when(knockoutService.resolveKnockout(gameId, knockedOutOwnerUserId, knockedOutPokemon))
                .thenReturn(new KnockoutService.KnockoutResult(null, false));
        when(prizeService.takePrizes(gameId, rewardPlayerId, 1)).thenReturn(prizeResult);
        when(pokemonInPlayStateService.findActivePokemon(gameId, knockedOutOwnerUserId))
                .thenReturn(Optional.of(replacementActivePokemon));
        when(victoryConditionService.attackerWinsAfterKnockout(5, true)).thenReturn(false);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.POKEMON_KNOCKED_OUT), any(Integer.class), any()))
                .thenReturn(event(GameEventType.POKEMON_KNOCKED_OUT));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.PRIZE_TAKEN), any(Integer.class), any()))
                .thenReturn(event(GameEventType.PRIZE_TAKEN));

        CombatResolutionService.CombatResolutionResult result = service.resolveKnockoutIfNeeded(
                gameId,
                knockedOutOwnerUserId,
                rewardPlayerId,
                knockedOutPokemon,
                knockedOutOwnerUserId,
                3,
                12,
                "ATTACK_DAMAGE");

        assertThat(result.knockedOut()).isTrue();
        assertThat(result.promotionPending()).isFalse();
        assertThat(result.gameFinished()).isFalse();
        verify(gameEventFactory, never()).publicEvent(
                any(),
                eq(GameEventType.PROMOTION_REQUIRED),
                any(Integer.class),
                any());
        verify(gameLookupService, never()).getRequiredGame(any());
    }

    private CombatResolutionServiceImpl service() {
        return new CombatResolutionServiceImpl(
                knockoutDetectionService,
                knockoutService,
                prizeService,
                prizeValueService,
                victoryConditionService,
                cardService,
                gameLookupService,
                gameEventFactory,
                pokemonInPlayStateService);
    }

    private PokemonInPlay pokemon(int slotPosition) {
        GameCardInstance activeCardInstance = new GameCardInstance();
        activeCardInstance.setId(UUID.randomUUID());
        activeCardInstance.setCardId(UUID.randomUUID());

        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        pokemon.setSlotPosition(slotPosition);
        pokemon.setActiveCardInstance(activeCardInstance);
        return pokemon;
    }

    private GameEventDto event(GameEventType eventType) {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), eventType, 12, false, Instant.now(), Map.of());
    }
}
