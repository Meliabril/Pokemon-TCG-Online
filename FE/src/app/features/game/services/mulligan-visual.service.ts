import { Injectable, signal } from '@angular/core';
import { MulliganRevealedCard } from '../../../core/models/interfaces/game/mulligan-event.interface';

export interface MulliganRevealedHandState {
  playerId: string;
  cards: MulliganRevealedCard[];
}

export interface MulliganBannerState {
  id: number;
  key: string;
  params?: Record<string, string | number>;
}

/** The hand shown right after a draw: real cards (own hand) or a count of face-down cards (rival). */
export interface MulliganSettledHand {
  cards?: MulliganRevealedCard[];
  faceDownCount?: number;
}

const BANNER_VISIBLE_MS = 1800;

/**
 * Holds the transient visual state of the automatic Mulligan flow so it stays in sync with the
 * board animation queue. The animation layer drives these signals as each command plays:
 *  - `revealedHand` surfaces the rival's invalid hand (the only allowed reveal) face-up in their
 *    hand zone while the reveal command runs, then clears so the gate hides it again.
 *  - `banner` shows the big floating headline (¡MULLIGAN!, shuffling, new hand, etc.).
 *  - `releasedEventIds` is the set of Mulligan event ids whose animation has already started (or that
 *    pre-date the live session); the panel renders only these so the log never spoils the visuals.
 *  - `gatedHandPlayerIds` are the players whose hand is currently hidden by the Mulligan flow. A player
 *    is gated up-front (when their Mulligan starts) and released exactly when their VALIDATED(hasBasic)
 *    animation completes — so each player's NEW hand reappears at the end of THEIR own sequence,
 *    independently of the interleaving and of the rest of the queue.
 *  - `pendingExtraCardsByPlayer` lets the board hide the trailing extra cards (which the final snapshot
 *    already contains) until the extra-cards animation actually deals them, per player.
 */
@Injectable({ providedIn: 'root' })
export class MulliganVisualService {
  private readonly revealedHandSignal = signal<MulliganRevealedHandState | null>(null);
  private readonly bannerSignal = signal<MulliganBannerState | null>(null);
  private readonly releasedEventIdsSignal = signal<ReadonlySet<string>>(new Set());
  private readonly gatedHandPlayerIdsSignal = signal<ReadonlySet<string>>(new Set());
  private readonly pendingExtraCardsByPlayerSignal = signal<ReadonlyMap<string, number>>(new Map());
  private readonly settledHandByPlayerSignal = signal<ReadonlyMap<string, MulliganSettledHand>>(new Map());
  private readonly setupCountsStagedSignal = signal(false);
  private bannerTimeoutId: number | null = null;
  private nextBannerId = 1;

  readonly revealedHand = this.revealedHandSignal.asReadonly();
  readonly banner = this.bannerSignal.asReadonly();
  readonly releasedEventIds = this.releasedEventIdsSignal.asReadonly();
  readonly gatedHandPlayerIds = this.gatedHandPlayerIdsSignal.asReadonly();
  readonly pendingExtraCardsByPlayer = this.pendingExtraCardsByPlayerSignal.asReadonly();
  readonly settledHandByPlayer = this.settledHandByPlayerSignal.asReadonly();
  /**
   * True while the Mulligan flow is being replayed on top of the (already final) snapshot. During this
   * window the board must NOT show the snapshot's final deck/prize counts: prizes aren't separated yet and
   * the deck still cycles 60<->53 with each attempt. Set on the first reveal (gateHide), cleared when the
   * MULLIGAN_FLOW_COMPLETED animation lands (resetHandStaging). Outside this window the real counts show.
   */
  readonly setupCountsStaged = this.setupCountsStagedSignal.asReadonly();

  /** Hides a player's hand for a Mulligan attempt (called when that attempt's reveal starts). Also drops
   *  any post-draw hand still shown from the previous attempt, so the zone is empty during the new attempt. */
  gateHide(playerId: string): void {
    // The first hidden hand marks the start of the Mulligan replay window: from here the deck/prize counts
    // are staged (deck cycles, prizes held at 0) until the flow-completed animation resets the staging.
    this.setupCountsStagedSignal.set(true);
    this.clearSettledHand(playerId);
    if (this.gatedHandPlayerIdsSignal().has(playerId)) {
      return;
    }
    this.gatedHandPlayerIdsSignal.update((ids) => new Set(ids).add(playerId));
  }

