package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchPokemonFromDeckByTypeToHandAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @Mock
    private GameRandomService gameRandomService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldMoveFirstMatchingPokemonToHandAndShuffleRemainingDeck() {
        SearchPokemonFromDeckByTypeToHandAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance firePokemon = deckCard(UUID.randomUUID(), 1);
        GameCardInstance grassPokemon = deckCard(UUID.randomUUID(), 2);
        GameCardInstance energyCard = deckCard(UUID.randomUUID(), 3);
        List<GameCardInstance> deckCards = List.of(firePokemon, grassPokemon, energyCard);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(deckCards);
        when(cardService.getCardEntityById(firePokemon.getCardId())).thenReturn(pokemonCard("Fire"));
        when(cardService.getCardEntityById(grassPokemon.getCardId())).thenReturn(pokemonCard("Grass"));
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.HAND)).thenReturn(4);
        when(gameRandomService.shuffledCopy(List.of(firePokemon, energyCard))).thenReturn(List.of(energyCard, firePokemon));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).hasSize(1);
        assertThat(grassPokemon.getZone()).isEqualTo(CardZone.HAND);
        assertThat(grassPokemon.getZonePosition()).isEqualTo(4);
        assertThat(energyCard.getZonePosition()).isEqualTo(1);
        assertThat(firePokemon.getZonePosition()).isEqualTo(2);
        verify(gameCardInstanceStateService).save(grassPokemon);
        verify(gameCardInstanceStateService).resequenceZone(gameId, attackerUserId, CardZone.HAND);
        verify(gameCardInstanceStateService).flush();
    }

    @Test
    void shouldOnlyEmitEventWhenNoMatchingPokemonIsFound() {
        SearchPokemonFromDeckByTypeToHandAttackEffect effect = effect();
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        GameCardInstance firePokemon = deckCard(UUID.randomUUID(), 1);
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(firePokemon));
        when(cardService.getCardEntityById(firePokemon.getCardId())).thenReturn(pokemonCard("Fire"));
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        AttackEffectResult result = effect.apply(context(gameId, attackerUserId));

        assertThat(result.events()).hasSize(1);
        verify(gameCardInstanceStateService, never()).save(any());
        verify(gameRandomService, never()).shuffledCopy(any());
    }

    private SearchPokemonFromDeckByTypeToHandAttackEffect effect() {
        return new SearchPokemonFromDeckByTypeToHandAttackEffect(
                gameCardInstanceStateService,
                cardService,
                gameRandomService,
                gameEventFactory);
    }

    private AttackEffectContext context(UUID gameId, UUID attackerUserId) {
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                gameId,
                attackerUserId,
                UUID.randomUUID(),
                attackerPokemon,
                null,
                null,
                null,
                null);
        return new AttackEffectContext(resolutionContext, null, operation(), Map.of(), 1, 1, 0);
    }

    private AttackEffectOperation operation() {
        return new AttackEffectOperation(
                "SEARCH_POKEMON_FROM_DECK_BY_TYPE_TO_HAND",
                AttackEffectPhase.AFTER_DAMAGE,
                "ATTACKER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null,
                null,
                null,
                "Grass",
                null);
    }

    private GameCardInstance deckCard(UUID cardId, int zonePosition) {
        GameCardInstance card = new GameCardInstance();
        card.setId(UUID.randomUUID());
        card.setCardId(cardId);
        card.setZone(CardZone.DECK);
        card.setZonePosition(zonePosition);
        card.setFaceDown(true);
        return card;
    }

    private Card pokemonCard(String pokemonType) {
        Card card = new Card();
        card.setCategory(CardCategory.BASIC_POKEMON);
        card.setPokemonType(pokemonType);
        return card;
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
