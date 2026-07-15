import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { MulliganRevealedCard } from '../../../../core/models/interfaces/game/mulligan-event.interface';

/** A Mulligan hand always has exactly 7 cards. */
const MULLIGAN_HAND_SIZE = 7;

/**
 * Mobile-only Mulligan modal. It is an AUTOMATIC, per-attempt presentation driven by the reveal animation
 * currently playing (no buttons, no parallel flow): the side that is mulliganing this attempt shows its
 * invalid hand face-up, the other side shows 7 face-down backs (privacy preserved — a hand is only shown
 * face-up when it was publicly revealed by the Mulligan rule). It opens/auto-closes with the reveal command
 * and never exposes the new valid hand, deck, prizes or extra cards.
 */
@Component({
  selector: 'app-mulligan-modal',
  templateUrl: './mulligan-modal.component.html',
  styleUrl: './mulligan-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MulliganModalComponent {
  private readonly languageService = inject(LanguageService);

  readonly open = input(false);
  /** The local player's invalid hand, shown face-up only on the attempt the local player is revealing. */
  readonly ownRevealedCards = input<MulliganRevealedCard[]>([]);
  /** The rival's invalid hand, shown face-up only on the attempt the rival is revealing (public reveal). */
  readonly rivalRevealedCards = input<MulliganRevealedCard[]>([]);
  readonly ownMulliganCount = input(0);
  readonly opponentMulliganCount = input(0);

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly hasRivalReveal = computed(() => this.rivalRevealedCards().length > 0);
  readonly hasOwnReveal = computed(() => this.ownRevealedCards().length > 0);
  readonly rivalCards = computed(() => this.rivalRevealedCards().slice(0, MULLIGAN_HAND_SIZE));
  readonly ownCards = computed(() => this.ownRevealedCards().slice(0, MULLIGAN_HAND_SIZE));
}
