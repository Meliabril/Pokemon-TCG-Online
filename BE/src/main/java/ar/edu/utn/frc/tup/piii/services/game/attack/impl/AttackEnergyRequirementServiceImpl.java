package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.AttackCost;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEnergyRequirementService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackEnergyRequirementServiceImpl implements AttackEnergyRequirementService {

    private static final String COLORLESS_ENERGY_TYPE = "colorless";
    private static final String DOUBLE_COLORLESS_ENERGY_EXTERNAL_ID = "xy1-130";
    private static final int DOUBLE_COLORLESS_ENERGY_VALUE = 2;

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;

    @Override
    public boolean hasRequiredEnergy(PokemonInPlay attackerPokemon, Attack attack) {
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(attackerPokemon.getId());
        int wildcardEnergyCount = 0;
        Map<String, Integer> energyByType = new LinkedHashMap<>();
        int totalEnergy = 0;
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (attachedCard.getAttachedCardType() == AttachedCardType.POKEMON_TOOL) {
                continue;
            }
            if (attachedCard.getAttachedCardType() == AttachedCardType.SPECIAL_ENERGY) {
                int wildcardValue = specialEnergyWildcardValue(attachedCard);
                wildcardEnergyCount += wildcardValue;
                totalEnergy += wildcardValue;
                continue;
            }
            totalEnergy++;
            if (attachedCard.getAttachedCardType() == AttachedCardType.BASIC_ENERGY) {
                Card energyCard = findCardOrNull(attachedCard.getGameCardInstance().getCardId());
                String energyType = energyCard == null ? null : resolveBasicEnergyType(energyCard);
                if (energyType != null) {
                    incrementEnergyCount(energyByType, energyType.toLowerCase());
                }
            }
        }

        int requiredTotal = requiredTotal(attack);
        if (totalEnergy < requiredTotal) {
            return false;
        }

        return hasTypedEnergyOrWildcards(attack, energyByType, wildcardEnergyCount, totalEnergy);
    }

    private int requiredTotal(Attack attack) {
        int requiredTotal = 0;
        for (AttackCost cost : attack.getCosts()) {
            requiredTotal = requiredTotal + cost.getQuantity();
        }
        return requiredTotal;
    }

    private boolean hasTypedEnergyOrWildcards(
            Attack attack,
            Map<String, Integer> energyByType,
            int wildcardEnergyCount,
            int totalEnergy) {
        int remainingWildcardEnergy = wildcardEnergyCount;
        int usedEnergy = 0;
        int colorlessEnergyRequired = 0;
        for (AttackCost cost : attack.getCosts()) {
            String energyType = cost.getEnergyType();
            if (isColorlessEnergy(energyType)) {
                colorlessEnergyRequired = colorlessEnergyRequired + cost.getQuantity();
                continue;
            }

            int availableTypedEnergy = typedEnergyCount(energyByType, energyType);
            if (availableTypedEnergy >= cost.getQuantity()) {
                consumeTypedEnergy(energyByType, energyType, cost.getQuantity());
                usedEnergy = usedEnergy + cost.getQuantity();
                continue;
            }

            int missingEnergy = cost.getQuantity() - availableTypedEnergy;
            if (remainingWildcardEnergy < missingEnergy) {
                return false;
            }
            consumeTypedEnergy(energyByType, energyType, availableTypedEnergy);
            remainingWildcardEnergy = remainingWildcardEnergy - missingEnergy;
            usedEnergy = usedEnergy + cost.getQuantity();
        }
        return totalEnergy - usedEnergy >= colorlessEnergyRequired;
    }

    private boolean isColorlessEnergy(String energyType) {
        return energyType != null && COLORLESS_ENERGY_TYPE.equalsIgnoreCase(energyType);
    }

    private int typedEnergyCount(Map<String, Integer> energyByType, String energyType) {
        if (energyType == null) {
            return 0;
        }

        return energyByType.getOrDefault(energyType.toLowerCase(), 0);
    }

    private void consumeTypedEnergy(Map<String, Integer> energyByType, String energyType, int quantity) {
        if (energyType == null || quantity <= 0) {
            return;
        }

        String key = energyType.toLowerCase();
        int remainingEnergy = energyByType.getOrDefault(key, 0) - quantity;
        if (remainingEnergy <= 0) {
            energyByType.remove(key);
            return;
        }

        energyByType.put(key, remainingEnergy);
    }

    private void incrementEnergyCount(Map<String, Integer> energyByType, String energyType) {
        Integer currentCount = energyByType.get(energyType);
        if (currentCount == null) {
            energyByType.put(energyType, 1);
            return;
        }

        energyByType.put(energyType, currentCount + 1);
    }

    private int specialEnergyWildcardValue(PokemonAttachedCard attachedCard) {
        Card energyCard = findCardOrNull(attachedCard.getGameCardInstance().getCardId());
        if (energyCard != null && DOUBLE_COLORLESS_ENERGY_EXTERNAL_ID.equals(energyCard.getExternalId())) {
            return DOUBLE_COLORLESS_ENERGY_VALUE;
        }

        return 1;
    }

    private String resolveBasicEnergyType(Card energyCard) {
        if (energyCard.getPokemonType() != null) {
            return energyCard.getPokemonType();
        }

        String name = energyCard.getName();
        if (name != null && name.endsWith(" Energy")) {
            return name.substring(0, name.length() - " Energy".length());
        }

        return null;
    }

    private Card findCardOrNull(UUID cardId) {
        try {
            return cardService.getCardEntityById(cardId);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
