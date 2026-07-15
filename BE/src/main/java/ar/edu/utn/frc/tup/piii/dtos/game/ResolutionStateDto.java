package ar.edu.utn.frc.tup.piii.dtos.game;

import java.util.UUID;
import java.util.Map;

public record ResolutionStateDto(
        String resolutionType,
        UUID playerToPromoteId,
        UUID nextActivePlayerId,
        int nextTurnNumber,
        UUID pendingChoicePlayerId,
        String pendingChoiceType,
        Map<String, Object> pendingChoicePayload,
        UUID turnEndingPlayerId) {

    public static final String PROMOTION_REQUIRED = "PROMOTION_REQUIRED";
    public static final String SUDDEN_DEATH_REQUIRED = "SUDDEN_DEATH_REQUIRED";
    public static final String ATTACK_CHOICE_REQUIRED = "ATTACK_CHOICE_REQUIRED";
    public static final String RESOLUTION_TYPE_KEY = "resolutionType";
    public static final String PLAYER_TO_PROMOTE_ID_KEY = "playerToPromoteId";
    public static final String NEXT_ACTIVE_PLAYER_ID_KEY = "nextActivePlayerId";
    public static final String NEXT_TURN_NUMBER_KEY = "nextTurnNumber";
    public static final String PENDING_CHOICE_PLAYER_ID_KEY = "pendingChoicePlayerId";
    public static final String PENDING_CHOICE_TYPE_KEY = "pendingChoiceType";
    public static final String PENDING_CHOICE_PAYLOAD_KEY = "pendingChoicePayload";
    public static final String TURN_ENDING_PLAYER_ID_KEY = "turnEndingPlayerId";

    public ResolutionStateDto {
        pendingChoicePayload = pendingChoicePayload == null ? Map.of() : Map.copyOf(pendingChoicePayload);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .resolutionType(resolutionType)
                .playerToPromoteId(playerToPromoteId)
                .nextActivePlayerId(nextActivePlayerId)
                .nextTurnNumber(nextTurnNumber)
                .pendingChoicePlayerId(pendingChoicePlayerId)
                .pendingChoiceType(pendingChoiceType)
                .pendingChoicePayload(pendingChoicePayload)
                .turnEndingPlayerId(turnEndingPlayerId);
    }

    public boolean hasPendingPromotion() {
        return PROMOTION_REQUIRED.equals(resolutionType) && playerToPromoteId != null;
    }

    public boolean hasPendingSuddenDeath() {
        return SUDDEN_DEATH_REQUIRED.equals(resolutionType);
    }

    public boolean hasPendingAttackChoice() {
        return ATTACK_CHOICE_REQUIRED.equals(resolutionType) && pendingChoicePlayerId != null;
    }

    /**
     * The player whose turn is ending once the pending attack choice resolves. This is normally
     * the attacker who declared the attack, even when the choice itself must be resolved by the
     * defender (e.g. Mental Trash, where the defender - not the attacker - picks which cards of
     * their own hand to discard). Falls back to pendingChoicePlayerId for older resolution states
     * that never recorded this field separately, since for every choice type prior to that the two
     * always coincided.
     */
    public UUID effectiveTurnEndingPlayerId() {
        return turnEndingPlayerId != null ? turnEndingPlayerId : pendingChoicePlayerId;
    }

    public static final class Builder {

        private String resolutionType;
        private UUID playerToPromoteId;
        private UUID nextActivePlayerId;
        private int nextTurnNumber;
        private UUID pendingChoicePlayerId;
        private String pendingChoiceType;
        private Map<String, Object> pendingChoicePayload;
        private UUID turnEndingPlayerId;

        private Builder() {
        }

        public Builder resolutionType(String resolutionType) {
            this.resolutionType = resolutionType;
            return this;
        }

        public Builder playerToPromoteId(UUID playerToPromoteId) {
            this.playerToPromoteId = playerToPromoteId;
            return this;
        }

        public Builder nextActivePlayerId(UUID nextActivePlayerId) {
            this.nextActivePlayerId = nextActivePlayerId;
            return this;
        }

        public Builder nextTurnNumber(int nextTurnNumber) {
            this.nextTurnNumber = nextTurnNumber;
            return this;
        }

        public Builder pendingChoicePlayerId(UUID pendingChoicePlayerId) {
            this.pendingChoicePlayerId = pendingChoicePlayerId;
            return this;
        }

        public Builder pendingChoiceType(String pendingChoiceType) {
            this.pendingChoiceType = pendingChoiceType;
            return this;
        }

        public Builder pendingChoicePayload(Map<String, Object> pendingChoicePayload) {
            this.pendingChoicePayload = pendingChoicePayload;
            return this;
        }

        public Builder turnEndingPlayerId(UUID turnEndingPlayerId) {
            this.turnEndingPlayerId = turnEndingPlayerId;
            return this;
        }

        public ResolutionStateDto build() {
            return new ResolutionStateDto(
                    resolutionType,
                    playerToPromoteId,
                    nextActivePlayerId,
                    nextTurnNumber,
                    pendingChoicePlayerId,
                    pendingChoiceType,
                    pendingChoicePayload,
                    turnEndingPlayerId);
        }
    }
}
