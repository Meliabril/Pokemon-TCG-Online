package ar.edu.utn.frc.tup.piii.services.game.effect.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.effect.DiscardCardEffectService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscardCardEffectServiceImpl implements DiscardCardEffectService {

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Override
    public GameCardInstance discardFromHand(UUID gameId, UUID playerId, UUID cardInstanceId) {
        GameCardInstance cardInstance = gameCardInstanceStateService.findByIdAndGameIdAndOwnerUserId(cardInstanceId, gameId, playerId)
                .orElseThrow(() -> new InvalidGameActionException("Selected hand card was not found"));
        if (!CardZone.HAND.equals(cardInstance.getZone())) {
            throw new InvalidGameActionException("Selected card is not in hand");
        }

        cardInstance.setZone(CardZone.DISCARD);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, playerId, CardZone.DISCARD));
        cardInstance.setFaceDown(false);
        GameCardInstance discardedCard = gameCardInstanceStateService.save(cardInstance);
        gameCardInstanceStateService.resequenceZone(gameId, playerId, CardZone.HAND);
        return discardedCard;
    }

    @Override
    public GameCardInstance discardAttachedCard(UUID gameId, UUID ownerUserId, PokemonAttachedCard attachedCard) {
        if (attachedCard == null || attachedCard.getGameCardInstance() == null) {
            throw new InvalidGameActionException("Attached card is required to discard");
        }

        GameCardInstance cardInstance = attachedCard.getGameCardInstance();
        cardInstance.setZone(CardZone.DISCARD);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, ownerUserId, CardZone.DISCARD));
        cardInstance.setFaceDown(false);
        GameCardInstance discardedCard = gameCardInstanceStateService.save(cardInstance);
        pokemonAttachedCardStateService.delete(attachedCard);
        return discardedCard;
    }
}
