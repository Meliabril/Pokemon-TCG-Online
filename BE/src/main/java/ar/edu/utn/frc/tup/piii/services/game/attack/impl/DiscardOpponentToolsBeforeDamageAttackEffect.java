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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Models attacks like Skarmory-EX's Joust: before damage is dealt, discard
 * every Pokemon Tool card attached to the defending Pokemon.
 */
@Service
@RequiredArgsConstructor
public class DiscardOpponentToolsBeforeDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DISCARD_OPPONENT_TOOLS_BEFORE_DAMAGE";

    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        PokemonInPlay defenderPokemon = context.targetPokemon();
        List<PokemonAttachedCard> attachedCards = pokemonAttachedCardStateService.findByPokemonInPlayId(defenderPokemon.getId());
        List<String> discardedCardIds = new ArrayList<>();
        for (PokemonAttachedCard attachedCard : attachedCards) {
            if (attachedCard.getAttachedCardType() != AttachedCardType.POKEMON_TOOL) {
                continue;
            }

            GameCardInstance cardInstance = attachedCard.getGameCardInstance();
            cardInstance.setZone(CardZone.DISCARD);
            cardInstance.setZonePosition(gameCardInstanceStateService.nextZonePosition(
                    context.resolutionContext().gameId(),
                    defenderPokemon.getOwnerUserId(),
                    CardZone.DISCARD));
            cardInstance.setFaceDown(false);
            gameCardInstanceStateService.save(cardInstance);
            pokemonAttachedCardStateService.delete(attachedCard);
            discardedCardIds.add(cardInstance.getCardId().toString());
        }

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "pokemonInPlayId", defenderPokemon.getId().toString(),
                        "discardedToolCount", discardedCardIds.size(),
                        "discardedCardIds", List.copyOf(discardedCardIds)));
        return new AttackEffectResult(0, false, List.of(event));
    }
}
