export interface MatchmakingQueueStatus {
  queued: boolean;
  queuedAt: string | null;
  queueSize: number | null;
  matchedUserId: string | null;
  status?: string | null;
  gameId?: string | null;
  participantId?: string | null;
}
