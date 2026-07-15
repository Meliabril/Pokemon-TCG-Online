package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityUsageTracker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AbilityUsageTrackerImpl implements AbilityUsageTracker {

    @Override
    public boolean wasUsedThisTurn(Game game, UUID pokemonInPlayId, AbilityCode abilityCode) {
        if (game == null || pokemonInPlayId == null || abilityCode == null) {
            return false;
        }
        return usageEntries(game).contains(entryKey(pokemonInPlayId, abilityCode));
    }

    @Override
    public void markUsed(Game game, UUID pokemonInPlayId, AbilityCode abilityCode) {
        if (game == null || pokemonInPlayId == null || abilityCode == null) {
            return;
        }

        List<String> entries = new ArrayList<>(usageEntries(game));
        String entry = entryKey(pokemonInPlayId, abilityCode);
        if (!entries.contains(entry)) {
            entries.add(entry);
        }

        Map<String, Object> resolutionState = new LinkedHashMap<>(game.getResolutionState() == null ? Map.of() : game.getResolutionState());
        resolutionState.put(ABILITY_USAGE_KEY, List.copyOf(entries));
        game.setResolutionState(Map.copyOf(resolutionState));
    }

    @Override
    public void clearTurnUsage(Game game) {
        if (game == null || game.getResolutionState() == null || !game.getResolutionState().containsKey(ABILITY_USAGE_KEY)) {
            return;
        }

        Map<String, Object> resolutionState = new LinkedHashMap<>(game.getResolutionState());
        resolutionState.remove(ABILITY_USAGE_KEY);
        game.setResolutionState(Map.copyOf(resolutionState));
    }

    private List<String> usageEntries(Game game) {
        if (game.getResolutionState() == null) {
            return List.of();
        }
        Object value = game.getResolutionState().get(ABILITY_USAGE_KEY);
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (Object entry : entries) {
            if (entry != null) {
                normalized.add(String.valueOf(entry));
            }
        }
        return List.copyOf(normalized);
    }

    private String entryKey(UUID pokemonInPlayId, AbilityCode abilityCode) {
        return pokemonInPlayId + ":" + abilityCode.name();
    }
}
