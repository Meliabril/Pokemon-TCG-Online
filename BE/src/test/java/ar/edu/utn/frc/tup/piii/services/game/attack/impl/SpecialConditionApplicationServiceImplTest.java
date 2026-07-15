package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class SpecialConditionApplicationServiceImplTest {

    @Mock
    private SpecialConditionStateService specialConditionStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Mock
    private PassiveAbilityService passiveAbilityService;

    @Mock
    private GameLookupService gameLookupService;

    @Test
    void shouldReplaceConfusedWithAsleepWithoutSkippingTheNewCondition() {
        UUID gameId = UUID.randomUUID();
        UUID sourcePlayerId = UUID.randomUUID();
        UUID pokemonId = UUID.randomUUID();
        PokemonInPlay targetPokemon = pokemon(pokemonId);
        GameEventDto event = event(gameId);
        SpecialConditionApplicationServiceImpl service = service();

        when(gameEventFactory.publicEvent(eq(gameId), eq(GameEventType.STATUS_APPLIED), eq(9), any()))
                .thenReturn(event);

        List<GameEventDto> events = service.applyCondition(
                gameId,
                sourcePlayerId,
                targetPokemon,
                SpecialConditionType.ASLEEP,
                4,
                9);

        ArgumentCaptor<SpecialCondition> savedCondition = ArgumentCaptor.forClass(SpecialCondition.class);
        verify(specialConditionStateService).clearConditions(
                pokemonId,
                java.util.EnumSet.of(
                        SpecialConditionType.ASLEEP,
                        SpecialConditionType.CONFUSED,
                        SpecialConditionType.PARALYZED));
        verify(specialConditionStateService).save(savedCondition.capture());
        verify(specialConditionStateService, never()).findByPokemonInPlayIdAndConditionType(pokemonId, SpecialConditionType.ASLEEP);
        assertThat(savedCondition.getValue().getConditionType()).isEqualTo(SpecialConditionType.ASLEEP);
        assertThat(savedCondition.getValue().getAppliedTurn()).isEqualTo(4);
        assertThat(events).containsExactly(event);
    }

    @Test
    void shouldSkipSavingWhenNonExclusiveConditionAlreadyExists() {
        UUID pokemonId = UUID.randomUUID();
        PokemonInPlay targetPokemon = pokemon(pokemonId);
        SpecialCondition poisoned = new SpecialCondition();
        poisoned.setPokemonInPlay(targetPokemon);
        poisoned.setConditionType(SpecialConditionType.POISONED);
        SpecialConditionApplicationServiceImpl service = service();

        when(specialConditionStateService.findByPokemonInPlayIdAndConditionType(pokemonId, SpecialConditionType.POISONED))
                .thenReturn(Optional.of(poisoned));

        List<GameEventDto> events = service.applyCondition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                targetPokemon,
                SpecialConditionType.POISONED,
                2,
                5);

        verify(specialConditionStateService, never()).clearConditions(any(), any());
        verify(specialConditionStateService, never()).save(any());
        assertThat(events).isEmpty();
    }

    private SpecialConditionApplicationServiceImpl service() {
        return new SpecialConditionApplicationServiceImpl(specialConditionStateService, gameEventFactory, passiveAbilityService, gameLookupService);
    }

    private PokemonInPlay pokemon(UUID pokemonId) {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(pokemonId);
        pokemon.setOwnerUserId(UUID.randomUUID());
        return pokemon;
    }

    private GameEventDto event(UUID gameId) {
        return new GameEventDto(UUID.randomUUID(), gameId, GameEventType.STATUS_APPLIED, 9, false, Instant.now(), Map.of());
    }
}
