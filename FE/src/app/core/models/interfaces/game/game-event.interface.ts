import { GameEventType } from '../../enums/game/game-event-type.enum';

export interface GameEvent {
  eventId: string;
  gameId: string;
  eventType: GameEventType;
  stateVersion: number;
  privateEvent: boolean;
  occurredAt: string;
  payload: Record<string, unknown>;
}
