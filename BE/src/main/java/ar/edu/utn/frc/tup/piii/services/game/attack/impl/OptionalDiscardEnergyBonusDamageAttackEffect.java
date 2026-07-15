package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
 * Models attacks like Emolga-EX's Electron Crush: the player may discard one
 * Energy attached to the attacker for a flat damage bonus. Unlike
 * DISCARD_ENERGY (mandatory, used as a cost), this is opt-in via payload and
 * only grants the bonus when an Energy was actually discarded.
 */
@Service
@RequiredArgsConstructor
public class OptionalDiscardEnergyBonusDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "OPTIONAL_DISCARD_ENERGY_BONUS_DAMAGE";
    private static final String USE_BONUS_DAMAGE_PAYLOAD_KEY = "useBonusDamage";

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        if (!Boolean.TRUE.equals(context.payload().get(USE_BONUS_DAMAGE_PAYLOAD_KEY))) {
            return AttackEffectResult.empty();
        }

        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();
        boolean discarded = discardOneEnergy(context, attackerPokemon);
        int damageModifier = discarded ? context.operation().amount() : 0;

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "discardedEnergy", discarded,
                        "damageModifier", damageModifier,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", attackerPokemon.getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private boolean discardOneEnergy(AttackEffectContext context, PokemonInPlay attackerPokemon) {
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(attackerPokemon.getId());
        PokemonAttachedCard energyCard = attachedCards.stream()
                .filter(this::isEnergy)
                .findFirst()
                .orElse(null);
        if (energyCard == null) {
            return false;
        }

        GameCardInstance cardInstance = energyCard.getGameCardInstance();
        cardInstance.setZone(CardZone.DISCARD);
        cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(
                context.resolutionContext().gameId(),
                attackerPokemon.getOwnerUserId(),
                CardZone.DISCARD));
        cardInstance.setFaceDown(false);
        gameCardInstanceStateService.save(cardInstance);
        pokemonAttachedCardStateService.delete(energyCard);
        return true;
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard.getAttachedCardType() == AttachedCardType.BASIC_ENERGY
                || attachedCard.getAttachedCardType() == AttachedCardType.SPECIAL_ENERGY;
    }
}
