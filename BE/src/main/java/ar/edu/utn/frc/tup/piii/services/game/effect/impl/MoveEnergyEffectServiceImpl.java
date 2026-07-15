package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.effect.MoveEnergyEffectService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveEnergyEffectServiceImpl implements MoveEnergyEffectService {

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final PassiveAbilityService passiveAbilityService;

    @Override
    public PokemonAttachedCard moveEnergy(PokemonAttachedCard attachedCard, PokemonInPlay fromPokemon, PokemonInPlay toPokemon) {
        if (attachedCard == null || fromPokemon == null || toPokemon == null) {
            throw new InvalidGameActionException("Energy move requires source energy, source Pokemon and target Pokemon");
        }
        if (fromPokemon.getId() == null || !fromPokemon.getId().equals(attachedCard.getPokemonInPlay().getId())) {
            throw new InvalidGameActionException("Selected energy is not attached to the declared source Pokemon");
        }
        attachedCard.setPokemonInPlay(toPokemon);
        PokemonAttachedCard movedCard = pokemonAttachedCardStateService.save(attachedCard);
        if (fromPokemon.getGame() != null) {
            passiveAbilityService.recalculateSweetVeil(fromPokemon.getGame(), fromPokemon.getOwnerUserId(), fromPokemon.getGame().getStateVersion());
            if (!fromPokemon.getOwnerUserId().equals(toPokemon.getOwnerUserId())) {
                passiveAbilityService.recalculateSweetVeil(fromPokemon.getGame(), toPokemon.getOwnerUserId(), fromPokemon.getGame().getStateVersion());
            }
        }
        return movedCard;
    }
}
