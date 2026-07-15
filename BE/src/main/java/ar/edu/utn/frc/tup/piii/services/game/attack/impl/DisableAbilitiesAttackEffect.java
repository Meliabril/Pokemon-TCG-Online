package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DisableAbilitiesAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DISABLE_ABILITIES";

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay targetPokemon = context.targetPokemon();
        // Card text ("Gastro Acid", xy1-48): "The Defending Pokemon has no Abilities until the
        // end of your next turn." Unlike the many "opponent's next turn" lock effects in this
        // package (which correctly use turnNumber + 1, since that next turn belongs to the
        // opponent), this duration is anchored to the *attacker's own* next turn. Turn numbers
        // alternate between players one at a time, so the attacker's next turn is two turns
        // ahead of the current one (current turn -> opponent's intervening turn -> attacker's
        // next turn), not one.
        int disabledUntilTurn = context.turnNumber() + 2;
        targetPokemon.setAbilitiesDisabledUntilTurn(disabledUntilTurn);
        pokemonInPlayStateService.save(targetPokemon);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", targetPokemon.getId().toString(),
                        "disabledUntilTurn", disabledUntilTurn));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
