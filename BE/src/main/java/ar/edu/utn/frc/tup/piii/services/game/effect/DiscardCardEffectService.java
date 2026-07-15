package ar.edu.utn.frc.tup.piii.services.game.effect;

import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;

import java.util.UUID;

public interface DiscardCardEffectService {

    GameCardInstance discardFromHand(UUID gameId, UUID playerId, UUID cardInstanceId);

    GameCardInstance discardAttachedCard(UUID gameId, UUID ownerUserId, PokemonAttachedCard attachedCard);
}
