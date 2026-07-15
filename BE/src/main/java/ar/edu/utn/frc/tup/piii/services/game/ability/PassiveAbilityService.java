package ar.edu.utn.frc.tup.piii.services.game.ability;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface PassiveAbilityService {

    int applyIncomingAttackDamageModifiers(Game game, PokemonInPlay defenderPokemon, int damageAfterWeaknessAndResistance);

    PassiveAbilityResolution applyAfterAttackDamage(
            Game game,
            UUID attackerUserId,
            UUID defenderUserId,
            PokemonInPlay attackerPokemon,
            PokemonInPlay defenderPokemon,
            int finalDamage,
            int nextTurnNumber,
            int stateVersion);

    boolean blocksItemCards(Game game, UUID playerId);

    boolean blocksSpecialCondition(Game game, PokemonInPlay targetPokemon, SpecialConditionType conditionType);

    List<GameEventDto> recalculateSweetVeil(Game game, UUID ownerUserId, int stateVersion);

    record PassiveAbilityResolution(
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId,
            List<GameEventDto> events) {

        public PassiveAbilityResolution {
            events = events == null ? List.of() : List.copyOf(events);
        }

        public static PassiveAbilityResolution empty() {
            return new PassiveAbilityResolution(false, false, null, List.of());
        }
    }
}
