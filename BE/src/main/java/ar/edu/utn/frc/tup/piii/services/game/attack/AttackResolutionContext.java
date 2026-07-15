package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.UUID;

public record AttackResolutionContext(
        UUID gameId,
        UUID attackerUserId,
        UUID defenderUserId,
        PokemonInPlay attackerPokemon,
        PokemonInPlay defenderPokemon,
        Card attackerCard,
        Card defenderCard,
        Attack selectedAttack) {
}
