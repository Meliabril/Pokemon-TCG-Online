package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchEnergyFromDeckAttachAndSwitchAttackEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;
    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;
    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;
    @Mock
    private CardService cardService;
    @Mock
    private GameRandomService gameRandomService;
    @Mock
    private GameEventFactory gameEventFactory;
    @Mock
    private GameActionPayloadReader payloadReader;

    @InjectMocks
    private SearchEnergyFromDeckAttachAndSwitchAttackEffect effect;

    @Test
    void supports_returnsTrue() {
        AttackEffectOperation operation = new AttackEffectOperation(
                "SEARCH_ENERGY_FROM_DECK_ATTACH_AND_SWITCH",
                null,
                null,
                0,
                null,
                null,
                false,
                0,
                null,
                null,
                "Lightning");
        assertThat(effect.supports(operation)).isTrue();
    }

    @Test
    void supports_returnsFalse() {
        AttackEffectOperation operation = new AttackEffectOperation(
                "OTHER",
                null,
                null,
                0,
                null,
                null,
                false,
                0,
                null,
                null,
                null);
        assertThat(effect.supports(operation)).isFalse();
    }

    @Test
    void supports_null_returnsFalse() {
        assertThat(effect.supports(null)).isFalse();
    }

    @Test
    void apply_searchAttachAndSwitch_success() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        
        GameCardInstance activeInstance = new GameCardInstance();
        activeInstance.setId(UUID.randomUUID());
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        attackerPokemon.setSlotPosition(0);
        attackerPokemon.setActiveCardInstance(activeInstance);

        AttackResolutionContext resolutionContext = new AttackResolutionContext(gameId, attackerUserId, UUID.randomUUID(), attackerPokemon, null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "SEARCH_ENERGY_FROM_DECK_ATTACH_AND_SWITCH",
                null,
                null,
                0,
                null,
                null,
                false,
                0,
                null,
                null,
                "Lightning");
        Map<String, Object> payload = Map.of("selfTargetPokemonInPlayId", UUID.randomUUID().toString());
        AttackEffectContext context = new AttackEffectContext(resolutionContext, null, operation, payload, 5, 1, 0);

        GameCardInstance deckCard = new GameCardInstance();
        deckCard.setId(UUID.randomUUID());
        deckCard.setCardId(UUID.randomUUID());
        
        Card energyCard = new Card();
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        energyCard.setPokemonType("Lightning");

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(cardService.getCardEntityById(deckCard.getCardId())).thenReturn(energyCard);
        
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(3);
        

        PokemonInPlay benchPokemon = new PokemonInPlay();
        benchPokemon.setId(UUID.randomUUID());
        benchPokemon.setSlotPosition(1);
        GameCardInstance benchInstance = new GameCardInstance();
        benchInstance.setId(UUID.randomUUID());
        benchPokemon.setActiveCardInstance(benchInstance);
        
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId)).thenReturn(List.of(attackerPokemon, benchPokemon));
        UUID selectedId = UUID.fromString(payload.get("selfTargetPokemonInPlayId").toString());
        when(payloadReader.requiredUuid(payload, "selfTargetPokemonInPlayId")).thenReturn(selectedId);
        when(pokemonInPlayStateService.findByIdAndGameIdAndOwnerUserId(selectedId, gameId, attackerUserId)).thenReturn(Optional.of(benchPokemon));

        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, false, Instant.now(), Map.of());
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), any())).thenReturn(event);

        AttackEffectResult result = effect.apply(context);

        assertThat(result.events()).containsExactly(event);
        
        verify(gameCardInstanceStateService).save(deckCard);
        verify(pokemonAttachedCardStateService).save(any(PokemonAttachedCard.class));
        
        verify(gameCardInstanceStateService).swapActiveWithBench(activeInstance.getId(), benchInstance.getId(), 1);
        verify(pokemonInPlayStateService).swapActiveWithBench(attackerPokemon.getId(), benchPokemon.getId(), 1);
    }
    
    @Test
    void apply_noEnergyFound_doesNotSwitch() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = new AttackResolutionContext(gameId, attackerUserId, UUID.randomUUID(), attackerPokemon, null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "SEARCH_ENERGY_FROM_DECK_ATTACH_AND_SWITCH",
                null,
                null,
                0,
                null,
                null,
                false,
                0,
                null,
                null,
                "Lightning");
        Map<String, Object> payload = Map.of();
        AttackEffectContext context = new AttackEffectContext(resolutionContext, null, operation, payload, 5, 1, 0);

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of());

        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, false, Instant.now(), Map.of());
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), any())).thenReturn(event);

        AttackEffectResult result = effect.apply(context);

        assertThat(result.events()).containsExactly(event);
        verifyNoInteractions(pokemonAttachedCardStateService);
        verifyNoInteractions(payloadReader);
    }

    @Test
    void apply_noBench_doesNotSwitch() {
        UUID gameId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();
        
        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());

        AttackResolutionContext resolutionContext = new AttackResolutionContext(gameId, attackerUserId, UUID.randomUUID(), attackerPokemon, null, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "SEARCH_ENERGY_FROM_DECK_ATTACH_AND_SWITCH",
                null,
                null,
                0,
                null,
                null,
                false,
                0,
                null,
                null,
                "Lightning");
        Map<String, Object> payload = Map.of();
        AttackEffectContext context = new AttackEffectContext(resolutionContext, null, operation, payload, 5, 1, 0);

        GameCardInstance deckCard = new GameCardInstance();
        deckCard.setId(UUID.randomUUID());
        deckCard.setCardId(UUID.randomUUID());
        
        Card energyCard = new Card();
        energyCard.setCategory(CardCategory.BASIC_ENERGY);
        energyCard.setPokemonType("Lightning");

        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, attackerUserId, CardZone.DECK))
                .thenReturn(List.of(deckCard));
        when(cardService.getCardEntityById(deckCard.getCardId())).thenReturn(energyCard);
        
        when(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED)).thenReturn(3);

        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId)).thenReturn(List.of(attackerPokemon));

        GameEventDto event = new GameEventDto(UUID.randomUUID(), gameId, GameEventType.ATTACK_EFFECT_RESOLVED, 5, false, Instant.now(), Map.of());
        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.ATTACK_EFFECT_RESOLVED), eq(5), any())).thenReturn(event);

        AttackEffectResult result = effect.apply(context);

        assertThat(result.events()).containsExactly(event);
        verifyNoInteractions(payloadReader);
    }
}
