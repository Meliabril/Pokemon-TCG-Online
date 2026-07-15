package ar.edu.utn.frc.tup.piii.services.game.attack;

import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;

import java.util.List;
import java.util.UUID;

public interface ConfusionResolutionService {

    ConfusionResolutionResult resolveBeforeAttack(
            AttackResolutionContext context,
            int currentTurnNumber,
            int nextTurnNumber,
            int stateVersion);

    record ConfusionResolutionResult(
            boolean attackCanProceed,
            boolean gameFinished,
            boolean promotionPending,
            UUID winnerUserId,
            List<GameEventDto> events) {

        public ConfusionResolutionResult {
            if (events == null) {
                events = List.of();
            } else {
                events = List.copyOf(events);
            }
        }

        public static ConfusionResolutionResult canProceed() {
            return new ConfusionResolutionResult(true, false, false, null, List.of());
        }
    }
}
