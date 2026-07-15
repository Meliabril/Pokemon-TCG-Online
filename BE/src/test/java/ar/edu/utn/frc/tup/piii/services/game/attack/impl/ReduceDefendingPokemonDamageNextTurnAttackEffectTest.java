package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectPhase;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.CoinRequirement;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReduceDefendingPokemonDamageNextTurnAttackEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldSetDamageReductionFieldsOnDefenderPokemon() {
        ReduceDefendingPokemonDamageNextTurnAttackEffect effect =
                new ReduceDefendingPokemonDamageNextTurnAttackEffect(pokemonInPlayStateService, gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay attackerPokemon = new PokemonInPlay();
        attackerPokemon.setId(UUID.randomUUID());
        attackerPokemon.setOwnerUserId(UUID.randomUUID());
        PokemonInPlay defenderPokemon = new PokemonInPlay();
        defenderPokemon.setId(UUID.randomUUID());

        AttackEffectResult result = effect.apply(context(attackerPokemon, defenderPokemon, 3));

        assertThat(defenderPokemon.getDamageReductionNextTurn()).isEqualTo(4);
        assertThat(defenderPokemon.getDamageReductionAmount()).isEqualTo(20);
        assertThat(result.events()).hasSize(1);
        verify(pokemonInPlayStateService).save(defenderPokemon);
    }

    private AttackEffectContext context(PokemonInPlay attackerPokemon, PokemonInPlay defenderPokemon, int turnNumber) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(),
                attackerPokemon.getOwnerUserId(),
                UUID.randomUUID(),
                attackerPokemon,
                defenderPokemon,
                null,
                null,
                null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "REDUCE_DEFENDING_POKEMON_DAMAGE_NEXT_TURN",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                20,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, defenderPokemon, operation, Map.of(), 1, turnNumber, 0);
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
