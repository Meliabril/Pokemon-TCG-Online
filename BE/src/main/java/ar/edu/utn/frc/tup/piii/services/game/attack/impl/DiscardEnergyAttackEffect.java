package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.effect.DiscardCardEffectService;
import ar.edu.utn.frc.tup.piii.services.game.effect.impl.DiscardCardEffectServiceImpl;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DiscardEnergyAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DISCARD_ENERGY";

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final DiscardCardEffectService discardCardEffectService;
    private final GameEventFactory gameEventFactory;
    private final CardService cardService;

    public DiscardEnergyAttackEffect(
            PokemonAttachedCardStateService pokemonAttachedCardStateService,
            GameCardInstanceStateService gameCardInstanceStateService,
            GameEventFactory gameEventFactory,
            CardService cardService) {
        this(pokemonAttachedCardStateService, new DiscardCardEffectServiceImpl(gameCardInstanceStateService, pokemonAttachedCardStateService), gameEventFactory, cardService);
    }

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay targetPokemon = targetPokemon(context);
        UUID ownerUserId = targetPokemon.getOwnerUserId();
        int amount = context.operation().amount();
        if (amount <= 0) {
            return AttackEffectResult.empty();
        }

        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(targetPokemon.getId());
        List<String> discardedCardIds = new ArrayList<>();
        int discardedCount = 0;
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (discardedCount >= amount) {
                break;
            }
            if (!matchesEnergy(attachedCard, context.operation().energyType())) {
                continue;
            }

            GameCardInstance cardInstance = discardCardEffectService.discardAttachedCard(
                    context.resolutionContext().gameId(),
                    ownerUserId,
                    attachedCard);
            discardedCardIds.add(cardInstance.getCardId().toString());
            discardedCount++;
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", targetPokemon.getId().toString(),
                        "discardedEnergyCount", discardedCount,
                        "discardedCardIds", List.copyOf(discardedCardIds)));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private PokemonInPlay targetPokemon(AttackEffectContext context) {
        if ("DEFENDER".equals(context.operation().target())) {
            return context.targetPokemon();
        }

        return context.resolutionContext().attackerPokemon();
    }

    private boolean isEnergy(PokemonAttachedCard attachedCard) {
        return attachedCard.getAttachedCardType() == AttachedCardType.BASIC_ENERGY
                || attachedCard.getAttachedCardType() == AttachedCardType.SPECIAL_ENERGY;
    }

    private boolean matchesEnergy(PokemonAttachedCard attachedCard, String energyType) {
        if (!isEnergy(attachedCard)) {
            return false;
        }
        if (energyType == null || energyType.isBlank()) {
            return true;
        }
        GameCardInstance cardInstance = attachedCard.getGameCardInstance();
        if (cardInstance == null) {
            return false;
        }
        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card == null) {
            return false;
        }
        if (card.getPokemonType() != null && energyType.equalsIgnoreCase(card.getPokemonType())) {
            return true;
        }
        String expectedEnergyName = energyType + " Energy";
        return card.getName() != null && expectedEnergyName.equalsIgnoreCase(card.getName());
    }
}
