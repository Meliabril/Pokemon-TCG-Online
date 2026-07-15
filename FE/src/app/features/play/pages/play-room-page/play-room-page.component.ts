import { ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { MatchFoundMessage } from '../../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { LanguageService } from '../../../../core/services/language.service';
import { PrematchFacadeService } from '../../services/prematch-facade.service';
import {
  MatchmakingRealtimeMessage,
  MatchmakingRealtimeService
} from '../../services/matchmaking-realtime.service';
import { DeckShuffleAnimationComponent } from '../../../../shared/ui/feedback/deck-shuffle-animation/deck-shuffle-animation.component';

const ROOM_POLL_INTERVAL_MS = 5000;
const SEARCH_TIMER_INTERVAL_MS = 1000;
const MATCH_FOUND_TRANSITION_MS = 4000;

@Component({
  selector: 'app-play-room-page',
  templateUrl: './play-room-page.component.html',
  styleUrl: './play-room-page.component.css',
  imports: [DeckShuffleAnimationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PlayRoomPageComponent implements OnDestroy {
  private readonly prematchFacade = inject(PrematchFacadeService);
  private readonly matchmakingRealtime = inject(MatchmakingRealtimeService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private isPolling = false;
  private pollTimerId: number | null = null;
  private elapsedTimerId: number | null = null;
  private transitionTimerId: number | null = null;
  private readonly searchStartedAt = Date.now();

  readonly routes = APP_ROUTES;
  readonly homeUrl = `/${APP_ROUTES.home}`;
  readonly state = this.prematchFacade.roomState;
  readonly isLightTheme = computed(() => this.appTheme.theme() === 'LIGHT');
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly elapsedSeconds = signal(0);
  readonly isMatchTransitioning = signal(false);
  readonly elapsedLabel = computed(() => {
    const minutes = Math.floor(this.elapsedSeconds() / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (this.elapsedSeconds() % 60).toString().padStart(2, '0');
    return `${minutes}:${seconds}`;
  });
  readonly activeDeckName = computed(() => {
    this.languageService.translations();
    return this.state().activeDeck?.name ?? this.t('PLAY.NO_ACTIVE_DECK');
  });
  readonly queueSizeLabel = computed(() => this.state().queueStatus?.queueSize?.toString() ?? '--');
  readonly waitingSinceLabel = computed(() => {
    this.languageService.translations();
    return this.state().queueStatus?.queuedAt ?? this.t('PLAY.NO_TIMESTAMP');
  });

  constructor() {
    this.startElapsedTimer();

    this.matchmakingRealtime
      .watchMatchmaking()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message) => {
        void this.handleRealtimeMessage(message);
      });

    const navigation = this.router.getCurrentNavigation();
    const matchedUserId = navigation?.extras.state?.['matchedUserId'];
    const gameId = navigation?.extras.state?.['gameId'];

    if (typeof gameId === 'string') {
      setTimeout(() => {
        void this.beginMatchFoundTransition(gameId);
      }, 50);
      return;
    }

    if (typeof matchedUserId === 'string') {
      void this.loadMatchedRoomFromNavigation(matchedUserId);
      return;
    }

    void this.loadRoomState();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.stopElapsedTimer();
    this.clearTransitionTimer();
  }

  async cancelSearch(): Promise<void> {
    if (this.isMatchTransitioning()) {
      return;
    }

    this.stopPolling();
    this.stopElapsedTimer();

    const leftQueue = await this.prematchFacade.leaveQueue();
    if (leftQueue) {
      await this.router.navigateByUrl(this.homeUrl);
    }
  }

  private async loadRoomState(): Promise<void> {
    const roomIsUsable = await this.prematchFacade.refreshRoomState();
    if (!roomIsUsable) {
      this.stopPolling();
      this.stopElapsedTimer();
      await this.router.navigateByUrl(this.homeUrl);
      return;
    }

    if (await this.navigateToReadyGame()) {
      this.stopPolling();
      return;
    }

    this.startPolling();
  }

  private async loadMatchedRoomFromNavigation(matchedUserId: string): Promise<void> {
    await this.prematchFacade.showMatchedRoom(matchedUserId, null);

    if (await this.navigateToReadyGame()) {
      this.stopPolling();
      return;
    }

    this.startPolling();
  }

  private startPolling(): void {
    if (this.pollTimerId !== null) {
      return;
    }

    this.pollTimerId = window.setInterval(() => {
      void this.pollRoomState();
    }, ROOM_POLL_INTERVAL_MS);
  }

  private stopPolling(): void {
    if (this.pollTimerId === null) {
      return;
    }

    window.clearInterval(this.pollTimerId);
    this.pollTimerId = null;
  }

  private startElapsedTimer(): void {
    if (this.elapsedTimerId !== null) {
      return;
    }

    this.elapsedTimerId = window.setInterval(() => {
      this.elapsedSeconds.set(Math.max(0, Math.floor((Date.now() - this.searchStartedAt) / 1000)));
    }, SEARCH_TIMER_INTERVAL_MS);
  }

  private stopElapsedTimer(): void {
    if (this.elapsedTimerId === null) {
      return;
    }

    window.clearInterval(this.elapsedTimerId);
    this.elapsedTimerId = null;
  }

  private async pollRoomState(): Promise<void> {
    if (this.isPolling || this.isMatchTransitioning()) {
      return;
    }

    this.isPolling = true;
    try {
      const roomIsUsable = await this.prematchFacade.refreshRoomState();

      if (!roomIsUsable) {
        this.stopPolling();
        this.stopElapsedTimer();
        await this.router.navigateByUrl(this.homeUrl);
        return;
      }

      if (await this.navigateToReadyGame()) {
        this.stopPolling();
      }
    } finally {
      this.isPolling = false;
    }
  }

  private async navigateToReadyGame(): Promise<boolean> {
    const gameId = this.state().queueStatus?.gameId;
    if (typeof gameId !== 'string' || gameId.length === 0) {
      return false;
    }

    await this.beginMatchFoundTransition(gameId);
    return true;
  }

  private async navigateToGame(gameId: string): Promise<void> {
    await this.router.navigate(['/', this.routes.game, gameId]);
  }

  private async handleRealtimeMessage(message: MatchmakingRealtimeMessage): Promise<void> {
    if (this.isMatchFoundMessage(message) && typeof message.gameId === 'string' && message.gameId.length > 0) {
      await this.beginMatchFoundTransition(message.gameId);
    }
  }

  private async beginMatchFoundTransition(gameId: string): Promise<void> {
    if (this.isMatchTransitioning()) {
      return;
    }

    this.stopPolling();
    this.stopElapsedTimer();
    this.isMatchTransitioning.set(true);
    this.clearTransitionTimer();

    this.transitionTimerId = window.setTimeout(() => {
      void this.navigateToGame(gameId);
    }, MATCH_FOUND_TRANSITION_MS);
  }

  private clearTransitionTimer(): void {
    if (this.transitionTimerId === null) {
      return;
    }

    window.clearTimeout(this.transitionTimerId);
    this.transitionTimerId = null;
  }

  private isMatchFoundMessage(message: MatchmakingRealtimeMessage): message is MatchFoundMessage {
    return 'opponentUserId' in message;
  }
}