  /** Shows the hand a player just drew (own real cards, or rival face-down count) and releases the gate. */
  settleHand(playerId: string, hand: MulliganSettledHand): void {
    this.settledHandByPlayerSignal.update((map) => new Map(map).set(playerId, hand));
    this.gateReveal(playerId);
  }

  clearSettledHand(playerId: string): void {
    if (!this.settledHandByPlayerSignal().has(playerId)) {
      return;
    }
    this.settledHandByPlayerSignal.update((map) => {
      const next = new Map(map);
      next.delete(playerId);
      return next;
    });
  }

  /** Releases a player's hand (called when their VALIDATED-with-Basic animation completes). */
  gateReveal(playerId: string): void {
    if (!this.gatedHandPlayerIdsSignal().has(playerId)) {
      return;
    }
    this.gatedHandPlayerIdsSignal.update((ids) => {
      const next = new Set(ids);
      next.delete(playerId);
      return next;
    });
  }

  /** Number of trailing cards in the snapshot hand still to be dealt by the extra-cards animation. */
  setPendingExtra(playerId: string, count: number): void {
    if (count <= 0) {
      return;
    }
    this.pendingExtraCardsByPlayerSignal.update((map) => new Map(map).set(playerId, count));
  }

  clearPendingExtra(playerId: string): void {
    if (!this.pendingExtraCardsByPlayerSignal().has(playerId)) {
      return;
    }
    this.pendingExtraCardsByPlayerSignal.update((map) => {
      const next = new Map(map);
      next.delete(playerId);
      return next;
    });
  }

  /** Marks a Mulligan event as visible in the panel (called when its animation starts, or for history). */
  releaseEvent(eventId: string): void {
    if (this.releasedEventIdsSignal().has(eventId)) {
      return;
    }
    this.releasedEventIdsSignal.update((ids) => new Set(ids).add(eventId));
  }

  /** Bulk-releases events that pre-date the live animation baseline (e.g. on refresh/reconnect). */
  releaseEvents(eventIds: Iterable<string>): void {
    const next = new Set(this.releasedEventIdsSignal());
    let changed = false;
    for (const eventId of eventIds) {
      if (!next.has(eventId)) {
        next.add(eventId);
        changed = true;
      }
    }
    if (changed) {
      this.releasedEventIdsSignal.set(next);
    }
  }

  setRevealedHand(playerId: string, cards: MulliganRevealedCard[]): void {
    this.revealedHandSignal.set({ playerId, cards });
  }

  clearRevealedHand(): void {
    this.revealedHandSignal.set(null);
  }

  showBanner(key: string, params?: Record<string, string | number>): void {
    this.clearBannerTimeout();
    this.bannerSignal.set({ id: this.nextBannerId++, key, params });
    this.bannerTimeoutId = window.setTimeout(() => {
      this.bannerSignal.set(null);
      this.bannerTimeoutId = null;
    }, BANNER_VISIBLE_MS);
  }

  /** Clears only the per-player hand staging (gate/settled/pending) and the deck/prize staging gate, so the
   *  real snapshot counts (deck 47, prizes 6) take over once the flow finishes. The panel timeline is kept. */
  resetHandStaging(): void {
    this.gatedHandPlayerIdsSignal.set(new Set());
    this.settledHandByPlayerSignal.set(new Map());
    this.pendingExtraCardsByPlayerSignal.set(new Map());
    this.setupCountsStagedSignal.set(false);
  }

  clear(): void {
    this.clearBannerTimeout();
    this.revealedHandSignal.set(null);
    this.bannerSignal.set(null);
    this.releasedEventIdsSignal.set(new Set());
    this.gatedHandPlayerIdsSignal.set(new Set());
    this.pendingExtraCardsByPlayerSignal.set(new Map());
    this.settledHandByPlayerSignal.set(new Map());
    this.setupCountsStagedSignal.set(false);
  }

  private clearBannerTimeout(): void {
    if (this.bannerTimeoutId !== null) {
      window.clearTimeout(this.bannerTimeoutId);
      this.bannerTimeoutId = null;
    }
  }
}
