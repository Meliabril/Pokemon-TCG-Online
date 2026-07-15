package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardResistance;
import ar.edu.utn.frc.tup.piii.entities.CardWeakness;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationRequest;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculationResult;
import ar.edu.utn.frc.tup.piii.services.game.attack.DamageCalculatorService;
import ar.edu.utn.frc.tup.piii.services.game.stadium.StadiumModifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DamageCalculatorServiceImpl implements DamageCalculatorService {

    private final PassiveAbilityService passiveAbilityService;
    private final StadiumModifierService stadiumModifierService;

    @Override
    public DamageCalculationResult calculateDamage(DamageCalculationRequest request) {
        Attack attack = request.attack();
        Card attackerCard = request.attackerCard();
        Card defenderCard = request.defenderCard();

        int damage = 0;
        if (attack != null && attack.getBaseDamage() != null) {
            damage = attack.getBaseDamage();
        }
        int baseDamage = damage;

        damage = damage + request.attackerModifier();
        if (damage < 0) {
            damage = 0;
        }
        int afterAttackerModifiers = damage;

        if (!request.ignoreWeakness()
                && (request.game() == null || !stadiumModifierService.isWeaknessNegated(
                        request.game().getId(),
                        request.defenderPokemon()))) {
            damage = applyWeakness(damage, attackerCard, defenderCard);
        }
        int afterWeakness = damage;

        if (!request.ignoreResistance()) {
            damage = applyResistance(damage, attackerCard, defenderCard);
        }
        if (!request.ignoreDefenderEffects()) {
            damage = passiveAbilityService.applyIncomingAttackDamageModifiers(
                    request.game(),
                    request.defenderPokemon(),
                    damage);
        }
        int afterResistance = damage;

        if (!request.ignoreDefenderEffects()) {
            damage = damage + request.defenderModifier();
        }
        if (damage < 0) {
            damage = 0;
        }

        return new DamageCalculationResult(
                baseDamage,
                afterAttackerModifiers,
                afterWeakness,
                afterResistance,
                damage,
                damage / 10);
    }

    private int applyWeakness(int damage, Card attackerCard, Card defenderCard) {
        if (attackerCard == null || defenderCard == null || attackerCard.getPokemonType() == null) {
            return damage;
        }
        for (CardWeakness weakness : defenderCard.getWeaknesses()) {
            if (attackerCard.getPokemonType().equalsIgnoreCase(weakness.getEnergyType())) {
                return damage * parseWeaknessMultiplier(weakness.getMultiplier());
            }
        }
        return damage;
    }

    private int applyResistance(int damage, Card attackerCard, Card defenderCard) {
        if (attackerCard == null || defenderCard == null || attackerCard.getPokemonType() == null) {
            return damage;
        }
        for (CardResistance resistance : defenderCard.getResistances()) {
            if (attackerCard.getPokemonType().equalsIgnoreCase(resistance.getEnergyType())) {
                return Math.max(0, damage + parseResistanceValue(resistance.getValue()));
            }
        }
        return damage;
    }

    private int parseWeaknessMultiplier(String multiplier) {
        if (multiplier == null || multiplier.isBlank()) {
            return 2;
        }
        String normalized = multiplier.replace("x", "").replace("X", "").replace("ÃƒÆ’Ã¢â‚¬â€", "").trim();
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            return 2;
        }
    }

    private int parseResistanceValue(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.replace("+", "").trim();
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
