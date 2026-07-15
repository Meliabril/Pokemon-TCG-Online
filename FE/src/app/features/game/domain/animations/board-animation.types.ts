import { MulliganRevealedCard } from '../../../../core/models/interfaces/game/mulligan-event.interface';

export type BoardAnimationType =
  | 'OPPONENT_DRAW_CARD'
  | 'PLAYER_DRAW_CARD'
  | 'OPPONENT_REVEAL_CARD'
  | 'OPPONENT_MOVE_CARD'
  | 'PLAYER_MOVE_CARD'
  | 'OPPONENT_ATTACH_ENERGY'
  | 'OPPONENT_PLAY_TRAINER'
  | 'OPPONENT_HAND_REVEAL_SHUFFLE'
  | 'ATTACK_LUNGE'
  | 'SELF_DAMAGE'
  | 'SELF_HEAL'
  | 'DISCARD_CARD'
  | 'MULLIGAN_REVEAL_CARD'
  | 'MULLIGAN_REVEAL_HAND'
  | 'MULLIGAN_RETURN_CARD'
  | 'MULLIGAN_SHUFFLE'
  | 'MULLIGAN_DRAW_CARD'
  | 'MULLIGAN_EXTRA_CARD'
  | 'OPENING_DECK_SHUFFLE'
  | 'OPENING_DEAL_CARD'
  | 'COIN_FLIP'
  | 'SHUFFLE_DECK'
  | 'WAIT';

export interface BoardAnimationCommand {
  type: BoardAnimationType;
  fromAnchor?: string;
  toAnchor?: string;
  mobileToAnchor?: string;
  cardImageUrl?: string;
  cardLabel?: string;
  durationMs?: number;
  revealBeforeMove?: boolean;
  targetPulseAnchor?: string;
  hideLabel?: boolean;
  useCardBack?: boolean;
  travelRotationDeg?: number;
  coinResults?: ('HEADS' | 'TAILS')[];
  coinFlipLabel?: string;
  highlightAnchor?: string;
  targetPokemonInPlayId?: string;
  /** Player whose hand this animation belongs to; used to gate (hide) that hand while it plays. */
  affectedPlayerId?: string;
  /** Cards to surface face-up in the rival's hand zone during a MULLIGAN_REVEAL_HAND command. */
  revealedHandCards?: MulliganRevealedCard[];
  /** Big floating headline to show while this command plays (translated by the banner overlay). */
  banner?: { key: string; params?: Record<string, string | number> };
  /**
   * Source game event id. When the command starts playing, its source event is "released" to the
   * Mulligan panel so the log timeline stays in sync with the animations (no early spoilers).
   */
  sourceEventId?: string;
  /** When this command completes, release this player's Mulligan hand gate (their new hand reappears). */
  gateRevealPlayerId?: string;
  /** When this command completes, stop hiding this player's pending extra cards (the +N appear). */
  clearsPendingExtraForPlayerId?: string;
  /**
   * When this command (the end of a draw) completes, show the hand the player just drew and release their
   * gate: own real cards (`cards`) for the local owner, or a count of face-down cards for the rival.
   */
  settleHand?: { playerId: string; cards?: MulliganRevealedCard[]; faceDownCount?: number };
  /** When this command completes, reset the Mulligan hand staging so the real snapshot hand takes over. */
  clearsMulliganStaging?: boolean;
}

export interface BoardAnimation extends BoardAnimationCommand {
  id: number;
}
