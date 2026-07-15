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
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachEnergyFromDiscardToBenchAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "ATTACH_ENERGY_FROM_DISCARD_TO_BENCH";

    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;
    private final GameCardInstanceStateService gameCardInstanceStateService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final CardService cardService;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        String energyType = context.operation().energyType();
        List<String> coinResults = new ArrayList<>();
        int requestedAttachCount = context.operation().coinCount() > 0
                ? flipCoins(context.operation().coinCount(), coinResults)
                : context.operation().amount();
        int attachedCount = attachEnergyCards(context, requestedAttachCount, energyType);

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "coinResults", List.copyOf(coinResults),
                        "attachedCount", attachedCount,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(0, false, List.of(event));
    }

    private int flipCoins(int coinCount, List<String> coinResults) {
        int headsCount = 0;
        for (int i = 0; i < coinCount; i++) {
            boolean heads = gameRandomService.flipCoin();
            coinResults.add(heads ? "HEADS" : "TAILS");
            if (heads) {
                headsCount++;
            }
        }
        return headsCount;
    }

    private int attachEnergyCards(AttackEffectContext context, int requestedAttachCount, String energyType) {
        if (requestedAttachCount <= 0) {
            return 0;
        }

        UUID gameId = context.resolutionContext().gameId();
        UUID attackerUserId = context.resolutionContext().attackerUserId();
        List<PokemonInPlay> benchPokemon = benchPokemon(gameId, attackerUserId);
        if (benchPokemon.isEmpty()) {
            return 0;
        }

        List<GameCardInstance> discardCards = gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(
                gameId,
                attackerUserId,
                CardZone.DISCARD);
        List<GameCardInstance> matchingEnergyCards = discardCards.stream()
                .filter(cardInstance -> isMatchingEnergy(cardInstance, energyType))
                .limit(requestedAttachCount)
                .toList();
        if (matchingEnergyCards.isEmpty()) {
            return 0;
        }

        int nextAttachedPosition = gameCardInstanceStateService.nextZonePosition(gameId, attackerUserId, CardZone.ATTACHED);
        int attachedCount = 0;
        for (GameCardInstance energyCard : matchingEnergyCards) {
            PokemonInPlay targetPokemon = benchPokemon.get(attachedCount % benchPokemon.size());
            energyCard.setZone(CardZone.ATTACHED);
            energyCard.setZonePosition(nextAttachedPosition + attachedCount);
            energyCard.setFaceDown(false);
            gameCardInstanceStateService.save(energyCard);

            PokemonAttachedCard attachedCard = new PokemonAttachedCard();
            attachedCard.setPokemonInPlay(targetPokemon);
            attachedCard.setGameCardInstance(energyCard);
            attachedCard.setAttachedCardType(AttachedCardType.BASIC_ENERGY);
            pokemonAttachedCardStateService.save(attachedCard);
            attachedCount++;
        }

        gameCardInstanceStateService.resequenceZone(gameId, attackerUserId, CardZone.DISCARD);
        return attachedCount;
    }

    private List<PokemonInPlay> benchPokemon(UUID gameId, UUID attackerUserId) {
        return pokemonInPlayStateService.findByGameIdAndOwnerUserId(gameId, attackerUserId).stream()
                .filter(pokemon -> pokemon.getSlotPosition() != null && pokemon.getSlotPosition() > 0)
                .sorted(Comparator.comparing(PokemonInPlay::getSlotPosition))
                .toList();
    }

    private boolean isMatchingEnergy(GameCardInstance cardInstance, String energyType) {
        if (energyType == null || energyType.isBlank()) {
            return false;
        }

        Card card = cardService.getCardEntityById(cardInstance.getCardId());
        if (card == null || !CardCategory.BASIC_ENERGY.equals(card.getCategory())) {
            return false;
        }
        if (card.getPokemonType() != null && card.getPokemonType().equalsIgnoreCase(energyType)) {
            return true;
        }
        return card.getName() != null && card.getName().equalsIgnoreCase(energyType + " Energy");
    }
}
