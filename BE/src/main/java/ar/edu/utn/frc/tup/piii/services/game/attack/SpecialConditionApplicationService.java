package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;

import java.util.List;
import java.util.UUID;

public interface SpecialConditionApplicationService {

    List<GameEventDto> applyCondition(
            UUID gameId,
            UUID sourcePlayerId,
            PokemonInPlay targetPokemon,
            SpecialConditionType conditionType,
            int currentTurnNumber,
            int stateVersion);
}
