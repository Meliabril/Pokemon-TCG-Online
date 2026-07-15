package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectOperation;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackEffectResult;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConditionalOwnBenchCardDamageAttackEffect implements AttackEffect {

    private static final String EFFECT_TYPE = "CONDITIONAL_OWN_BENCH_CARD_DAMAGE";
    private static final int ACTIVE_SLOT_POSITION = 0;

    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final CardService cardService;
    private final GameEventFactory gameEventFactory;

    @Override
    public boolean supports(AttackEffectOperation operation) {
        return operation != null && EFFECT_TYPE.equals(operation.type());
    }

    @Override
    public AttackEffectResult apply(AttackEffectContext context) {
        String expectedCardName = context.operation().cardName();
        boolean conditionMet = ownBenchHasCardNamed(context, expectedCardName);
        int damageModifier = conditionMet ? context.operation().amount() : 0;

        GameEventDto event = gameEventFactory.publicEvent(
                context.resolutionContext().gameId(),
                GameEventType.ATTACK_EFFECT_RESOLVED,
                context.stateVersion(),
                Map.of(
                        "effectType", EFFECT_TYPE,
                        "conditionMet", conditionMet,
                        "damageModifier", damageModifier,
                        "expectedCardName", expectedCardName == null ? "" : expectedCardName,
                        "actorPlayerId", context.resolutionContext().attackerUserId().toString(),
                        "pokemonInPlayId", context.resolutionContext().attackerPokemon().getId().toString()));
        return new AttackEffectResult(damageModifier, false, List.of(event));
    }

    private boolean ownBenchHasCardNamed(AttackEffectContext context, String expectedCardName) {
        if (expectedCardName == null || expectedCardName.isBlank()) {
            return false;
        }

        List<PokemonInPlay> ownPokemon = pokemonInPlayStateService.findByGameIdAndOwnerUserId(
                context.resolutionContext().gameId(),
                context.resolutionContext().attackerUserId());
        for (PokemonInPlay pokemon : ownPokemon) {
            if (pokemon.getSlotPosition() == null || pokemon.getSlotPosition() <= ACTIVE_SLOT_POSITION) {
                continue;
            }

            if (matchesCardName(pokemon.getActiveCardInstance(), expectedCardName)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesCardName(GameCardInstance activeCardInstance, String expectedCardName) {
        if (activeCardInstance == null || activeCardInstance.getCardId() == null) {
            return false;
        }

        Card card = cardService.getCardEntityById(activeCardInstance.getCardId());
        return card != null
                && card.getName() != null
                && card.getName().equalsIgnoreCase(expectedCardName);
    }
}
