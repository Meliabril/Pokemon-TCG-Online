import { DOCUMENT } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  effect,
  inject,
  signal
} from '@angular/core';
import { BoardAnimation } from '../../domain/animations/board-animation.types';
import { BoardAnimationService } from '../../services/board-animation.service';
import { MulliganVisualService } from '../../services/mulligan-visual.service';
import { LanguageService } from '../../../../core/services/language.service';

interface FloatingCardViewModel {
  animation: BoardAnimation;
  width: number;
  height: number;
  transform: string;
  opacity: number;
  transition: string;
  isBack: boolean;
  phase: 'idle' | 'lift' | 'travel' | 'drop';
}

interface CoinViewModel {
  result: 'HEADS' | 'TAILS';
  transform: string;
  transition: string;
}

interface CoinFlipViewModel {
  label?: string;
  coins: CoinViewModel[];
  phase: 'idle' | 'toss' | 'fall' | 'reveal';
}

interface DeckShuffleViewModel {
  x: number;
  y: number;
  width: number;
  height: number;
  cardIndexes: number[];
}

const DEFAULT_CARD_WIDTH = 88;
const MIN_CARD_WIDTH = 58;
const MAX_CARD_WIDTH = 106;
const CARD_ASPECT_RATIO = 88 / 63;
const DEFAULT_DURATION_MS = 760;
const MOBILE_DRAW_DURATION_MS = 450;
const MOBILE_DRAW_MEDIA_QUERY = '(orientation: landscape) and (max-width: 950px) and (max-height: 430px)';
const REDUCED_MOTION_MEDIA_QUERY = '(prefers-reduced-motion: reduce)';
const REVEAL_DELAY_MS = 260;
const LIFT_DELAY_MS = 35;
const DROP_DURATION_MS = 150;
const BOARD_EASING = 'cubic-bezier(0.22, 1, 0.36, 1)';
const ANCHOR_PULSE_CLASS = 'board-anchor-pulse';
const ATTACK_LUNGE_CLASS = 'board-card-lunge';
const ATTACK_IMPACT_CLASS = 'board-card-impact';
const SELF_DAMAGE_CLASS = 'board-card-self-damage';
const SELF_HEAL_CLASS = 'board-card-self-heal';
const ATTACK_LUNGE_DURATION_MS = 640;
const ATTACK_IMPACT_DELAY_RATIO = 190 / 460;
const SELF_DAMAGE_DURATION_MS = 520;
const SELF_HEAL_DURATION_MS = 1100;
const SHUFFLE_DECK_DURATION_MS = 1280;
const SHUFFLE_DECK_CARD_COUNT = 5;
const SHUFFLE_DECK_OVERLAY_PADDING_RATIO = 0.15;
const SHUFFLE_DECK_MAX_CARD_WIDTH = 48;
const ATTACK_LUNGE_DISTANCE_RATIO = 0.6;
const ATTACK_LUNGE_ROTATION_DEG = 5;
const ANCHOR_PULSE_DURATION_MS = 460;
const COIN_FLIP_HIGHLIGHT_CLASS = 'board-coin-flip-highlight';
const COIN_FLIP_HIGHLIGHT_DURATION_MS = 1700;
const COIN_PHASE_STEP_DELAY_MS = 20;
const COIN_TOSS_DURATION_MS = 450;
const COIN_FALL_DURATION_MS = 500;
const COIN_REVEAL_HOLD_MS = 730;
const COIN_TOSS_HEIGHT_PX = 70;
const COIN_TOSS_ROTATION_DEG = 720;
const COIN_FALL_EXTRA_TURNS_DEG = 360;
const COIN_TOSS_EASING = 'cubic-bezier(0.33, 0, 0.2, 1)';
const COIN_FALL_EASING = 'cubic-bezier(0.55, 0.06, 0.68, 0.19)';
const COIN_IDLE_TRANSFORM = 'translateY(0) rotateY(0deg) scale(1)';
const MULLIGAN_REVEAL_HAND_DURATION_MS = 2000;
const HAND_REVEAL_CARD_WIDTH = 126;
const HAND_REVEAL_ENTER_MS = 360;
const HAND_REVEAL_HOLD_MS = 520;
const HAND_REVEAL_TRAVEL_MS = 460;
const HAND_REVEAL_TOTAL_MS = HAND_REVEAL_ENTER_MS + HAND_REVEAL_HOLD_MS + HAND_REVEAL_TRAVEL_MS + 120;

