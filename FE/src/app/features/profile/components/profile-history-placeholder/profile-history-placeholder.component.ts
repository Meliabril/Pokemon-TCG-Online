import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { input } from '@angular/core';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';
import {
  MatchHistory,
  MatchHistoryFilter,
  MatchResult,
  PlayerStats
} from '../../../../core/models/interfaces/match-history/match-history.interface';
import { MatchHistoryService } from '../../../../infrastructure/api/match-history/match-history.service';

type HistoryFilterOption = {
  value: MatchHistoryFilter;
  label: string;
};

const HISTORY_PAGE_SIZE = 5;
const EMPTY_PLAYER_STATS: PlayerStats = {
  totalMatches: 0,
  wins: 0,
  losses: 0,
  winRate: 0,
  currentStreak: 0
};

@Component({
  selector: 'app-profile-history-placeholder',
  templateUrl: './profile-history-placeholder.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileHistoryPlaceholderComponent {
  private readonly languageService = inject(LanguageService);
  private readonly matchHistoryService = inject(MatchHistoryService);

  readonly isLightTheme = input(false);
  readonly activeFilter = signal<MatchHistoryFilter>('ALL');
  readonly stats = signal<PlayerStats | null>(null);
  readonly matches = signal<MatchHistory[]>([]);
  readonly currentPage = signal(0);
  readonly isStatsLoading = signal(false);
  readonly isHistoryLoading = signal(false);
  readonly isLoadingMore = signal(false);
  readonly statsErrorMessage = signal('');
  readonly historyErrorMessage = signal('');
  readonly lastPage = signal(true);
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);
  readonly displayedStats = computed<PlayerStats>(() => this.stats() ?? EMPTY_PLAYER_STATS);
  readonly canLoadMore = computed(
    () => !this.lastPage() && !this.isHistoryLoading() && !this.isLoadingMore()
  );
  readonly isInitialHistoryLoading = computed(
    () => this.isHistoryLoading() && this.matches().length === 0
  );
  readonly filters = computed<HistoryFilterOption[]>(() => {
    this.languageService.translations();
    return [
      { value: 'ALL', label: this.t('PROFILE.HISTORY.FILTERS.ALL') },
      { value: 'WINS', label: this.t('PROFILE.HISTORY.FILTERS.WINS') },
      { value: 'LOSSES', label: this.t('PROFILE.HISTORY.FILTERS.LOSSES') },
      { value: 'LAST_7_DAYS', label: this.t('PROFILE.HISTORY.FILTERS.LAST_7') },
      { value: 'LAST_30_DAYS', label: this.t('PROFILE.HISTORY.FILTERS.LAST_30') }
    ];
  });

  constructor() {
    this.loadStats();
    this.loadHistory(true);
  }

  selectFilter(filter: MatchHistoryFilter): void {
    if (this.activeFilter() === filter) {
      return;
    }

    this.activeFilter.set(filter);
    this.loadHistory(true);
  }

  loadMore(): void {
    if (!this.canLoadMore()) {
      return;
    }

    this.loadHistory(false);
  }

  retryHistory(): void {
    this.loadHistory(true);
  }

  filterClasses(filter: MatchHistoryFilter): string {
    if (this.activeFilter() === filter) {
      return 'border-[#B8792E]/45 bg-[#D9A441]/14 text-[#D9A441]';
    }

    if (this.isLightTheme()) {
      return 'border-[#8a4b20]/70 bg-[#fff8e3]/82 text-[#6f4d35] hover:border-[#8b070c]/35 hover:text-[#4a2a17]';
    }

    return 'border-[#8a4b20]/70 bg-[#2a0805]/72 text-[#d8b982] hover:border-[#B8792E]/35 hover:text-[#D9A441]';
  }

  matchCardClasses(match: MatchHistory): string {
    if (this.isLightTheme()) {
      if (match.result === 'VICTORIA') {
        return 'bg-[#fff8e3]/90 hover:border-yellow-500/70 hover:shadow-[0_0_15px_rgba(250,204,21,0.25)]';
      }

      return 'bg-[#fff8e3]/90 hover:border-red-500/70 hover:shadow-[0_0_15px_rgba(239,68,68,0.25)]';
    }

    if (match.result === 'VICTORIA') {
      return 'bg-[#3E1D1D]/88 dark:bg-[#3E1D1D] hover:border-yellow-500/70 hover:shadow-[0_0_15px_rgba(250,204,21,0.25)]';
    }

    return 'bg-[#3E1D1D]/88 dark:bg-red-950 hover:border-red-500/70 hover:shadow-[0_0_15px_rgba(239,68,68,0.25)]';
  }

  resultBadgeClasses(result: MatchResult): string {
    if (result === 'VICTORIA') {
      return 'border-[#22c55e]/45 bg-[#166534]/72 text-[#dcfce7]';
    }

    return 'border-[#ef4444]/45 bg-[#7f1d1d]/72 text-[#fee2e2]';
  }

  iconPlaceholderClasses(result: MatchResult): string {
    if (result === 'VICTORIA') {
      return 'border-[#22c55e]/45 bg-[#22c55e]/12 text-[#86efac]';
    }

    return 'border-[#ef4444]/45 bg-[#ef4444]/12 text-[#fca5a5]';
  }

  resultLabel(result: MatchResult): string {
    if (result === 'VICTORIA') {
      return this.t('PROFILE.HISTORY.RESULTS.VICTORY');
    }

    return this.t('PROFILE.HISTORY.RESULTS.DEFEAT');
  }

  winRateLabel(): string {
    return `${this.displayedStats().winRate.toFixed(2)}%`;
  }

  formattedDate(date: string): string {
    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) {
      return date;
    }

    const locale = this.languageService.language() === 'es' ? 'es-AR' : 'en-US';
    return new Intl.DateTimeFormat(locale, {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(parsedDate);
  }

  private loadStats(): void {
    this.isStatsLoading.set(true);
    this.statsErrorMessage.set('');

    this.matchHistoryService.getPlayerStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.isStatsLoading.set(false);
      },
      error: (error: unknown) => {
        this.statsErrorMessage.set(this.normalizeError(error));
        this.isStatsLoading.set(false);
      }
    });
  }

  private loadHistory(reset: boolean): void {
    const nextPage = reset ? 0 : this.currentPage() + 1;

    if (reset) {
      this.matches.set([]);
      this.currentPage.set(0);
      this.lastPage.set(true);
      this.isHistoryLoading.set(true);
    } else {
      this.isLoadingMore.set(true);
    }

    this.historyErrorMessage.set('');

    this.matchHistoryService.getHistory(this.activeFilter(), nextPage, HISTORY_PAGE_SIZE).subscribe({
      next: (response) => {
        if (reset) {
          this.matches.set(response.items);
        } else {
          this.matches.update((matches) => [...matches, ...response.items]);
        }

        this.currentPage.set(response.page);
        this.lastPage.set(response.last);
        this.isHistoryLoading.set(false);
        this.isLoadingMore.set(false);
      },
      error: (error: unknown) => {
        this.historyErrorMessage.set(this.normalizeError(error));
        this.isHistoryLoading.set(false);
        this.isLoadingMore.set(false);
      }
    });
  }

  private normalizeError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return this.t('PROFILE.HISTORY.CONNECTION_ERROR');
    }

    return this.t('PROFILE.HISTORY.LOAD_ERROR');
  }
}
