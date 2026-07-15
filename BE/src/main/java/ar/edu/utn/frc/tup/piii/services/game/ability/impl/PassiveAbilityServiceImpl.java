package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityCatalogService;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.effect.DamageCounterEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameRandomService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassiveAbilityServiceImpl implements PassiveAbilityService {

    private static final int ACTIVE_SLOT_POSITION = 0;
    private static final int FUR_COAT_REDUCTION = 20;
    private static final int SPIKY_SHIELD_COUNTERS = 3;
    private static final int DESTINY_BURST_COUNTERS = 5;

    private final AbilityCatalogService abilityCatalogService;
    private final CardService cardService;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final PokemonAttachedCardStateService pokemonAttachedCardStateService;
    private final SpecialConditionStateService specialConditionStateService;
    private final DamageCounterEffectService damageCounterEffectService;
    private final GameRandomService gameRandomService;
    private final GameEventFactory gameEventFactory;

    @Override
    public int applyIncomingAttackDamageModifiers(Game game, PokemonInPlay defenderPokemon, int damageAfterWeaknessAndResistance) {
        if (damageAfterWeaknessAndResistance <= 0 || defenderPokemon == null || game == null) {
            return damageAfterWeaknessAndResistance;
        }
        if (hasPassiveAbility(game, defenderPokemon, AbilityCode.FUR_COAT)) {
            return Math.max(0, damageAfterWeaknessAndResistance - FUR_COAT_REDUCTION);
        }
        return damageAfterWeaknessAndResistance;
    }

    @Override
    public PassiveAbilityResolution applyAfterAttackDamage(
            Game game,
            UUID attackerUserId,
            UUID defenderUserId,
            PokemonInPlay attackerPokemon,
            PokemonInPlay defenderPokemon,
            int finalDamage,
            int nextTurnNumber,
            int stateVersion) {
        if (game == null || attackerPokemon == null || defenderPokemon == null || finalDamage <= 0) {
            return PassiveAbilityResolution.empty();
        }

        List<GameEventDto> events = new ArrayList<>();
        boolean gameFinished = false;
        boolean promotionPending = false;
        UUID winnerUserId = null;

        if (hasPassiveAbility(game, defenderPokemon, AbilityCode.SPIKY_SHIELD) && isActive(defenderPokemon)) {
            PassiveAbilityResolution resolution = placeCountersOnAttacker(
                    game,
                    defenderPokemon,
                    attackerPokemon,
                    defenderUserId,
                    defenderUserId,
                    nextTurnNumber,
                    stateVersion,
                    AbilityCode.SPIKY_SHIELD,
                    SPIKY_SHIELD_COUNTERS,
                    null);
            events.addAll(resolution.events());
            gameFinished = resolution.gameFinished();
            promotionPending = resolution.promotionPending();
            winnerUserId = resolution.winnerUserId();
        }

        if (hasPassiveAbility(game, defenderPokemon, AbilityCode.DESTINY_BURST)
                && isActive(defenderPokemon)
                && isKnockedOut(defenderPokemon)) {
            boolean heads = gameRandomService.flipCoin();
            String coinFlip = heads ? "HEADS" : "TAILS";
            if (heads) {
                PassiveAbilityResolution resolution = placeCountersOnAttacker(
                        game,
                        defenderPokemon,
                        attackerPokemon,
                        defenderUserId,
                        defenderUserId,
                        nextTurnNumber,
                        stateVersion,
                        AbilityCode.DESTINY_BURST,
                        DESTINY_BURST_COUNTERS,
                        coinFlip);
                events.addAll(resolution.events());
                gameFinished = gameFinished || resolution.gameFinished();
                promotionPending = promotionPending || resolution.promotionPending();
                if (winnerUserId == null) {
                    winnerUserId = resolution.winnerUserId();
                }
            } else {
                events.add(passiveAbilityEvent(
                        game,
                        defenderPokemon,
                        attackerPokemon,
                        defenderUserId,
                        stateVersion,
                        AbilityCode.DESTINY_BURST,
                        0,
                        coinFlip));
            }
        }

        return new PassiveAbilityResolution(gameFinished, promotionPending, winnerUserId, List.copyOf(events));
    }

    @Override
    public boolean blocksItemCards(Game game, UUID playerId) {
        if (game == null || playerId == null) {
            return false;
        }
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdOrdered(game.getId())) {
            if (!playerId.equals(pokemon.getOwnerUserId())
                    && isActive(pokemon)
                    && hasPassiveAbility(game, pokemon, AbilityCode.FORESTS_CURSE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean blocksSpecialCondition(Game game, PokemonInPlay targetPokemon, SpecialConditionType conditionType) {
        if (game == null || targetPokemon == null || conditionType == null) {
            return false;
        }
        return protectedBySweetVeil(game, targetPokemon);
    }

    @Override
    public List<GameEventDto> recalculateSweetVeil(Game game, UUID ownerUserId, int stateVersion) {
        if (game == null || ownerUserId == null) {
            return List.of();
        }

        List<GameEventDto> events = new ArrayList<>();
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdAndOwnerUserId(game.getId(), ownerUserId)) {
            if (!protectedBySweetVeil(game, pokemon)) {
                continue;
            }

            List<SpecialConditionType> currentConditions = specialConditionStateService.activeConditionTypes(pokemon.getId());
            if (currentConditions.isEmpty()) {
                continue;
            }

            specialConditionStateService.deleteByPokemonInPlayId(pokemon.getId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("effectType", AbilityCode.SWEET_VEIL.name());
            payload.put("pokemonInPlayId", pokemon.getId().toString());
            payload.put("clearedConditions", currentConditions.stream().map(Enum::name).toList());
            events.add(gameEventFactory.publicEvent(game.getId(), GameEventType.ATTACK_EFFECT_RESOLVED, stateVersion, Map.copyOf(payload)));
        }
        return List.copyOf(events);
    }

    private PassiveAbilityResolution placeCountersOnAttacker(
            Game game,
            PokemonInPlay sourcePokemon,
            PokemonInPlay attackerPokemon,
            UUID rewardPlayerId,
            UUID nextActivePlayerId,
            int nextTurnNumber,
            int stateVersion,
            AbilityCode abilityCode,
            int counters,
            String coinFlip) {
        List<GameEventDto> events = new ArrayList<>();
        events.add(passiveAbilityEvent(
                game,
                sourcePokemon,
                attackerPokemon,
                rewardPlayerId,
                stateVersion,
                abilityCode,
                counters,
                coinFlip));
        DamageCounterEffectService.DamageCounterResult damageResult = damageCounterEffectService.placeDamageCounters(
                game.getId(),
                attackerPokemon,
                counters,
                rewardPlayerId,
                nextActivePlayerId,
                nextTurnNumber,
                stateVersion,
                abilityCode.name());
        events.addAll(damageResult.events());
        return new PassiveAbilityResolution(
                damageResult.gameFinished(),
                damageResult.promotionPending(),
                damageResult.winnerUserId(),
                List.copyOf(events));
    }

    private GameEventDto passiveAbilityEvent(
            Game game,
            PokemonInPlay sourcePokemon,
            PokemonInPlay targetPokemon,
            UUID actorPlayerId,
            int stateVersion,
            AbilityCode abilityCode,
            int counters,
            String coinFlip) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("abilityId", abilityCode.name());
        payload.put("effectType", abilityCode.name());
        payload.put("actorPlayerId", actorPlayerId.toString());
        if (sourcePokemon != null) {
            payload.put("sourcePokemonId", sourcePokemon.getId().toString());
            payload.put("pokemonInPlayId", sourcePokemon.getId().toString());
        }
        if (targetPokemon != null) {
            payload.put("targetPokemonId", targetPokemon.getId().toString());
            payload.put("defenderPokemonInPlayId", targetPokemon.getId().toString());
        }
        payload.put("damageCounters", counters);
        if (coinFlip != null) {
            payload.put("coinFlip", coinFlip);
            payload.put("coinResults", List.of(coinFlip));
        }
        return gameEventFactory.publicEvent(
                game.getId(),
                GameEventType.PASSIVE_ABILITY_TRIGGERED,
                stateVersion,
                Map.copyOf(payload));
    }

    private boolean protectedBySweetVeil(Game game, PokemonInPlay targetPokemon) {
        if (!hasFairyEnergyAttached(targetPokemon)) {
            return false;
        }
        for (PokemonInPlay pokemon : pokemonInPlayStateService.findByGameIdAndOwnerUserId(game.getId(), targetPokemon.getOwnerUserId())) {
            if (hasPassiveAbility(game, pokemon, AbilityCode.SWEET_VEIL)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFairyEnergyAttached(PokemonInPlay pokemon) {
        for (PokemonAttachedCard attachedCard : pokemonAttachedCardStateService.findByPokemonInPlayId(pokemon.getId())) {
            Card card = cardService.getCardEntityById(attachedCard.getGameCardInstance().getCardId());
            if (card.getPokemonType() != null && "Fairy".equalsIgnoreCase(card.getPokemonType())) {
                return true;
            }
            if (card.getName() != null && "Fairy Energy".equalsIgnoreCase(card.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPassiveAbility(Game game, PokemonInPlay pokemon, AbilityCode abilityCode) {
        if (pokemon == null || pokemon.getActiveCardInstance() == null || pokemonAbilitiesDisabled(game, pokemon)) {
            return false;
        }
        Card activeCard = cardService.getCardEntityById(pokemon.getActiveCardInstance().getCardId());
        return abilityCatalogService.find(activeCard.getExternalId(), abilityCode).isPresent();
    }

    private boolean pokemonAbilitiesDisabled(Game game, PokemonInPlay pokemon) {
        Integer disabledUntilTurn = pokemon.getAbilitiesDisabledUntilTurn();
        return disabledUntilTurn != null && game.getTurnNumber() != null && game.getTurnNumber() <= disabledUntilTurn;
    }

    private boolean isActive(PokemonInPlay pokemon) {
        return pokemon.getSlotPosition() != null && pokemon.getSlotPosition() == ACTIVE_SLOT_POSITION;
    }

    private boolean isKnockedOut(PokemonInPlay pokemon) {
        Card activeCard = cardService.getCardEntityById(pokemon.getActiveCardInstance().getCardId());
        int damage = (pokemon.getDamageCounters() == null ? 0 : pokemon.getDamageCounters()) * 10;
        return activeCard.getHp() != null && damage >= activeCard.getHp();
    }
}
