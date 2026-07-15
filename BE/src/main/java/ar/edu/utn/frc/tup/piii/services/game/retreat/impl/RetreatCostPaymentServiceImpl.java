package ar.edu.utn.frc.tup.piii.services.game.retreat.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.retreat.RetreatCostPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetreatCostPaymentServiceImpl implements RetreatCostPaymentService {

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;

    @Override
    public int payRetreatCost(UUID gameId, UUID actorUserId, PokemonInPlay activePokemon, int retreatCost) {
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(activePokemon.getId());
        List<PokemonAttachedCard> attachedEnergies = attachedEnergies(attachedCards);
        if (attachedEnergies.size() < retreatCost) {
            throw new InvalidGameActionException("Active Pokemon does not have enough attached energy to retreat");
        }

        for (int index = 0; index < retreatCost; index++) {
            discardEnergy(gameId, actorUserId, attachedEnergies.get(index));
        }

        return retreatCost;
    }

    private List<PokemonAttachedCard> attachedEnergies(List<PokemonAttachedCard> attachedCards) {
        List<PokemonAttachedCard> attachedEnergies = new ArrayList<>();
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (isEnergy(attachedCard)) {
                attachedEnergies.add(attachedCard);
            }
        }
        return attachedEnergies;
    }

    private void discardEnergy(UUID gameId, UUID actorUserId, PokemonAttachedCard attachedEnergy) {
        GameCardInstance energyInstance = attachedEnergy.getGameCardInstance();
        energyInstance.setZone(CardZone.DISCARD);
        energyInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, actorUserId, CardZone.DISCARD));
        gameCardInstanceStateService.save(energyInstance);
        pokemonAttachedCardStateService.delete(attachedEnergy);
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard.getAttachedCardType() == AttachedCardType.BASIC_ENERGY
                || attachedCard.getAttachedCardType() == AttachedCardType.SPECIAL_ENERGY;
    }
}
