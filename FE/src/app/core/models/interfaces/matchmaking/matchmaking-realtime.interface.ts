import { GameEvent } from '../game/game-event.interface';
import { GameStateSync } from '../game/game-state.interface';

export interface MatchFoundMessage {
  opponentUserId: string;
  gameId: string;
  matchedAt: string;
}

export type GameRealtimeEnvelope = GameEvent | GameStateSync;
