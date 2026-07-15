import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { DeckSummary } from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { PreMatchChecklistState } from '../../../../core/models/interfaces/play/prematch.interface';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { LanguageService } from '../../../../core/services/language.service';
import { StorageService } from '../../../../core/storage/storage.service';
import { GameManualModalComponent } from '../../components/game-manual-modal/game-manual-modal.component';
import { HomeActionsComponent } from '../../components/home-actions/home-actions.component';
import { HomeCustomMatchModalComponent } from '../../components/home-custom-match-modal/home-custom-match-modal.component';
import { HomeDeckSelectionModalComponent } from '../../components/home-deck-selection-modal/home-deck-selection-modal.component';
import { HomeErrorMessageComponent } from '../../components/home-error-message/home-error-message.component';
import { HomeHeroComponent } from '../../components/home-hero/home-hero.component';
import { HowToPlayCardComponent } from '../../components/how-to-play-card/how-to-play-card.component';
import { PrematchFacadeService } from '../../../play/services/prematch-facade.service';
import { OakTutorialService } from '../../services/oak-tutorial.service';
import { AppCardComponent, AppCardTone } from '../../../../shared/ui/layout/app-card/app-card.component';

type DeckActionKind = 'activating' | 'randomizing' | null;

@Component({
  selector: 'app-home-page',
  host: {
    '(document:keydown.escape)': 'closeOpenModals()'
  },
  imports: [
    HomeHeroComponent,
    HomeErrorMessageComponent,
    HomeActionsComponent,
    HowToPlayCardComponent,
    HomeCustomMatchModalComponent,
    HomeDeckSelectionModalComponent,
    GameManualModalComponent,
    AppCardComponent
  ],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomePageComponent {
  private static readonly PRIVATE_ROOM_CODE = 'SALA-2026';
  private readonly storageService = inject(StorageService);
  private readonly router = inject(Router);
  private readonly prematchFacade = inject(PrematchFacadeService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private readonly oakTutorial = inject(OakTutorialService);

  readonly routes = APP_ROUTES;
  readonly playRoomUrl = `/${APP_ROUTES.playRoom}`;
  readonly privateRoomCode = computed(() => this.currentUser()?.matchmakingCode ?? HomePageComponent.PRIVATE_ROOM_CODE);
  readonly currentUser = this.storageService.currentUser;
  readonly isAuthenticated = this.storageService.isAuthenticated;
  readonly prematchState = this.prematchFacade.checklistState;
  readonly hasActiveGame = this.prematchFacade.hasActiveGame;
  readonly activeDeck = computed<DeckSummary | null>(() => this.prematchState().activeDeck);
  readonly hasNoDecks = computed<boolean>(() => this.prematchState().availableDecks.length === 0);
  readonly isLightTheme = computed(() => this.appTheme.theme() === 'LIGHT');
  readonly isCustomMatchModalOpen = signal(false);
  readonly isDeckSelectionModalOpen = signal(false);
  readonly isManualOpen = signal(false);
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  private readonly deckAction = signal<{ kind: DeckActionKind; deckId: string | null }>({
    kind: null,
    deckId: null
  });
  readonly headline = computed(() => {
    this.languageService.translations();
    const username = this.currentUser()?.username?.toUpperCase();
    return username ? this.t('HOME.HEADLINE_USER', { username }) : this.t('HOME.HEADLINE');
  });

  cardTone(): AppCardTone {
    return this.isLightTheme() ? 'light' : 'dark';
  }

  constructor() {
    if (this.isAuthenticated()) {
      void this.prematchFacade.refreshChecklist();
    }
  }

  async primaryAction(): Promise<void> {
    if (this.isPrimaryDisabled()) {
      return;
    }

    if (this.prematchState().queueStatus?.gameId) {
      await this.router.navigateByUrl(`/${APP_ROUTES.game}/${this.prematchState().queueStatus?.gameId}`);
      return;
    }

    if (this.prematchState().queueStatus?.queued) {
      await this.router.navigateByUrl(this.playRoomUrl);
      return;
    }

    await this.prematchFacade.refreshChecklist();

    if (this.prematchState().queueStatus?.queued) {
      await this.router.navigateByUrl(this.playRoomUrl);
      return;
    }

    const queueStatus = await this.prematchFacade.joinQueue();
    if (!queueStatus) {
      if (!this.prematchState().checks.activeDeckFound || !this.prematchState().checks.deckValid) {
        this.openDeckSelectionModal();
      }
      return;
    }

    if (queueStatus.gameId) {
      await this.router.navigateByUrl(this.playRoomUrl, {
        state: { gameId: queueStatus.gameId }
      });
      return;
    }

    await this.router.navigateByUrl(this.playRoomUrl, {
      state: { matchedUserId: queueStatus.matchedUserId ?? null, gameId: queueStatus.gameId ?? null }
    });
  }

  primaryButtonLabel(): string {
    if (this.prematchState().status === 'checking') {
      return this.t('HOME.PRIMARY_CHECKING');
    }

    if (this.prematchState().status === 'joining') {
      return this.t('HOME.PRIMARY_JOINING');
    }

    if (this.prematchState().queueStatus?.gameId) {
      return this.t('HOME.PRIMARY_RESUME');
    }

    if (this.prematchState().queueStatus?.queued) {
      return this.t('HOME.PRIMARY_LOBBY');
    }

    return this.t('HOME.PRIMARY_SEARCH');
  }

  isPrimaryDisabled(): boolean {
    if (this.prematchState().queueStatus?.gameId || this.prematchState().queueStatus?.queued) {
      return false;
    }

    const hasNoDecks = this.prematchState().availableDecks.length === 0;
    return this.prematchState().status === 'checking' || this.prematchState().status === 'joining' || hasNoDecks;
  }

  openCustomMatchModal(): void {
    this.isCustomMatchModalOpen.set(true);
  }

  openDeckSelectionModal(): void {
    this.isDeckSelectionModalOpen.set(true);
    void this.prematchFacade.refreshChecklist();
  }

  closeCustomMatchModal(): void {
    this.isCustomMatchModalOpen.set(false);
  }

  closeDeckSelectionModal(): void {
    this.isDeckSelectionModalOpen.set(false);
  }

  closeOpenModals(): void {
    this.closeCustomMatchModal();
    this.closeDeckSelectionModal();
    this.isManualOpen.set(false);
  }

  async createCustomMatch(): Promise<void> {
    const queueStatus = await this.prematchFacade.createCustomQueue();
    if (queueStatus?.gameId) {
      await this.router.navigateByUrl(this.playRoomUrl, {
        state: { gameId: queueStatus.gameId }
      });
      return;
    }
    if (queueStatus) {
      await this.router.navigateByUrl(this.playRoomUrl, {
        state: { matchedUserId: queueStatus.matchedUserId ?? null, gameId: queueStatus.gameId ?? null }
      });
    }
  }

  async joinCustomMatch(code: string): Promise<void> {
    const queueStatus = await this.prematchFacade.joinCustomQueue(code);
    if (queueStatus?.gameId) {
      await this.router.navigateByUrl(this.playRoomUrl, {
        state: { gameId: queueStatus.gameId }
      });
      return;
    }
    if (queueStatus) {
      await this.router.navigateByUrl(this.playRoomUrl, {
        state: { matchedUserId: queueStatus.matchedUserId ?? null, gameId: queueStatus.gameId ?? null }
      });
    }
  }

  startOakTutorial(): void {
    this.closeOpenModals();
    this.oakTutorial.start();
  }

  async openDeckCreation(): Promise<void> {
    this.closeDeckSelectionModal();
    await this.router.navigate(['/', APP_ROUTES.deck], {
      queryParams: { mode: 'new' }
    });
  }

  async activateDeck(deckId: string): Promise<void> {
    this.deckAction.set({ kind: 'activating', deckId });

    try {
      await this.prematchFacade.activateDeck(deckId);
    } finally {
      this.deckAction.set({ kind: null, deckId: null });
    }
  }

  async randomizeDeck(deckId: string): Promise<void> {
    this.deckAction.set({ kind: 'randomizing', deckId });

    try {
      await this.prematchFacade.randomizeDeck(deckId);
    } finally {
      this.deckAction.set({ kind: null, deckId: null });
    }
  }

  checklistState(): PreMatchChecklistState {
    return this.prematchState();
  }

  deckActionKind(): DeckActionKind {
    return this.deckAction().kind;
  }

  deckActionDeckId(): string | null {
    return this.deckAction().deckId;
  }

  handleManualClick(): void {
    this.isManualOpen.set(true);
  }
}
