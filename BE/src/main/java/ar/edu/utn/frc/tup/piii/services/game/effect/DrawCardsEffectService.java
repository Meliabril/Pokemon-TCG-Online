package ar.edu.utn.frc.tup.piii.services.game.effect;

import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;

import java.util.List;
import java.util.UUID;

public interface DrawCardsEffectService {

    List<GameCardInstance> drawCards(UUID gameId, UUID playerId, int amount);

    List<GameCardInstance> drawUntilHandSize(UUID gameId, UUID playerId, int targetHandSize);
}
