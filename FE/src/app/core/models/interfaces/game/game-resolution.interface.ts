export enum GameResolutionType {
  PromotionRequired = 'PROMOTION_REQUIRED',
  SuddenDeathRequired = 'SUDDEN_DEATH_REQUIRED',
  AttackChoiceRequired = 'ATTACK_CHOICE_REQUIRED'
}

export interface GameResolution {
  resolutionType: GameResolutionType | null;
  playerToPromoteId: string | null;
  nextActivePlayerId: string | null;
  nextTurnNumber: number;
  pendingChoicePlayerId: string | null;
  pendingChoiceType: string | null;
  pendingChoicePayload?: PendingAttackChoicePayload | null;
}

export type PendingAttackChoicePayload = Record<string, unknown>;
