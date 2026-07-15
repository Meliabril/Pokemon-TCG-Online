import { DeckSummary, DeckValidationResponse } from '../deck/deck-summary.interface';
import { MatchmakingQueueStatus } from '../matchmaking/matchmaking-queue-status.interface';

export type PrematchVisualState =
  | 'idle'
  | 'checking'
  | 'ready'
  | 'blocked'
  | 'joining'
  | 'queued'
  | 'leaving'
  | 'error';

export interface PreMatchChecklistState {
  status: PrematchVisualState;
  availableDecks: DeckSummary[];
  activeDeck: DeckSummary | null;
  validation: DeckValidationResponse | null;
  queueStatus: MatchmakingQueueStatus | null;
  errorMessage: string;
  feedbackMessage: string;
  checks: {
    sessionReady: boolean;
    activeDeckFound: boolean;
    deckValid: boolean;
  };
}

export interface QueueRoomState {
  status: PrematchVisualState;
  activeDeck: DeckSummary | null;
  queueStatus: MatchmakingQueueStatus | null;
  errorMessage: string;
  infoMessage: string;
  matchFound: boolean;
  gameId: string | null;
  opponentUserId: string | null;
  matchedAt: string | null;
}
