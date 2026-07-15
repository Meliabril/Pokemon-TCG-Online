export type MatchResult = 'VICTORIA' | 'DERROTA';

export type MatchHistoryFilter = 'ALL' | 'WINS' | 'LOSSES' | 'LAST_7_DAYS' | 'LAST_30_DAYS';

export interface PlayerStats {
  totalMatches: number;
  wins: number;
  losses: number;
  winRate: number;
  currentStreak: number;
}

export interface MatchHistory {
  matchId: string;
  result: MatchResult;
  opponentName: string;
  date: string;
  turnsPlayed: number;
}
