package ar.edu.utn.frc.tup.piii.dtos.game;

import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;

import java.time.Instant;
import java.util.UUID;

public record TurnContextDto(
        TurnPhase currentPhase,
        int turnNumber,
        UUID activePlayerId,
        UUID playerWhoWentFirstId,
        Instant turnStartedAt,
        boolean energyAttachedThisTurn,
        boolean supporterPlayedThisTurn,
        boolean retreatedThisTurn) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .currentPhase(currentPhase)
                .turnNumber(turnNumber)
                .activePlayerId(activePlayerId)
                .playerWhoWentFirstId(playerWhoWentFirstId)
                .turnStartedAt(turnStartedAt)
                .energyAttachedThisTurn(energyAttachedThisTurn)
                .supporterPlayedThisTurn(supporterPlayedThisTurn)
                .retreatedThisTurn(retreatedThisTurn);
    }

    public static final class Builder {

        private TurnPhase currentPhase;
        private int turnNumber;
        private UUID activePlayerId;
        private UUID playerWhoWentFirstId;
        private Instant turnStartedAt;
        private boolean energyAttachedThisTurn;
        private boolean supporterPlayedThisTurn;
        private boolean retreatedThisTurn;

        private Builder() {
        }

        public Builder currentPhase(TurnPhase currentPhase) {
            this.currentPhase = currentPhase;
            return this;
        }

        public Builder turnNumber(int turnNumber) {
            this.turnNumber = turnNumber;
            return this;
        }

        public Builder activePlayerId(UUID activePlayerId) {
            this.activePlayerId = activePlayerId;
            return this;
        }

        public Builder playerWhoWentFirstId(UUID playerWhoWentFirstId) {
            this.playerWhoWentFirstId = playerWhoWentFirstId;
            return this;
        }

        public Builder turnStartedAt(Instant turnStartedAt) {
            this.turnStartedAt = turnStartedAt;
            return this;
        }

        public Builder energyAttachedThisTurn(boolean energyAttachedThisTurn) {
            this.energyAttachedThisTurn = energyAttachedThisTurn;
            return this;
        }

        public Builder supporterPlayedThisTurn(boolean supporterPlayedThisTurn) {
            this.supporterPlayedThisTurn = supporterPlayedThisTurn;
            return this;
        }

        public Builder retreatedThisTurn(boolean retreatedThisTurn) {
            this.retreatedThisTurn = retreatedThisTurn;
            return this;
        }

        public TurnContextDto build() {
            return new TurnContextDto(
                    currentPhase,
                    turnNumber,
                    activePlayerId,
                    playerWhoWentFirstId,
                    turnStartedAt,
                    energyAttachedThisTurn,
                    supporterPlayedThisTurn,
                    retreatedThisTurn);
        }
    }
}
