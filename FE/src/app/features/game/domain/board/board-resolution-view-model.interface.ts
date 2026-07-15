import {
  GameResolutionType,
  PendingAttackChoicePayload
} from '../../../../core/models/interfaces/game/game-resolution.interface';

export interface BoardResolutionViewModel {
  resolutionType: GameResolutionType;
  label: string;
  playerToPromoteId: string | null;
  nextActivePlayerId: string | null;
  nextTurnNumber: number | null;
  pendingChoicePlayerId: string | null;
  pendingChoiceType: string | null;
  pendingChoicePayload: PendingAttackChoicePayload | null;
  revealedCardId: string | null;
}
