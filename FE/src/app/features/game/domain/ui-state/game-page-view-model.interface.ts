import { GameStatus } from '../../../../core/models/enums/game/game-status.enum';

export interface GamePageParticipantViewModel {
  id: string;
  label: string;
  connected: boolean;
}

export interface GamePageHistoryItemViewModel {
  id: string;
  actorLabel: string;
  actionLabel: string;
  createdAt: string | null;
  version: number;
}

export interface GamePageSnapshotFactViewModel {
  label: string;
  value: string;
}

export interface GamePageViewModel {
  gameId: string;
  status: GameStatus;
  statusLabel: string;
  statusSummary: string;
  turnNumber: number;
  stateVersion: number;
  phaseLabel: string;
  activePlayerLabel: string | null;
  winnerLabel: string | null;
  pauseReason: string | null;
  currentParticipantId: string | null;
  participants: GamePageParticipantViewModel[];
  benchCountByParticipantId: Record<string, number>;
  availableActionLabels: string[];
  snapshotFacts: GamePageSnapshotFactViewModel[];
  historyCount: number;
  historySummary: string;
  latestHistoryEntry: GamePageHistoryItemViewModel | null;
  history: GamePageHistoryItemViewModel[];
}
