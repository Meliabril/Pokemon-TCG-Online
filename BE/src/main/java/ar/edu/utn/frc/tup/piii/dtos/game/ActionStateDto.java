package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ActionStateDto(
        List<GameActionType> availableActions,
        Set<UUID> processedClientActionIds) {

    public ActionStateDto {
        availableActions = copyActions(availableActions);
        processedClientActionIds = copyUuids(processedClientActionIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .availableActions(availableActions)
                .processedClientActionIds(processedClientActionIds);
    }

    public static final class Builder {

        private List<GameActionType> availableActions;
        private Set<UUID> processedClientActionIds;

        private Builder() {
        }

        public Builder availableActions(List<GameActionType> availableActions) {
            this.availableActions = availableActions;
            return this;
        }

        public Builder processedClientActionIds(Set<UUID> processedClientActionIds) {
            this.processedClientActionIds = processedClientActionIds;
            return this;
        }

        public ActionStateDto build() {
            return new ActionStateDto(availableActions, processedClientActionIds);
        }
    }

    private static List<GameActionType> copyActions(List<GameActionType> source) {
        if (source == null) {
            return List.of();
        }

        return List.copyOf(source);
    }

    private static Set<UUID> copyUuids(Set<UUID> source) {
        if (source == null) {
            return Set.of();
        }

        return Set.copyOf(source);
    }
}
