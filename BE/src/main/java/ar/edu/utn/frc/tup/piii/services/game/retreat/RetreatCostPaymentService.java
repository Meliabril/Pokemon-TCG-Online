package ar.edu.utn.frc.tup.piii.services.game.retreat;

import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.UUID;

public interface RetreatCostPaymentService {

    int payRetreatCost(UUID gameId, UUID actorUserId, PokemonInPlay activePokemon, int retreatCost);
}
