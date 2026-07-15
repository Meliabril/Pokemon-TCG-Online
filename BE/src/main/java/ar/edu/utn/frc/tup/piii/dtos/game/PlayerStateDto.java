package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.SpecialConditionType;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PlayerStateDto(
        int benchPokemonCount,
        List<SpecialConditionType> activePokemonConditions,
        List<UUID> cardIdsInHand,
        List<UUID> cardInstanceIdsInHand,
        Set<UUID> affordableAttackIds,
        int mulliganCount,
        boolean mulliganNoticePending,
        boolean mulliganFlowActive,
        boolean mulliganReadyForInitialSelection,
        int mulliganRoundNumber,
        boolean mulliganCurrentPlayer,
        boolean initialPokemonSelectionSubmitted,
        UUID initialActiveCardInstanceId,
        List<UUID> initialBenchCardInstanceIds) {

    public PlayerStateDto {
        activePokemonConditions = copyConditions(activePokemonConditions);
        cardIdsInHand = copyUuids(cardIdsInHand);
        cardInstanceIdsInHand = copyUuids(cardInstanceIdsInHand);
        affordableAttackIds = copyUuidSet(affordableAttackIds);
        initialBenchCardInstanceIds = copyUuids(initialBenchCardInstanceIds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .benchPokemonCount(benchPokemonCount)
                .activePokemonConditions(activePokemonConditions)
                .cardIdsInHand(cardIdsInHand)
                .cardInstanceIdsInHand(cardInstanceIdsInHand)
                .affordableAttackIds(affordableAttackIds)
                .mulliganCount(mulliganCount)
                .mulliganNoticePending(mulliganNoticePending)
                .mulliganFlowActive(mulliganFlowActive)
                .mulliganReadyForInitialSelection(mulliganReadyForInitialSelection)
                .mulliganRoundNumber(mulliganRoundNumber)
                .mulliganCurrentPlayer(mulliganCurrentPlayer)
                .initialPokemonSelectionSubmitted(initialPokemonSelectionSubmitted)
                .initialActiveCardInstanceId(initialActiveCardInstanceId)
                .initialBenchCardInstanceIds(initialBenchCardInstanceIds);
    }

    public static final class Builder {

        private int benchPokemonCount;
        private List<SpecialConditionType> activePokemonConditions;
        private List<UUID> cardIdsInHand;
        private List<UUID> cardInstanceIdsInHand;
        private Set<UUID> affordableAttackIds;
        private int mulliganCount;
        private boolean mulliganNoticePending;
        private boolean mulliganFlowActive;
        private boolean mulliganReadyForInitialSelection;
        private int mulliganRoundNumber;
        private boolean mulliganCurrentPlayer;
        private boolean initialPokemonSelectionSubmitted;
        private UUID initialActiveCardInstanceId;
        private List<UUID> initialBenchCardInstanceIds;

        private Builder() {
        }

        public Builder benchPokemonCount(int benchPokemonCount) {
            this.benchPokemonCount = benchPokemonCount;
            return this;
        }

        public Builder activePokemonConditions(List<SpecialConditionType> activePokemonConditions) {
            this.activePokemonConditions = activePokemonConditions;
            return this;
        }

        public Builder cardIdsInHand(List<UUID> cardIdsInHand) {
            this.cardIdsInHand = cardIdsInHand;
            return this;
        }

        public Builder cardInstanceIdsInHand(List<UUID> cardInstanceIdsInHand) {
            this.cardInstanceIdsInHand = cardInstanceIdsInHand;
            return this;
        }

        public Builder affordableAttackIds(Set<UUID> affordableAttackIds) {
            this.affordableAttackIds = affordableAttackIds;
            return this;
        }

        public Builder mulliganCount(int mulliganCount) {
            this.mulliganCount = mulliganCount;
            return this;
        }

        public Builder mulliganNoticePending(boolean mulliganNoticePending) {
            this.mulliganNoticePending = mulliganNoticePending;
            return this;
        }

        public Builder mulliganFlowActive(boolean mulliganFlowActive) {
            this.mulliganFlowActive = mulliganFlowActive;
            return this;
        }

        public Builder mulliganReadyForInitialSelection(boolean mulliganReadyForInitialSelection) {
            this.mulliganReadyForInitialSelection = mulliganReadyForInitialSelection;
            return this;
        }

        public Builder mulliganRoundNumber(int mulliganRoundNumber) {
            this.mulliganRoundNumber = mulliganRoundNumber;
            return this;
        }

        public Builder mulliganCurrentPlayer(boolean mulliganCurrentPlayer) {
            this.mulliganCurrentPlayer = mulliganCurrentPlayer;
            return this;
        }

        public Builder initialPokemonSelectionSubmitted(boolean initialPokemonSelectionSubmitted) {
            this.initialPokemonSelectionSubmitted = initialPokemonSelectionSubmitted;
            return this;
        }

        public Builder initialActiveCardInstanceId(UUID initialActiveCardInstanceId) {
            this.initialActiveCardInstanceId = initialActiveCardInstanceId;
            return this;
        }

        public Builder initialBenchCardInstanceIds(List<UUID> initialBenchCardInstanceIds) {
            this.initialBenchCardInstanceIds = initialBenchCardInstanceIds;
            return this;
        }

        public PlayerStateDto build() {
            return new PlayerStateDto(
                    benchPokemonCount,
                    activePokemonConditions,
                    cardIdsInHand,
                    cardInstanceIdsInHand,
                    affordableAttackIds,
                    mulliganCount,
                    mulliganNoticePending,
                    mulliganFlowActive,
                    mulliganReadyForInitialSelection,
                    mulliganRoundNumber,
                    mulliganCurrentPlayer,
                    initialPokemonSelectionSubmitted,
                    initialActiveCardInstanceId,
                    initialBenchCardInstanceIds);
        }
    }

    private static List<SpecialConditionType> copyConditions(List<SpecialConditionType> source) {
        if (source == null) {
            return List.of();
        }

        return List.copyOf(source);
    }

    private static List<UUID> copyUuids(List<UUID> source) {
        if (source == null) {
            return List.of();
        }

        return List.copyOf(source);
    }

    private static Set<UUID> copyUuidSet(Set<UUID> source) {
        if (source == null) {
            return Set.of();
        }

        return Set.copyOf(source);
    }
}
