import { ChangeDetectionStrategy, Component, OnDestroy, computed, inject, input, output, signal } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import {
  BoardEventFeedItemViewModel,
  BoardGameViewModel
} from '../../domain/board/board-game-view-model.interface';
import { GameLogFeedComponent } from '../game-log-feed/game-log-feed.component';

const TURN_TIMEOUT_SECONDS = 120;
const URGENT_THRESHOLD_SECONDS = 20;

@Component({
  selector: 'app-turn-hud',
  templateUrl: './turn-hud.component.html',
  styleUrl: './turn-hud.component.css',
  imports: [GameLogFeedComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TurnHudComponent implements OnDestroy {
  private readonly languageService = inject(LanguageService);
  private readonly nowMillis = signal(Date.now());
  private readonly intervalId = window.setInterval(() => this.nowMillis.set(Date.now()), 1000);

  readonly statusLabel = input.required<string>();
  readonly statusSummary = input.required<string>();
  readonly turnNumber = input.required<number>();
  readonly phaseLabel = input.required<string>();
  readonly activePlayerLabel = input<string | null>(null);
  readonly stadium = input<BoardGameViewModel['stadium']>(null);
  readonly turnStartedAt = input<string | null>(null);
  readonly stateVersion = input.required<number>();
  readonly eventFeed = input.required<BoardEventFeedItemViewModel[]>();
  readonly historySummary = input.required<string>();
  readonly latestActionLabel = input.required<string>();
  readonly endTurnEnabled = input(false);
  readonly endTurnDisabledReason = input<string | null>(null);
  readonly setupMode = input(false);
  readonly setupAlreadySubmitted = input(false);
  readonly setupSubmitEnabled = input(false);
  readonly setupBusy = input(false);
  readonly leaveTable = output<void>();
  readonly endTurn = output<void>();
  readonly setupSubmitted = output<void>();
  readonly setupCancelled = output<void>();
  readonly historyOpen = signal(false);
  readonly setupImageUrl = computed(() =>
    this.languageService.language() === 'es'
      ? '/assets/images/setup-guide-es.png'
      : '/assets/images/setup-guide-en.png'
  );
  readonly isSpanishSetupImage = this.languageService.isSpanish;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly currentTurnLabel = computed(() => {
    if (this.endTurnEnabled()) {
      return this.t('GAME.YOUR_TURN');
    }

    const activePlayer = this.activePlayerLabel();
    return activePlayer
      ? this.t('GAME.PLAYER_TURN', { player: activePlayer })
      : this.t('GAME.WAITING_STATE');
  });

  readonly stadiumStatusLabel = computed(() => {
    const stadium = this.stadium();
    if (!stadium?.card) {
      return this.t('GAME.STADIUM_NONE');
    }

    return stadium.card.label
      ? this.t('GAME.STADIUM_IN_PLAY_WITH_CARD', { card: stadium.card.label })
      : this.t('GAME.STADIUM_IN_PLAY');
  });

  readonly remainingSeconds = computed(() => {
    const startedAt = this.turnStartedAt();
    if (!startedAt) {
      return null;
    }

    const elapsedSeconds = Math.floor((this.nowMillis() - new Date(startedAt).getTime()) / 1000);
    return Math.max(0, TURN_TIMEOUT_SECONDS - elapsedSeconds);
  });

  readonly remainingLabel = computed(() => {
    const remaining = this.remainingSeconds();
    if (remaining === null) {
      return null;
    }

    const minutes = Math.floor(remaining / 60);
    const seconds = remaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  });

  readonly isUrgent = computed(() => {
    const remaining = this.remainingSeconds();
    return remaining !== null && remaining <= URGENT_THRESHOLD_SECONDS;
  });

  toggleHistory(): void {
    this.historyOpen.update((isOpen) => !isOpen);
  }

  ngOnDestroy(): void {
    window.clearInterval(this.intervalId);
  }
}
