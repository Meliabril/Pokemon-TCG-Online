package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.entities.SpecialCondition;
import ar.edu.utn.frc.tup.piii.services.game.ability.PassiveAbilityService;
import ar.edu.utn.frc.tup.piii.services.game.attack.SpecialConditionApplicationService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameLookupService;
import ar.edu.utn.frc.tup.piii.services.game.state.SpecialConditionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpecialConditionApplicationServiceImpl implements SpecialConditionApplicationService {

    private static final Set<SpecialConditionType> EXCLUSIVE_CONDITIONS = EnumSet.of(
            SpecialConditionType.ASLEEP,
            SpecialConditionType.CONFUSED,
            SpecialConditionType.PARALYZED);

    private final SpecialConditionStateService specialConditionStateService;
    private final GameEventFactory gameEventFactory;
    private final PassiveAbilityService passiveAbilityService;
    private final GameLookupService gameLookupService;

    @Override
    public List<GameEventDto> applyCondition(
            UUID gameId,
            UUID sourcePlayerId,
            PokemonInPlay targetPokemon,
            SpecialConditionType conditionType,
            int currentTurnNumber,
            int stateVersion) {
        if (conditionType == null) {
            return List.of();
        }
        if (passiveAbilityService.blocksSpecialCondition(
                gameLookupService.getRequiredGame(gameId),
                targetPokemon,
                conditionType)) {
            return List.of();
        }

        if (EXCLUSIVE_CONDITIONS.contains(conditionType)) {
            specialConditionStateService.clearConditions(targetPokemon.getId(), EXCLUSIVE_CONDITIONS);
        } else {
            Optional<SpecialCondition> existingCondition =
                    specialConditionStateService.findByPokemonInPlayIdAndConditionType(targetPokemon.getId(), conditionType);
            if (existingCondition.isPresent()) {
                return List.of();
            }
        }

        SpecialCondition specialCondition = new SpecialCondition();
        specialCondition.setPokemonInPlay(targetPokemon);
        specialCondition.setConditionType(conditionType);
        specialCondition.setAppliedTurn(currentTurnNumber);
        specialConditionStateService.save(specialCondition);

        return List.of(gameEventFactory.publicEvent(
                gameId,
                GameEventType.STATUS_APPLIED,
                stateVersion,
                Map.of(
                        "playerId", sourcePlayerId.toString(),
                        "pokemonInPlayId", targetPokemon.getId().toString(),
                        "conditionType", conditionType.name())));
    }
}
