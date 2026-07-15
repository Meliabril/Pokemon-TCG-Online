export type GameVisualEventType = 'CARD_HOVER_CHANGED' | 'SETUP_SLOT_CHANGED';

export type BoardZone = 'HAND' | 'DECK' | 'PRIZES' | 'BENCH' | 'ACTIVE' | 'DISCARD';

export type BoardVisualOwner = 'SELF' | 'OPPONENT';

interface BaseGameVisualEvent {
  type: GameVisualEventType;
  gameId: string;
  playerId: string;
  zone: BoardZone;
  owner: BoardVisualOwner;
  visualIndex?: number;
  cardInstanceId?: string;
}

export interface CardHoverChangedEvent extends BaseGameVisualEvent {
  type: 'CARD_HOVER_CHANGED';
  hovered: boolean;
}

export interface SetupSlotChangedEvent extends BaseGameVisualEvent {
  type: 'SETUP_SLOT_CHANGED';
  occupied: boolean;
}

export type GameVisualEvent = CardHoverChangedEvent | SetupSlotChangedEvent;
