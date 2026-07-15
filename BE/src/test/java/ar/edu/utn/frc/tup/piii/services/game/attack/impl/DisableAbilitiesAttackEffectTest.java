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
class DisableAbilitiesAttackEffectTest {

    @Mock
    private PokemonInPlayStateService pokemonInPlayStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    @Test
    void shouldDisableAbilitiesUntilTheEndOfTheAttackersOwnNextTurn() {
        // Arbok (xy1-48) "Gastro Acid": "The Defending Pokemon has no Abilities until the end of
        // your next turn." Turns alternate one player at a time, so when this is declared on
        // turn N (the attacker's own turn), the attacker's *own* next turn is N + 2: the
        // opponent's intervening turn is N + 1, and only after that does control return to the
        // attacker.
        DisableAbilitiesAttackEffect effect =
                new DisableAbilitiesAttackEffect(pokemonInPlayStateService, gameEventFactory);
        when(gameEventFactory.publicEvent(any(), eq(GameEventType.ATTACK_EFFECT_RESOLVED), any(Integer.class), any()))
                .thenReturn(event());

        PokemonInPlay defendingPokemon = new PokemonInPlay();
        defendingPokemon.setId(UUID.randomUUID());

        AttackEffectResult result = effect.apply(context(defendingPokemon, 3));

        assertThat(defendingPokemon.getAbilitiesDisabledUntilTurn()).isEqualTo(5);
        assertThat(result.damageModifier()).isZero();
        assertThat(result.attackCancelled()).isFalse();
        assertThat(result.events()).hasSize(1);
        verify(pokemonInPlayStateService).save(defendingPokemon);
    }

    private AttackEffectContext context(PokemonInPlay defendingPokemon, int turnNumber) {
        AttackResolutionContext resolutionContext = new AttackResolutionContext(
                UUID.randomUUID(), null, null, null, defendingPokemon, null, null, null);
        AttackEffectOperation operation = new AttackEffectOperation(
                "DISABLE_ABILITIES",
                AttackEffectPhase.AFTER_DAMAGE,
                "DEFENDER",
                0,
                null,
                CoinRequirement.NONE,
                false,
                0,
                null);
        return new AttackEffectContext(resolutionContext, defendingPokemon, operation, Map.of(), 1, turnNumber, 0);
    }

    private GameEventDto event() {
        return new GameEventDto(UUID.randomUUID(), UUID.randomUUID(), GameEventType.ATTACK_EFFECT_RESOLVED, 1, false, Instant.now(), Map.of());
    }
}
