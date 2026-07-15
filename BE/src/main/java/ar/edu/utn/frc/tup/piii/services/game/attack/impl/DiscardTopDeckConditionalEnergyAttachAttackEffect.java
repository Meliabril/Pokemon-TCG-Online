package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscardTopDeckConditionalEnergyAttachAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "DISCARD_TOP_DECK_CONDITIONAL_ENERGY_ATTACH";
    private static final String FIGHTING = "Fighting";

    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<GameCardInstance> deckCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                attackerUserId,
                CardZone.DECK);
        if (deckCards.isEmpty()) {
            return AttackEffectResult.empty();
        }

        GameCardInstance topCard = deckCards.get(0);
        boolean isFightingEnergy = isFightingEnergy(topCard);
        if (isFightingEnergy) {
            attachToAttacker(context, topCard);
        } else {
            discard(gameId, attackerUserId, topCard);
        }
        gameCardInstanceStateService.resequenceZone(gameId, attackerUserId, CardZone.DECK);

        GameEventDto event = gameEventFactory.publicEvent(
                gameId,
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "cardInstanceId", topCard.getId().toString(),
                        "attached", isFightingEnergy,
                        "actorPlayerId", attackerUserId.toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private void attachToAttacker(AttackEffectContext context, GameCardInstance topCard) {
        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        PokemonInPlay attackerPokemon = context.resolutionContext().attackerPokemon();

        topCard.setZone(CardZone.ATTACHED);
        topCard.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED));
        topCard.setFaceDown(false);
        gameCardInstanceStateService.save(topCard);

        PokemonAttachedCard attachedCard = new PokemonAttachedCard();
        attachedCard.setPokemonInPlay(attackerPokemon);
        attachedCard.setGameCardInstance(topCard);
        attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
        pokemonAttachedCardStateService.save(attachedCard);
    }

    private void discard(UUID gameId, UUID attackerUserId, GameCardInstance topCard) {
        topCard.setZone(CardZone.DISCARD);
        topCard.setZonePosition(gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.DISCARD));
        topCard.setFaceDown(false);
        gameCardInstanceStateService.save(topCard);
    }

    private boolean isFightingEnergy(GameCardInstance cardInstance) {
        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card == null || !CardCategory.BASIC_ENERGY.equals(card.getCategory())) {
            return false;
        }
        if (card.getPokemonType() != null && card.getPokemonType().equalsIgnoreCase(FIGHTING)) {
            return true;
        }
        return card.getName() != null && card.getName().equalsIgnoreCase(FIGHTING + " Energy");
    }
}