@Component({
  selector: 'app-board-animation-layer',
  templateUrl: './board-animation-layer.component.html',
  styleUrl: './board-animation-layer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BoardAnimationLayerComponent implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly animationService = inject(BoardAnimationService);
  private readonly mulliganVisual = inject(MulliganVisualService);
  private readonly languageService = inject(LanguageService);
  private readonly timeoutIds: number[] = [];

  readonly floatingCard = signal<FloatingCardViewModel | null>(null);
  readonly coinFlip = signal<CoinFlipViewModel | null>(null);
  readonly deckShuffle = signal<DeckShuffleViewModel | null>(null);
  readonly t = (key: string) => this.languageService.t(key);

  private previousActiveAnimation: BoardAnimation | null = null;

  constructor() {
    effect(() => {
      const animation = this.animationService.activeAnimation();
      // When the active animation changes, the previous one has just finished: apply its Mulligan gate
      // side effects (reveal that player's hand / stop hiding their extra cards) exactly on completion.
      const previous = this.previousActiveAnimation;
      if (previous && previous.id !== animation?.id) {
        this.applyMulliganGateOnComplete(previous);
      }
      this.previousActiveAnimation = animation;
      queueMicrotask(() => this.startAnimation(animation));
    });
  }

  private applyMulliganGateOnComplete(animation: BoardAnimation): void {
    if (animation.settleHand) {
      // End of a draw: show the just-drawn hand (own real cards / rival face-down count) and open the gate.
      this.mulliganVisual.settleHand(animation.settleHand.playerId, {
        cards: animation.settleHand.cards,
        faceDownCount: animation.settleHand.faceDownCount
      });
    }
    if (animation.gateRevealPlayerId) {
      this.mulliganVisual.gateReveal(animation.gateRevealPlayerId);
    }
    if (animation.clearsPendingExtraForPlayerId) {
      // The extra cards have landed: drop the post-draw hand and the pending slice so the real snapshot
      // hand (now including the extra cards) takes over for that player.
      this.mulliganVisual.clearPendingExtra(animation.clearsPendingExtraForPlayerId);
      this.mulliganVisual.clearSettledHand(animation.clearsPendingExtraForPlayerId);
    }
    if (animation.clearsMulliganStaging) {
      // End of the whole flow: hand the board back to the real snapshot (needed for initial selection).
      this.mulliganVisual.resetHandStaging();
    }
  }

  ngOnDestroy(): void {
    this.clearTimers();
    this.floatingCard.set(null);
    this.coinFlip.set(null);
    this.deckShuffle.set(null);
    this.mulliganVisual.clear();
  }

  private startAnimation(animation: BoardAnimation | null): void {
    this.clearTimers();
    this.floatingCard.set(null);
    this.coinFlip.set(null);
    this.deckShuffle.set(null);

    if (!animation) {
      return;
    }

    // Banners ride the queue: each command that carries one shows its headline when it plays.
    if (animation.banner) {
      this.mulliganVisual.showBanner(animation.banner.key, animation.banner.params);
    }

    // Release the source Mulligan event to the panel/log exactly when its animation starts, so the
    // timeline never shows revealed cards or steps before the player sees them animate.
    if (animation.sourceEventId) {
      this.mulliganVisual.releaseEvent(animation.sourceEventId);
    }

    if (animation.type === 'MULLIGAN_REVEAL_HAND') {
      this.playMulliganRevealHand(animation);
      return;
    }

    if (animation.type === 'WAIT') {
      this.schedule(() => this.animationService.complete(animation.id), animation.durationMs ?? 220);
      return;
    }

    if (animation.type === 'COIN_FLIP') {
      this.playCoinFlip(animation);
      return;
    }

    if (this.isDrawAnimation(animation) && this.matchesMedia(MOBILE_DRAW_MEDIA_QUERY)) {
      if (this.matchesMedia(REDUCED_MOTION_MEDIA_QUERY)) {
        this.animationService.complete(animation.id);
        return;
      }

      const mobileFromRect = this.anchorRect(animation.fromAnchor);
      const mobileToRect = this.anchorRect(animation.mobileToAnchor);
      if (!mobileFromRect || !mobileToRect) {
        this.animationService.complete(animation.id);
        return;
      }

      this.playMobileDraw(animation, mobileFromRect, mobileToRect);
      return;
    }

    const fromRect = this.anchorRect(animation.fromAnchor);
    // On mobile, prefer the mobile hand button target when a command provides one (Mulligan deck->hand
    // moves): the desktop hand zone anchor is collapsed/mispositioned on mobile, which sent the local
    // player's draw up-left toward the rival area. Desktop and non-tagged commands keep using toAnchor.
    const targetAnchor =
      animation.mobileToAnchor && this.matchesMedia(MOBILE_DRAW_MEDIA_QUERY)
        ? animation.mobileToAnchor
        : animation.toAnchor;
    const toRect = this.anchorRect(targetAnchor) ?? fromRect;
    if (!fromRect || !toRect) {
      this.animationService.releaseDamage(animation.targetPokemonInPlayId);
      this.animationService.complete(animation.id);
      return;
    }

    if (animation.type === 'ATTACK_LUNGE') {
      this.playAttack(animation, fromRect, toRect);
      return;
    }

    if (animation.type === 'OPPONENT_HAND_REVEAL_SHUFFLE') {
      this.playHandRevealShuffle(animation, fromRect, toRect);
      return;
    }

    if (animation.type === 'SELF_DAMAGE') {
      this.playSelfDamage(animation);
      return;
    }

    if (animation.type === 'SELF_HEAL') {
      this.playSelfHeal(animation);
      return;
    }

    if (animation.type === 'SHUFFLE_DECK') {
      this.playShuffleDeck(animation, fromRect);
      return;
    }

    this.playMove(animation, fromRect, toRect);
  }

  /**
   * Surfaces the rival's invalid hand face-up in their hand zone (via MulliganVisualService) for
   * the command's duration, then clears it so the staging gate hides the hand again and the
   * following return-to-deck animation can play. No floating card here — the hand zone renders it.
   */
  private playMulliganRevealHand(animation: BoardAnimation): void {
    const playerId = animation.affectedPlayerId;
    const cards = animation.revealedHandCards ?? [];
    if (!playerId || cards.length === 0) {
      this.animationService.complete(animation.id);
      return;
    }

    // Re-hide this player's hand for THIS attempt (drops any post-draw hand shown from the previous
    // attempt) and surface the invalid hand face-up on top of the gate while the reveal plays.
    this.mulliganVisual.gateHide(playerId);
    this.mulliganVisual.setRevealedHand(playerId, cards);

    this.schedule(() => {
      this.mulliganVisual.clearRevealedHand();
      this.animationService.complete(animation.id);
    }, animation.durationMs ?? MULLIGAN_REVEAL_HAND_DURATION_MS);
  }

  private playMobileDraw(animation: BoardAnimation, fromRect: DOMRect, toRect: DOMRect): void {
    const width = Math.min(64, this.cardWidth(fromRect, toRect));
    const height = this.cardHeight(width);
    const startTransform = this.transformFor(fromRect, width, height, 0.92);
    const targetTransform = this.transformFor(toRect, width, height, 0.56);

    this.floatingCard.set({
      animation,
      width,
      height,
      transform: startTransform,
      opacity: 1,
      transition: 'none',
      isBack: true,
      phase: 'idle'
    });

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: targetTransform,
              opacity: 0.88,
              transition: `transform ${MOBILE_DRAW_DURATION_MS}ms ${BOARD_EASING}, opacity ${MOBILE_DRAW_DURATION_MS}ms ease-out`,
              phase: 'travel'
            }
          : card
      );
    }, 16);

    this.schedule(() => {
      this.floatingCard.set(null);
      this.animationService.complete(animation.id);
    }, MOBILE_DRAW_DURATION_MS + 32);
  }

  private isDrawAnimation(animation: BoardAnimation): boolean {
    return animation.type === 'PLAYER_DRAW_CARD' || animation.type === 'OPPONENT_DRAW_CARD';
  }

  /** The opening shuffle + deal of the first hand (game start). Used to suppress the gold target-pulse. */
  private isOpeningDealAnimation(animation: BoardAnimation): boolean {
    return animation.type === 'OPENING_DEAL_CARD' || animation.type === 'OPENING_DECK_SHUFFLE';
  }

  private matchesMedia(query: string): boolean {
    return this.document.defaultView?.matchMedia(query).matches ?? false;
  }

  private playMove(animation: BoardAnimation, fromRect: DOMRect, toRect: DOMRect): void {
    const durationMs = animation.durationMs ?? DEFAULT_DURATION_MS;
    const previewDelayMs = animation.revealBeforeMove ? REVEAL_DELAY_MS : 90;
    const travelDurationMs = Math.max(360, durationMs - previewDelayMs - DROP_DURATION_MS);
    const baseWidth = this.cardWidth(fromRect, toRect);
    // Card-back moves (Mulligan draw/return/extra, opening deal) skip the mobile draw path, so on mobile
    // cap their floating width like playMobileDraw does — otherwise they render oversized vs the board.
    const width =
      animation.useCardBack && this.matchesMedia(MOBILE_DRAW_MEDIA_QUERY)
        ? Math.min(64, baseWidth)
        : baseWidth;
    const height = this.cardHeight(width);
    const rotationDeg = animation.travelRotationDeg ?? 4;
    const startTransform = this.transformFor(fromRect, width, height, 0.92, 0);
    const liftTransform = this.transformFor(fromRect, width, height, 1.08, rotationDeg / 2, -14);
    const travelTransform = this.transformFor(toRect, width, height, 1.08, rotationDeg, -12);
    const dropTransform = this.transformFor(toRect, width, height, 1, 0, 0);

    this.floatingCard.set({
      animation,
      width,
      height,
      transform: startTransform,
      opacity: 1,
      transition: 'none',
      isBack: Boolean(animation.useCardBack || animation.type === 'OPPONENT_DRAW_CARD' || !animation.cardImageUrl),
      phase: 'idle'
    });

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: liftTransform,
              transition: `transform 170ms ${BOARD_EASING}, filter 170ms ${BOARD_EASING}`,
              phase: 'lift'
            }
          : card
      );
    }, LIFT_DELAY_MS);

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: travelTransform,
              opacity: 0.98,
              transition: `transform ${travelDurationMs}ms ${BOARD_EASING}, opacity ${travelDurationMs}ms ease-out`,
              phase: 'travel'
            }
          : card
      );
    }, previewDelayMs);

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: dropTransform,
              opacity: 1,
              transition: `transform ${DROP_DURATION_MS}ms cubic-bezier(0.2, 0.9, 0.28, 1.35), opacity ${DROP_DURATION_MS}ms ease-out`,
              phase: 'drop'
            }
          : card
      );
      // Skip the gold target-pulse for the setup deal: Mulligan-flow moves (return-to-deck / new-hand
      // draw, tagged with `sourceEventId`) AND the opening shuffle/deal of the first hand. Both should
      // read clean. Normal gameplay draws/plays keep their landing pulse.
      if (!animation.sourceEventId && !this.isOpeningDealAnimation(animation)) {
        this.pulseAnchor(animation.targetPulseAnchor);
      }
    }, previewDelayMs + travelDurationMs);

    this.schedule(() => {
      this.floatingCard.set(null);
      this.animationService.complete(animation.id);
    }, previewDelayMs + travelDurationMs + DROP_DURATION_MS + 90);
  }

  private playAttack(animation: BoardAnimation, fromRect: DOMRect, toRect: DOMRect): void {
    const attackerEl = this.anchorElement(animation.fromAnchor);
    if (!attackerEl) {
      this.animationService.releaseDamage(animation.targetPokemonInPlayId);
      this.animationService.complete(animation.id);
      return;
    }

    const defenderEl = this.anchorElement(animation.toAnchor);
    const fromCenterX = fromRect.left + fromRect.width / 2;
    const fromCenterY = fromRect.top + fromRect.height / 2;
    const toCenterX = toRect.left + toRect.width / 2;
    const toCenterY = toRect.top + toRect.height / 2;
    const dx = toCenterX - fromCenterX;
    const dy = toCenterY - fromCenterY;
    const distance = Math.hypot(dx, dy) || 1;
    const lungeDistance = fromRect.height * ATTACK_LUNGE_DISTANCE_RATIO;
    const lungeX = (dx / distance) * lungeDistance;
    const lungeY = (dy / distance) * lungeDistance;
    const lungeRotate = dx >= 0 ? ATTACK_LUNGE_ROTATION_DEG : -ATTACK_LUNGE_ROTATION_DEG;

    const duration = animation.durationMs ?? ATTACK_LUNGE_DURATION_MS;
    const impactDelay = Math.round(duration * ATTACK_IMPACT_DELAY_RATIO);

    attackerEl.style.setProperty('--lunge-x', `${lungeX}px`);
    attackerEl.style.setProperty('--lunge-y', `${lungeY}px`);
    attackerEl.style.setProperty('--lunge-rotate', `${lungeRotate}deg`);
    attackerEl.style.setProperty('--lunge-duration', `${duration}ms`);
    attackerEl.classList.remove(ATTACK_LUNGE_CLASS);
    window.requestAnimationFrame(() => attackerEl.classList.add(ATTACK_LUNGE_CLASS));

    this.schedule(() => {
      this.animationService.releaseDamage(animation.targetPokemonInPlayId);

      if (!defenderEl) {
        return;
      }

      defenderEl.style.setProperty('--impact-duration', `${duration}ms`);
      defenderEl.classList.remove(ATTACK_IMPACT_CLASS);
      window.requestAnimationFrame(() => defenderEl.classList.add(ATTACK_IMPACT_CLASS));
    }, impactDelay);

    this.schedule(() => {
      attackerEl.classList.remove(ATTACK_LUNGE_CLASS);
      attackerEl.style.removeProperty('--lunge-x');
      attackerEl.style.removeProperty('--lunge-y');
      attackerEl.style.removeProperty('--lunge-rotate');
      attackerEl.style.removeProperty('--lunge-duration');
      defenderEl?.classList.remove(ATTACK_IMPACT_CLASS);
      defenderEl?.style.removeProperty('--impact-duration');
      this.animationService.complete(animation.id);
    }, duration);
  }

  private playHandRevealShuffle(animation: BoardAnimation, fromRect: DOMRect, toRect: DOMRect): void {
    const width = Math.min(HAND_REVEAL_CARD_WIDTH, Math.max(MAX_CARD_WIDTH, fromRect.width * 1.25));
    const height = this.cardHeight(width);
    const rotationDeg = animation.travelRotationDeg ?? 6;
    const startTransform = this.transformFor(fromRect, width, height, 0.78, -rotationDeg, 0);
    const revealTransform = this.centerTransform(width, height, 1.18, 0, -18);
    const turnBackTransform = this.centerTransform(width, height, 1.08, rotationDeg / 2, -18);
    const deckTransform = this.transformFor(toRect, width, height, 0.42, rotationDeg, -4);

    this.floatingCard.set({
      animation,
      width,
      height,
      transform: startTransform,
      opacity: 0,
      transition: 'none',
      isBack: true,
      phase: 'idle'
    });

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: revealTransform,
              opacity: 1,
              transition: `transform ${HAND_REVEAL_ENTER_MS}ms ${BOARD_EASING}, opacity 180ms ease-out`,
              isBack: false,
              phase: 'lift'
            }
          : card
      );
    }, 20);

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: turnBackTransform,
              transition: 'transform 180ms cubic-bezier(0.2, 0.9, 0.28, 1.35)',
              isBack: true,
              phase: 'travel'
            }
          : card
      );
    }, HAND_REVEAL_ENTER_MS + HAND_REVEAL_HOLD_MS);

    this.schedule(() => {
      this.floatingCard.update((card) =>
        card
          ? {
              ...card,
              transform: deckTransform,
              opacity: 0.9,
              transition: `transform ${HAND_REVEAL_TRAVEL_MS}ms ${BOARD_EASING}, opacity ${HAND_REVEAL_TRAVEL_MS}ms ease-out`,
              phase: 'drop'
            }
          : card
      );
      this.pulseAnchor(animation.targetPulseAnchor);
    }, HAND_REVEAL_ENTER_MS + HAND_REVEAL_HOLD_MS + 120);

    this.schedule(() => {
      this.floatingCard.set(null);
      this.animationService.complete(animation.id);
    }, animation.durationMs ?? HAND_REVEAL_TOTAL_MS);
  }

  private playSelfDamage(animation: BoardAnimation): void {
    const targetEl = this.anchorElement(animation.fromAnchor);
    if (!targetEl) {
      this.animationService.releaseDamage(animation.targetPokemonInPlayId);
      this.animationService.complete(animation.id);
      return;
    }

    const targetRect = targetEl.getBoundingClientRect();
    const viewportMidY = window.innerHeight / 2;
    const viewportMidX = window.innerWidth / 2;
    const recoilY = targetRect.top < viewportMidY ? targetRect.height * 0.18 : targetRect.height * -0.18;
    const recoilX = targetRect.left < viewportMidX ? targetRect.width * 0.08 : targetRect.width * -0.08;
    const recoilRotate = recoilX >= 0 ? 7 : -7;

    const duration = animation.durationMs ?? SELF_DAMAGE_DURATION_MS;
    targetEl.style.setProperty('--self-damage-x', `${recoilX}px`);
    targetEl.style.setProperty('--self-damage-y', `${recoilY}px`);
    targetEl.style.setProperty('--self-damage-rotate', `${recoilRotate}deg`);
    targetEl.style.setProperty('--self-damage-duration', `${duration}ms`);
    targetEl.classList.remove(SELF_DAMAGE_CLASS);
    window.requestAnimationFrame(() => targetEl.classList.add(SELF_DAMAGE_CLASS));

    this.schedule(() => {
      this.animationService.releaseDamage(animation.targetPokemonInPlayId);
    }, Math.floor(duration * 0.35));

    this.schedule(() => {
      targetEl.classList.remove(SELF_DAMAGE_CLASS);
      targetEl.style.removeProperty('--self-damage-x');
      targetEl.style.removeProperty('--self-damage-y');
      targetEl.style.removeProperty('--self-damage-rotate');
      targetEl.style.removeProperty('--self-damage-duration');
      this.animationService.complete(animation.id);
    }, duration);
  }

  private playSelfHeal(animation: BoardAnimation): void {
    const targetEl = this.anchorElement(animation.fromAnchor);
    if (!targetEl) {
      this.animationService.complete(animation.id);
      return;
    }

    targetEl.classList.remove(SELF_HEAL_CLASS);
    window.requestAnimationFrame(() => targetEl.classList.add(SELF_HEAL_CLASS));

    this.schedule(() => {
      targetEl.classList.remove(SELF_HEAL_CLASS);
      this.animationService.complete(animation.id);
    }, animation.durationMs ?? SELF_HEAL_DURATION_MS);
  }

  private playShuffleDeck(animation: BoardAnimation, anchorRect: DOMRect): void {
    const cardWidth = Math.min(SHUFFLE_DECK_MAX_CARD_WIDTH, anchorRect.width, anchorRect.height / CARD_ASPECT_RATIO);
    const cardHeight = this.cardHeight(cardWidth);
    const width = cardWidth * (1 + SHUFFLE_DECK_OVERLAY_PADDING_RATIO);
    const height = cardHeight * (1 + SHUFFLE_DECK_OVERLAY_PADDING_RATIO);
    const centerX = anchorRect.left + anchorRect.width / 2;
    const centerY = anchorRect.top + anchorRect.height / 2;

    this.deckShuffle.set({
      x: centerX - width / 2,
      y: centerY - height / 2,
      width,
      height,
      cardIndexes: Array.from({ length: SHUFFLE_DECK_CARD_COUNT }, (_, index) => index)
    });

    this.schedule(() => {
      this.deckShuffle.set(null);
      this.animationService.complete(animation.id);
    }, animation.durationMs ?? SHUFFLE_DECK_DURATION_MS);
  }

  private playCoinFlip(animation: BoardAnimation): void {
    const results = animation.coinResults ?? [];
    if (results.length === 0) {
      this.animationService.complete(animation.id);
      return;
    }

    this.pulseAnchor(animation.highlightAnchor, COIN_FLIP_HIGHLIGHT_CLASS, COIN_FLIP_HIGHLIGHT_DURATION_MS);
    this.coinFlip.set({
      label: animation.coinFlipLabel,
      phase: 'idle',
      coins: results.map((result) => ({ result, transform: COIN_IDLE_TRANSFORM, transition: 'none' }))
    });

    this.schedule(() => {
      this.coinFlip.update((value) =>
        value
          ? {
              ...value,
              phase: 'toss',
              coins: value.coins.map((coin) => ({
                ...coin,
                transform: `translateY(-${COIN_TOSS_HEIGHT_PX}px) rotateY(${COIN_TOSS_ROTATION_DEG}deg) scale(1.15)`,
                transition: `transform ${COIN_TOSS_DURATION_MS}ms ${COIN_TOSS_EASING}`
              }))
            }
          : value
      );
    }, COIN_PHASE_STEP_DELAY_MS);

    this.schedule(() => {
      this.coinFlip.update((value) =>
        value
          ? {
              ...value,
              phase: 'fall',
              coins: value.coins.map((coin) => {
                const landingDeg =
                  COIN_TOSS_ROTATION_DEG + COIN_FALL_EXTRA_TURNS_DEG + (coin.result === 'HEADS' ? 0 : 180);
                return {
                  ...coin,
                  transform: `translateY(0) rotateY(${landingDeg}deg) scale(1)`,
                  transition: `transform ${COIN_FALL_DURATION_MS}ms ${COIN_FALL_EASING}`
                };
              })
            }
          : value
      );
    }, COIN_PHASE_STEP_DELAY_MS + COIN_TOSS_DURATION_MS);

    this.schedule(() => {
      this.coinFlip.update((value) => (value ? { ...value, phase: 'reveal' } : value));
    }, COIN_PHASE_STEP_DELAY_MS + COIN_TOSS_DURATION_MS + COIN_FALL_DURATION_MS);

    this.schedule(() => {
      this.coinFlip.set(null);
      this.animationService.complete(animation.id);
    }, COIN_PHASE_STEP_DELAY_MS + COIN_TOSS_DURATION_MS + COIN_FALL_DURATION_MS + COIN_REVEAL_HOLD_MS);
  }

  coinFlipHeadsCount(coins: CoinViewModel[]): number {
    return coins.filter((coin) => coin.result === 'HEADS').length;
  }

  private anchorElement(anchorId: string | undefined): HTMLElement | null {
    if (!anchorId) {
      return null;
    }

    return this.document.querySelector<HTMLElement>(`[data-board-anchor="${anchorId}"]`) ?? null;
  }

  private anchorRect(anchorId: string | undefined): DOMRect | null {
    if (!anchorId) {
      return null;
    }

    return this.document.querySelector(`[data-board-anchor="${anchorId}"]`)?.getBoundingClientRect() ?? null;
  }

  private transformFor(
    rect: DOMRect,
    width: number,
    height: number,
    scale: number,
    rotationDeg = 0,
    yOffset = 0
  ): string {
    const x = rect.left + rect.width / 2 - width / 2;
    const y = rect.top + rect.height / 2 - height / 2 + yOffset;
    return `translate3d(${x}px, ${y}px, 0) rotate(${rotationDeg}deg) scale(${scale})`;
  }

  private centerTransform(width: number, height: number, scale: number, rotationDeg = 0, yOffset = 0): string {
    const viewport = this.document.defaultView;
    const x = ((viewport?.innerWidth ?? window.innerWidth) - width) / 2;
    const y = ((viewport?.innerHeight ?? window.innerHeight) - height) / 2 + yOffset;
    return `translate3d(${x}px, ${y}px, 0) rotate(${rotationDeg}deg) scale(${scale})`;
  }

  private cardWidth(fromRect: DOMRect, toRect: DOMRect): number {
    const anchorWidth = Math.min(nonZero(fromRect.width), nonZero(toRect.width));
    const anchorHeightWidth = Math.min(nonZero(fromRect.height), nonZero(toRect.height)) / CARD_ASPECT_RATIO;
    const candidate = Math.max(anchorWidth, anchorHeightWidth, DEFAULT_CARD_WIDTH);
    return Math.max(MIN_CARD_WIDTH, Math.min(MAX_CARD_WIDTH, candidate));
  }

  private cardHeight(width: number): number {
    return width * CARD_ASPECT_RATIO;
  }

  private pulseAnchor(
    anchorId: string | undefined,
    className: string = ANCHOR_PULSE_CLASS,
    durationMs: number = ANCHOR_PULSE_DURATION_MS
  ): void {
    if (!anchorId) {
      return;
    }

    const anchor = this.document.querySelector(`[data-board-anchor="${anchorId}"]`);
    if (!anchor) {
      return;
    }

    anchor.classList.remove(className);
    window.requestAnimationFrame(() => {
      anchor.classList.add(className);
      this.schedule(() => anchor.classList.remove(className), durationMs);
    });
  }

  private schedule(callback: () => void, delayMs: number): void {
    this.timeoutIds.push(window.setTimeout(callback, delayMs));
  }

  private clearTimers(): void {
    for (const timeoutId of this.timeoutIds) {
      window.clearTimeout(timeoutId);
    }
    this.timeoutIds.length = 0;
  }
}

function nonZero(value: number): number {
  return value > 0 ? value : DEFAULT_CARD_WIDTH;
}
