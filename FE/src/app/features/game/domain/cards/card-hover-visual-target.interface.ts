import {
  BoardVisualOwner,
  BoardZone
} from '../../../../core/models/interfaces/game/game-visual-event.interface';

export interface CardHoverVisualTarget {
  zone: BoardZone;
  owner: BoardVisualOwner;
  visualIndex?: number;
  cardInstanceId?: string | null;
  hovered: boolean;
}

export function cardHoverKey(target: {
  owner: BoardVisualOwner;
  zone: BoardZone;
  visualIndex?: number;
  cardInstanceId?: string | null;
}): string | null {
  const cardInstanceId = target.cardInstanceId?.trim();
  if (cardInstanceId) {
    return `${target.owner}:${target.zone}:${cardInstanceId}`;
  }

  if (typeof target.visualIndex === 'number') {
    return `${target.owner}:${target.zone}:${target.visualIndex}`;
  }

  return null;
}
