package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonEvolutionStack;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.outcome.KnockoutService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonEvolutionStackStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnockoutServiceImplTest {

    @Mock
    private PokemonEvolutionStackStateService pokemonEvolutionStackStateService;

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Test
    void shouldDiscardActiveCardOnlyOnceWhenItAlsoExistsInEvolutionStack() {
        UUID gameId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        GameCardInstance baseCard = cardInstance(ownerUserId, CardZone.ACTIVE);
        GameCardInstance activeCard = cardInstance(ownerUserId, CardZone.ACTIVE);
        PokemonInPlay knockedOutPokemon = pokemonInPlay(ownerUserId, activeCard, 0);
        PokemonEvolutionStack baseStackEntry = stackEntry(knockedOutPokemon, baseCard, 0);
        PokemonEvolutionStack activeStackEntry = stackEntry(knockedOutPokemon, activeCard, 1);

        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(knockedOutPokemon.getId()))
                .thenReturn(List.of(baseStackEntry, activeStackEntry));
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(knockedOutPokemon.getId())).thenReturn(List.of());
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, ownerUserId)).thenReturn(List.of());
        when(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DISCARD)).thenReturn(1, 2);

        KnockoutService.KnockoutResult result = service().resolveKnockout(gameId, ownerUserId, knockedOutPokemon);

        assertThat(result.hasReplacementActivePokemon()).isFalse();
        assertThat(activeCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(baseCard.getZone()).isEqualTo(CardZone.DISCARD);
        verify(gameCardInstanceStateService, times(1)).save(activeCard);
        verify(gameCardInstanceStateService, times(1)).save(baseCard);
        verify(pokemonEvolutionStackStateService).delete(baseStackEntry);
        verify(pokemonEvolutionStackStateService).delete(activeStackEntry);
        verify(specialConditionStateService).deleteByPokemonInPlayId(knockedOutPokemon.getId());
        verify(pokemonInPlayStateService).delete(knockedOutPokemon);
    }

    @Test
    void shouldDiscardAttachedEnergyAndToolAndReportBenchReplacement() {
        UUID gameId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        GameCardInstance activeCard = cardInstance(ownerUserId, CardZone.ACTIVE);
        GameCardInstance energyCard = cardInstance(ownerUserId, CardZone.ATTACHED);
        GameCardInstance toolCard = cardInstance(ownerUserId, CardZone.ATTACHED);
        PokemonInPlay knockedOutPokemon = pokemonInPlay(ownerUserId, activeCard, 0);
        PokemonInPlay benchPokemon = pokemonInPlay(ownerUserId, cardInstance(ownerUserId, CardZone.BENCH), 1);
        PokemonAttachedCard attachedEnergy = attachedCard(knockedOutPokemon, energyCard, AttachedCardType.BASIC_ENERGY);
        PokemonAttachedCard attachedTool = attachedCard(knockedOutPokemon, toolCard, AttachedCardType.POKEMON_TOOL);

        when(pokemonEvolutionStackStateService.findByPokemonInPlayId(knockedOutPokemon.getId())).thenReturn(List.of());
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(knockedOutPokemon.getId()))
                .thenReturn(List.of(attachedEnergy, attachedTool));
        when(pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, ownerUserId)).thenReturn(List.of(benchPokemon));
        when(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DISCARD)).thenReturn(1, 2, 3);

        KnockoutService.KnockoutResult result = service().resolveKnockout(gameId, ownerUserId, knockedOutPokemon);

        assertThat(result.hasReplacementActivePokemon()).isTrue();
        assertThat(activeCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(energyCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(toolCard.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(energyCard.getFaceDown()).isFalse();
        assertThat(toolCard.getFaceDown()).isFalse();
        verify(pokemonAttachedCardStateService).delete(attachedEnergy);
        verify(pokemonAttachedCardStateService).delete(attachedTool);
        verify(gameCardInstanceStateService).resequenceZone(gameId, ownerUserId, CardZone.ATTACHED);
        verify(gameCardInstanceStateService).resequenceZone(gameId, ownerUserId, CardZone.DISCARD);
    }

    private KnockoutServiceImpl service() {
        return new KnockoutServiceImpl(
                pokemonEvolutionStackStateService,
                pokemonAttachedCardStateService,
                pokemonInPlayStateService,
                gameCardInstanceStateService,
                specialConditionStateService);
    }

    private PokemonInPlay pokemonInPlay(UUID ownerUserId, GameCardInstance activeCard, int slotPosition) {
        PokemonInPlay pokemonInPlay = new PokemonInPlay();
        pokemonInPlay.setId(UUID.randomUUID());
        pokemonInPlay.setOwnerUserId(ownerUserId);
        pokemonInPlay.setActiveCardInstance(activeCard);
        pokemonInPlay.setSlotPosition(slotPosition);
        pokemonInPlay.setDamageCounters(0);
        pokemonInPlay.setEnteredPlayTurn(1);
        return pokemonInPlay;
    }

    private GameCardInstance cardInstance(UUID ownerUserId, CardZone zone) {
        GameCardInstance cardInstance = new GameCardInstance();
        cardInstance.setId(UUID.randomUUID());
        cardInstance.setOwnerUserId(ownerUserId);
        cardInstance.setCardId(UUID.randomUUID());
        cardInstance.setZone(zone);
        cardInstance.setZonePosition(0);
        cardInstance.setFaceDown(true);
        return cardInstance;
    }

    private PokemonEvolutionStack stackEntry(PokemonInPlay pokemonInPlay, GameCardInstance cardInstance, int stackOrder) {
        PokemonEvolutionStack stackEntry = new PokemonEvolutionStack();
        stackEntry.setId(UUID.randomUUID());
        stackEntry.setPokemonInPlay(pokemonInPlay);
        stackEntry.setGameCardInstance(cardInstance);
        stackEntry.setStackOrder(stackOrder);
        stackEntry.setCreatedAtTurn(1);
        return stackEntry;
    }

    private PokemonAttachedCard attachedCard(
            PokemonInPlay pokemonInPlay,
            GameCardInstance cardInstance,
            AttachedCardType attachedCardType) {
        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setId(UUID.randomUUID());
        attachedCard.setPokemonInPlay(pokemonInPlay);
        attachedCard.setGameCardInstance(cardInstance);
        attachedCard.setAttachedCardType(attachedCardType);
        return attachedCard;
    }
}
