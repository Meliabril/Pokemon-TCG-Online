import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DriveStep, Driver, DriverHook, PopoverDOM, driver } from 'driver.js';
import { APP_ROUTES } from '../../../core/constants/routing/routes.constants';
import { LanguageService } from '../../../core/services/language.service';

type DeckTutorialMode = 'empty' | 'create' | 'with-decks' | 'edit';
type TutorialTarget = string | (() => Element | null);
type StepButtons = Array<'next' | 'previous' | 'close'>;
type DialogDirection =
  | 'top'
  | 'bottom'
  | 'left'
  | 'right'
  | 'center'
  | 'top-left'
  | 'top-right'
  | 'bottom-left'
  | 'bottom-right';

// Minimum gap kept between the dialog/professor and the highlighted target on mobile landscape.
const MOBILE_TARGET_GAP = 12;
const MOBILE_TUTORIAL_MEDIA_QUERY = '(max-width: 1024px) and (orientation: landscape)';

type TutorialStepConfig = {
  target: TutorialTarget;
  titleKey: string;
  descriptionKey: string;
  disableActiveInteraction?: boolean;
  showButtons?: StepButtons;
  shouldShow?: () => boolean;
  onHighlight?: (tour: Driver) => void;
  onNextClick?: (tour: Driver) => void;
};

@Injectable({ providedIn: 'root' })
export class OakTutorialService {
  private readonly languageService = inject(LanguageService);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly professorImages = [
    '/assets/tutorial/profesor1.png',
    '/assets/tutorial/profesor2.png',
    '/assets/tutorial/profesor3.png',
    '/assets/tutorial/profesor4.png',
    '/assets/tutorial/profesor5.png'
  ];

  private activeTour: Driver | null = null;
  private lastProfessorImage: string | null = null;
  private listenerCleanups: Array<() => void> = [];

  start(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.destroyActiveTour();
    this.lastProfessorImage = null;

    if (this.isDeckRoute()) {
      this.startDeckTutorial();
      return;
    }

    if (this.isPokedexRoute()) {
      this.startPokedexTutorial();
      return;
    }

    if (this.isProfileRoute()) {
      this.startProfileTutorial();
      return;
    }

    if (this.isHomeRoute()) {
      this.startHomeTutorial();
    }
  }

  private startHomeTutorial(): void {
    const tour = driver({
      ...this.commonConfig(),
      steps: this.buildHomeSteps()
    });

    this.activeTour = tour;
    tour.drive();
  }

  private startDeckTutorial(): void {
    const mode = this.resolveDeckTutorialMode();
    if (!mode) {
      return;
    }

    const tour = driver({
      ...this.commonConfig(),
      steps: this.buildDeckSteps(mode)
    });

    this.activeTour = tour;
    tour.drive();
  }

  private startPokedexTutorial(): void {
    const tour = driver({
      ...this.commonConfig(),
      steps: this.buildPokedexSteps()
    });

    this.activeTour = tour;
    tour.drive();
  }

  private startProfileTutorial(): void {
    const isHistoryVisible = this.queryElement('[data-tour="profile-history-card"]') !== null;
    const tour = driver({
      ...this.commonConfig(),
      steps: isHistoryVisible ? this.buildProfileHistorySteps(false) : this.buildProfileTrainerSteps()
    });

    this.activeTour = tour;
    tour.drive();
  }

  private commonConfig() {
    return {
      animate: true,
      allowClose: true,
      allowKeyboardControl: true,
      disableActiveInteraction: true,
      overlayColor: '#080b16',
      overlayOpacity: 0.55,
      overlayClickBehavior: () => undefined,
      popoverClass: 'oak-tutorial-popover',
      popoverOffset: 18,
      stagePadding: 8,
      stageRadius: 12,
      showButtons: ['next', 'previous', 'close'] as StepButtons,
      nextBtnText: this.t('OAK_TUTORIAL.BUTTONS.CONTINUE'),
      prevBtnText: this.t('OAK_TUTORIAL.BUTTONS.PREVIOUS'),
      doneBtnText: this.t('OAK_TUTORIAL.BUTTONS.FINISH'),
      onPopoverRender: (popover: PopoverDOM, options: { driver: Driver }) =>
        this.decoratePopover(popover, options.driver),
      onDestroyed: () => this.finishTour()
    };
  }

  private buildHomeSteps(): DriveStep[] {
    return [
      this.step('[data-tour="main-panel"]', 'OAK_TUTORIAL.STEPS.WELCOME.TITLE', 'OAK_TUTORIAL.STEPS.WELCOME.DESCRIPTION'),
      this.step('[data-tour="navbar"]', 'OAK_TUTORIAL.STEPS.NAVBAR.TITLE', 'OAK_TUTORIAL.STEPS.NAVBAR.DESCRIPTION'),
      this.step('[data-tour="nav-home"]', 'OAK_TUTORIAL.STEPS.HOME.TITLE', 'OAK_TUTORIAL.STEPS.HOME.DESCRIPTION'),
      this.step('[data-tour="nav-deck"]', 'OAK_TUTORIAL.STEPS.DECK_NAV.TITLE', 'OAK_TUTORIAL.STEPS.DECK_NAV.DESCRIPTION'),
      this.step('[data-tour="nav-pokedex"]', 'OAK_TUTORIAL.STEPS.POKEDEX.TITLE', 'OAK_TUTORIAL.STEPS.POKEDEX.DESCRIPTION'),
      this.step('[data-tour="language-switch"]', 'OAK_TUTORIAL.STEPS.LANGUAGE.TITLE', 'OAK_TUTORIAL.STEPS.LANGUAGE.DESCRIPTION'),
      this.step('[data-tour="user-status"]', 'OAK_TUTORIAL.STEPS.USER_STATUS.TITLE', 'OAK_TUTORIAL.STEPS.USER_STATUS.DESCRIPTION'),
      this.step('[data-tour="main-panel"]', 'OAK_TUTORIAL.STEPS.MAIN_PANEL.TITLE', 'OAK_TUTORIAL.STEPS.MAIN_PANEL.DESCRIPTION'),
      this.step('[data-tour="active-deck-status"]', 'OAK_TUTORIAL.STEPS.INVALID_DECK.TITLE', 'OAK_TUTORIAL.STEPS.INVALID_DECK.DESCRIPTION'),
      this.homeDeckActionStep(),
      this.step('[data-tour="search-match"]', 'OAK_TUTORIAL.STEPS.SEARCH_MATCH.TITLE', 'OAK_TUTORIAL.STEPS.SEARCH_MATCH.DESCRIPTION'),
      this.step('[data-tour="custom-match"]', 'OAK_TUTORIAL.STEPS.CUSTOM_MATCH.TITLE', 'OAK_TUTORIAL.STEPS.CUSTOM_MATCH.DESCRIPTION'),
      this.step('[data-tour="how-to-play"]', 'OAK_TUTORIAL.STEPS.HOW_TO_PLAY.TITLE', 'OAK_TUTORIAL.STEPS.HOW_TO_PLAY.DESCRIPTION'),
      this.step('[data-tour="objective"]', 'OAK_TUTORIAL.STEPS.OBJECTIVE.TITLE', 'OAK_TUTORIAL.STEPS.OBJECTIVE.DESCRIPTION'),
      this.step('[data-tour="turn"]', 'OAK_TUTORIAL.STEPS.TURN.TITLE', 'OAK_TUTORIAL.STEPS.TURN.DESCRIPTION'),
      this.step('[data-tour="conditions"]', 'OAK_TUTORIAL.STEPS.CONDITIONS.TITLE', 'OAK_TUTORIAL.STEPS.CONDITIONS.DESCRIPTION'),
      this.step('[data-tour="manual"]', 'OAK_TUTORIAL.STEPS.MANUAL.TITLE', 'OAK_TUTORIAL.STEPS.MANUAL.DESCRIPTION'),
      this.step('[data-tour="main-panel"]', 'OAK_TUTORIAL.STEPS.FINISH.TITLE', 'OAK_TUTORIAL.STEPS.FINISH.DESCRIPTION')
    ];
  }

  private buildPokedexSteps(): DriveStep[] {
    return this.isMobileTutorial() ? this.buildPokedexMobileSteps() : this.buildPokedexDesktopSteps();
  }

  private buildPokedexDesktopSteps(): DriveStep[] {
    const pageFallback = (): Element | null => this.queryElement('[data-tour="pokedex-page"]');
    const detailFallback = (): Element | null =>
      this.queryElement('[data-tour="pokedex-card-info"]') ?? pageFallback();

    return [
      this.step(
        '[data-tour="pokedex-title"]',
        'OAK_TUTORIAL.POKEDEX_GUIDE.INTRO.TITLE',
        'OAK_TUTORIAL.POKEDEX_GUIDE.INTRO.DESCRIPTION'
      ),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-card-image"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_IMAGE.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_IMAGE.DESCRIPTION'
      }),
      this.step({
        target: detailFallback,
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_INFO.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_INFO.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-empty-data"]') ?? detailFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.EMPTY_DATA.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.EMPTY_DATA.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-attacks"]') ?? detailFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.ATTACKS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.ATTACKS.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-search"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SEARCH.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SEARCH.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-filters"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FILTERS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FILTERS.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-card-list"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_LIST.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.CARD_LIST.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () =>
          this.queryElement('[data-tour="pokedex-card-list-item"]') ??
          this.queryElement('[data-tour="pokedex-card-list"]') ??
          pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SELECT_CARD.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SELECT_CARD.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-card-image"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FINISH.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FINISH.DESCRIPTION'
      })
    ];
  }

  /**
   * Mobile Pokedex flow: there is no fixed detail panel here. Cards live in a grid and tapping
   * one opens a modal with the full card info, so the tutorial has to wait for a real tap and
   * then point at the modal instead of the desktop-only detail panel.
   */
  private buildPokedexMobileSteps(): DriveStep[] {
    const pageFallback = (): Element | null => this.queryElement('[data-tour="pokedex-page"]');
    const gridFallback = (): Element | null =>
      this.queryElement('[data-tour="pokedex-card-list"]') ?? pageFallback();
    const modalFallback = (): Element | null => this.queryVisibleElement('[data-tour="pokedex-mobile-card-modal"]');

    return [
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-title"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.INTRO.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.INTRO.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-search"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SEARCH.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.SEARCH.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-filters"]') ?? pageFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FILTERS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.FILTERS.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: gridFallback,
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CARD_GRID.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CARD_GRID.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="pokedex-card-list-item"]') ?? gridFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.OPEN_CARD.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.OPEN_CARD.DESCRIPTION',
        disableActiveInteraction: false,
        showButtons: ['previous', 'close'],
        shouldShow: () => this.queryElement('[data-tour="pokedex-card-list-item"]') !== null,
        onHighlight: () =>
          this.enableTargetInteraction({
            target: '[data-tour="pokedex-card-list-item"]',
            afterClick: () => {
              void this.waitForElement(() => modalFallback()).then((modal) => {
                if (modal) {
                  this.activeTour?.moveNext();
                }
              });
            }
          })
      }),
      this.step({
        target: () => modalFallback() ?? gridFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CARD_MODAL.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CARD_MODAL.DESCRIPTION',
        disableActiveInteraction: false,
        shouldShow: () => modalFallback() !== null
      }),
      this.step({
        target: () =>
          this.queryVisibleElement('[data-tour="pokedex-mobile-card-modal-close"]') ?? modalFallback() ?? gridFallback(),
        titleKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CLOSE_MODAL.TITLE',
        descriptionKey: 'OAK_TUTORIAL.POKEDEX_GUIDE.MOBILE.CLOSE_MODAL.DESCRIPTION',
        disableActiveInteraction: false,
        shouldShow: () => modalFallback() !== null
      })
    ];
  }

  private buildProfileTrainerSteps(): DriveStep[] {
    const pageFallback = (): Element | null => this.queryElement('[data-tour="profile-page"]');
    const trainerFallback = (): Element | null =>
      this.queryElement('[data-tour="profile-trainer-card"]') ?? pageFallback();

    return [
      this.step({
        target: trainerFallback,
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WELCOME.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WELCOME.DESCRIPTION'
      }),
      this.step({
        target: trainerFallback,
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.TRAINER_DATA.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.TRAINER_DATA.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-avatar"]') ?? trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.AVATAR.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.AVATAR.DESCRIPTION',
        disableActiveInteraction: false,
        onHighlight: () =>
          this.enableTargetInteraction({
            target: '[data-tour="profile-avatar"]',
            afterClick: () => undefined
          }),
        onNextClick: (tour) => {
          this.queryElement('[data-tour="profile-close-avatar"]')?.click();
          tour.moveNext();
        }
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-username"]') ?? trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.USERNAME.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.USERNAME.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-edit-username"]') ?? trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.EDIT_USERNAME.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.EDIT_USERNAME.DESCRIPTION',
        disableActiveInteraction: false,
        showButtons: ['previous', 'close'],
        shouldShow: () => this.queryElement('[data-tour="profile-edit-username"]') !== null,
        onHighlight: (tour) => {
          if (this.queryElement('[data-tour="profile-edit-username"]')) {
            this.enableTargetInteraction({
              target: '[data-tour="profile-edit-username"]',
              afterClick: () => {
                void this.waitForElement(() => this.queryElement('[data-tour="profile-username-actions"]')).then(() => {
                  tour.moveNext();
                });
              }
            });
          }
        }
      }),
      this.step({
        target: () =>
          this.queryElement('[data-tour="profile-username-actions"]') ??
          this.queryElement('[data-tour="profile-username"]') ??
          trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.SAVE_CANCEL_USERNAME.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.SAVE_CANCEL_USERNAME.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-email-verified"]') ?? trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.EMAIL_VERIFIED.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.EMAIL_VERIFIED.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-change-password"]') ?? trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.CHANGE_PASSWORD.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.CHANGE_PASSWORD.DESCRIPTION'
      }),
      this.step({
        target: () =>
          this.queryElement('[data-tour="profile-password-panel"]') ??
          this.queryElement('[data-tour="profile-change-password"]') ??
          trainerFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.PASSWORD_CODE.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.PASSWORD_CODE.DESCRIPTION'
      }),
      this.step({
        target: '[data-tour="profile-history-tab"]',
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.OPEN_HISTORY.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.OPEN_HISTORY.DESCRIPTION',
        disableActiveInteraction: false,
        showButtons: ['previous', 'close'],
        onHighlight: () =>
          this.enableTargetInteraction({
            target: '[data-tour="profile-history-tab"]',
            afterClick: () => void this.continueInProfileHistory()
          })
      })
    ];
  }

  private buildProfileHistorySteps(disablePreviousOnFirst: boolean): DriveStep[] {
    const historyFallback = (): Element | null =>
      this.queryElement('[data-tour="profile-history-card"]') ??
      this.queryElement('[data-tour="profile-page"]');

    return [
      this.step(
        '[data-tour="profile-history-card"]',
        'OAK_TUTORIAL.PROFILE_GUIDE.HISTORY_OVERVIEW.TITLE',
        'OAK_TUTORIAL.PROFILE_GUIDE.HISTORY_OVERVIEW.DESCRIPTION',
        { showButtons: disablePreviousOnFirst ? ['next', 'close'] : undefined }
      ),
      this.step({
        target: () => this.queryElement('[data-tour="profile-total-games"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.TOTAL_GAMES.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.TOTAL_GAMES.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-wins"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WINS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WINS.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-losses"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.LOSSES.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.LOSSES.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-win-rate"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WIN_RATE.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.WIN_RATE.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-current-streak"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.CURRENT_STREAK.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.CURRENT_STREAK.DESCRIPTION'
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-history-filters"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.HISTORY_FILTERS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.HISTORY_FILTERS.DESCRIPTION',
        disableActiveInteraction: false
      }),
      this.step({
        target: () => this.queryElement('[data-tour="profile-match-list"]') ?? historyFallback(),
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.MATCH_LIST.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.MATCH_LIST.DESCRIPTION'
      }),
      this.step({
        target: historyFallback,
        titleKey: 'OAK_TUTORIAL.PROFILE_GUIDE.FINISH.TITLE',
        descriptionKey: 'OAK_TUTORIAL.PROFILE_GUIDE.FINISH.DESCRIPTION'
      })
    ];
  }

  private buildDeckSteps(mode: DeckTutorialMode): DriveStep[] {
    if (mode === 'empty') {
      return [
        this.step('[data-tour="deck-page"]', 'OAK_TUTORIAL.DECK_GUIDE.EMPTY.WELCOME.TITLE', 'OAK_TUTORIAL.DECK_GUIDE.EMPTY.WELCOME.DESCRIPTION'),
        this.deckCreateFromEmptyStep()
      ];
    }

    if (mode === 'create') {
      return this.buildDeckBuilderSteps(false);
    }

    if (mode === 'edit') {
      return this.buildDeckBuilderSteps(true);
    }

    return [
      this.step({
        target: '[data-tour="deck-active-banner"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVE_DECK.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVE_DECK.DESCRIPTION',
        shouldShow: () => this.queryElement('[data-tour="deck-active-banner"]') !== null
      }),
      this.step(
        '[data-tour="deck-card"]',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.DECK_CARD.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.DECK_CARD.DESCRIPTION'
      ),
      this.step(
        '[data-tour="deck-card-stats"]',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.STATS.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.STATS.DESCRIPTION'
      ),
      this.step({
        target: '[data-tour="deck-activate-button"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVATE.TITLE',
        descriptionKey: this.deckActivateDescriptionKey(),
        shouldShow: () => this.queryElement('[data-tour="deck-activate-button"]') !== null
      }),
      this.step(
        '[data-tour="deck-delete-button"]',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.DELETE.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.EXISTING.DELETE.DESCRIPTION'
      ),
      this.step({
        target: '[data-tour="deck-edit-button"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.EDIT.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.EDIT.DESCRIPTION',
        disableActiveInteraction: false,
        onHighlight: () =>
          this.enableTargetInteraction({
            target: '[data-tour="deck-edit-button"]',
            afterClick: () => this.continueInDeckEditor(true)
          })
      }),
      this.step({
        target: '[data-tour="deck-active-cards"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVE_CARDS.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVE_CARDS.DESCRIPTION',
        shouldShow: () => this.queryElement('[data-tour="deck-active-cards"]') !== null
      }),
      this.step({
        target: '[data-tour="deck-active-card"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.POKEDEX.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.POKEDEX.DESCRIPTION',
        disableActiveInteraction: false,
        showButtons: ['previous', 'close'],
        shouldShow: () => this.queryElement('[data-tour="deck-active-card"]') !== null,
        onHighlight: () =>
          this.enableTargetInteraction({
            target: '[data-tour="deck-active-card"]',
            afterClick: (target) => this.continuePokedexTutorialFromDeck(target)
          })
      })
    ];
  }

  private buildDeckBuilderSteps(isEditFlow: boolean, disablePreviousOnFirst = false): DriveStep[] {
    return this.isMobileTutorial()
      ? this.buildDeckBuilderMobileSteps(isEditFlow, disablePreviousOnFirst)
      : this.buildDeckBuilderDesktopSteps(isEditFlow, disablePreviousOnFirst);
  }

  private buildDeckBuilderDesktopSteps(isEditFlow: boolean, disablePreviousOnFirst = false): DriveStep[] {
    const steps: DriveStep[] = [
      ...this.buildDeckBuilderHeaderSteps(isEditFlow, disablePreviousOnFirst),
      ...this.buildDeckBuilderCatalogIntroSteps(),
      this.deckAddCardStep('OAK_TUTORIAL.DECK_GUIDE.BUILDER.ADD_CARD.TITLE', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.ADD_CARD.DESCRIPTION'),
      this.step({
        target: '[data-tour="deck-selected-list"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SELECTED_LIST.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SELECTED_LIST.DESCRIPTION',
        shouldShow: () => this.queryElement('[data-tour="deck-selected-list"]') !== null
      }),
      this.step('[data-tour="deck-rules"]', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RULES.TITLE', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RULES.DESCRIPTION'),
      this.step(
        '[data-tour="deck-count"]',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.COUNT.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.COUNT.DESCRIPTION'
      ),
      this.step({
        target: '[data-tour="deck-card-quantity-controls"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.QUANTITY.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.QUANTITY.DESCRIPTION',
        disableActiveInteraction: false,
        shouldShow: () => this.queryElement('[data-tour="deck-card-quantity-controls"]') !== null,
        onHighlight: () => this.enableLayoutSafeHighlight('[data-tour="deck-card-quantity-controls"]')
      }),
      this.step({
        target: () => this.queryElement('[data-tour="deck-card-type-highlight"]') ?? this.queryElement('[data-tour="deck-first-card"]'),
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.TYPE_HIGHLIGHT.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.TYPE_HIGHLIGHT.DESCRIPTION'
      })
    ];

    if (isEditFlow) {
      steps.push(
        this.step(
          '[data-tour="deck-editor-root"]',
          'OAK_TUTORIAL.DECK_GUIDE.EXISTING.FINISH.TITLE',
          'OAK_TUTORIAL.DECK_GUIDE.EXISTING.FINISH.DESCRIPTION'
        )
      );
    }

    return steps;
  }

  /**
   * Steps shared verbatim by desktop and mobile: deck name, random, clear and save all live in
   * the same header on both layouts, so there is no mobile-specific variant for them.
   */
  private buildDeckBuilderHeaderSteps(isEditFlow: boolean, disablePreviousOnFirst: boolean): DriveStep[] {
    return [
      this.step(
        '[data-tour="deck-name-input"]',
        isEditFlow ? 'OAK_TUTORIAL.DECK_GUIDE.EDITOR.WELCOME.TITLE' : 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.NAME.TITLE',
        isEditFlow
          ? 'OAK_TUTORIAL.DECK_GUIDE.EDITOR.WELCOME.DESCRIPTION'
          : 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.NAME.DESCRIPTION',
        {
          disableActiveInteraction: false,
          showButtons: disablePreviousOnFirst ? ['next', 'close'] : undefined
        }
      ),
      this.step(
        '[data-tour="deck-random-button"]',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RANDOM.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RANDOM.DESCRIPTION',
        { disableActiveInteraction: false }
      ),
      this.step('[data-tour="deck-clear-button"]', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.CLEAR.TITLE', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.CLEAR.DESCRIPTION'),
      this.step('[data-tour="deck-save-button"]', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SAVE.TITLE', 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SAVE.DESCRIPTION')
    ];
  }

  /**
   * Catalog intro shared verbatim by desktop and mobile: the card library, search and type
   * filters are the same element on both layouts.
   */
  private buildDeckBuilderCatalogIntroSteps(): DriveStep[] {
    return [
      this.step(
        '[data-tour="deck-card-catalog"]',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.CARD_LIBRARY.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.CARD_LIBRARY.DESCRIPTION'
      ),
      this.step(
        '[data-tour="deck-search-input"]',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SEARCH.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.SEARCH.DESCRIPTION',
        { disableActiveInteraction: false }
      ),
      this.step(
        '[data-tour="deck-type-filters"]',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.FILTERS.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.FILTERS.DESCRIPTION',
        { disableActiveInteraction: false }
      )
    ];
  }

  /**
   * "Add a card" step shared by desktop and mobile. Only the copy differs (mobile explains there
   * is no fixed side list), the target resolution and the real-click validation logic are identical.
   */
  private deckAddCardStep(titleKey: string, descriptionKey: string): DriveStep {
    return this.step({
      target: () => this.queryElement('[data-tour="deck-first-card"]') ?? this.queryElement('[data-tour="deck-card-catalog"]'),
      titleKey,
      descriptionKey,
      disableActiveInteraction: false,
      onNextClick: (driver) => {
        const hasSelectedCard = !!this.queryElement('[data-tour="deck-card-quantity-controls"]');
        if (hasSelectedCard) {
          driver.moveNext();
          return;
        }

        const hasCardsAvailable = !!this.queryElement('[data-tour="deck-first-card"]');
        const messageKey = hasCardsAvailable
          ? 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.ADD_CARD.REQUIRED'
          : 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.ADD_CARD.NO_CARDS';

        const dialogText = this.document.querySelector('.oak-dialog-text');
        if (dialogText) {
          dialogText.textContent = this.t(messageKey);
        }
      }
    });
  }

  /**
   * Mobile Deck builder flow: there is no fixed selected-cards panel next to the catalog like on
   * desktop. Adding a card still updates the draft normally, but reviewing/adjusting quantities
   * happens in a collapsible panel ("desplegable") toggled by a dedicated button, so the tutorial
   * opens that panel explicitly before pointing at anything that lives inside it.
   */
  private buildDeckBuilderMobileSteps(isEditFlow: boolean, disablePreviousOnFirst = false): DriveStep[] {
    const panelStepShouldShow = (selector: string): (() => boolean) => () =>
      this.isMobileDeckPanelOpen() && this.queryElement(selector) !== null;

    const steps: DriveStep[] = [
      ...this.buildDeckBuilderHeaderSteps(isEditFlow, disablePreviousOnFirst),
      ...this.buildDeckBuilderCatalogIntroSteps(),
      this.deckAddCardStep(
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.ADD_CARD.TITLE',
        'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.ADD_CARD.DESCRIPTION'
      ),
      this.step({
        target: '[data-tour="deck-mobile-panel-toggle"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.OPEN_PANEL.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.OPEN_PANEL.DESCRIPTION',
        disableActiveInteraction: false,
        showButtons: ['previous', 'close'],
        // The panel can already be open (e.g. it defaults to open, or the trainer toggled it
        // earlier), so this step is skipped instead of asking them to close it by mistake.
        shouldShow: () => !this.isMobileDeckPanelOpen() && this.queryElement('[data-tour="deck-mobile-panel-toggle"]') !== null,
        onHighlight: (tour) =>
          this.enableTargetInteraction({
            target: '[data-tour="deck-mobile-panel-toggle"]',
            afterClick: () => {
              void this.waitForElement(() =>
                this.isMobileDeckPanelOpen() ? this.queryElement('[data-tour="deck-mobile-panel-toggle"]') : null
              ).then((opened) => {
                if (opened) {
                  tour.moveNext();
                }
              });
            }
          })
      }),
      this.step({
        target: '[data-tour="deck-selected-list"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.CURRENT_DECK.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.CURRENT_DECK.DESCRIPTION',
        shouldShow: panelStepShouldShow('[data-tour="deck-selected-list"]')
      }),
      this.step({
        target: '[data-tour="deck-rules"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RULES.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.RULES.DESCRIPTION',
        shouldShow: panelStepShouldShow('[data-tour="deck-rules"]')
      }),
      this.step({
        target: '[data-tour="deck-count"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.COUNT.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.COUNT.DESCRIPTION',
        shouldShow: panelStepShouldShow('[data-tour="deck-count"]')
      }),
      this.step({
        target: '[data-tour="deck-card-quantity-controls"]',
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.QUANTITY.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.MOBILE.QUANTITY.DESCRIPTION',
        disableActiveInteraction: false,
        shouldShow: panelStepShouldShow('[data-tour="deck-card-quantity-controls"]'),
        onHighlight: () => this.enableLayoutSafeHighlight('[data-tour="deck-card-quantity-controls"]')
      }),
      this.step({
        target: () => this.queryElement('[data-tour="deck-card-type-highlight"]') ?? this.queryElement('[data-tour="deck-selected-list"]'),
        titleKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.TYPE_HIGHLIGHT.TITLE',
        descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.BUILDER.TYPE_HIGHLIGHT.DESCRIPTION',
        shouldShow: panelStepShouldShow('[data-tour="deck-card-type-highlight"]')
      })
    ];

    if (isEditFlow) {
      steps.push(
        this.step(
          '[data-tour="deck-editor-root"]',
          'OAK_TUTORIAL.DECK_GUIDE.EXISTING.FINISH.TITLE',
          'OAK_TUTORIAL.DECK_GUIDE.EXISTING.FINISH.DESCRIPTION'
        )
      );
    }

    return steps;
  }

  private deckCreateFromEmptyStep(): DriveStep {
    return this.step({
      target: '[data-tour="deck-empty-slot"]',
      titleKey: 'OAK_TUTORIAL.DECK_GUIDE.EMPTY.SLOTS.TITLE',
      descriptionKey: 'OAK_TUTORIAL.DECK_GUIDE.EMPTY.SLOTS.DESCRIPTION',
      disableActiveInteraction: false,
      showButtons: ['previous', 'close'],
      onHighlight: () =>
        this.enableProxyInteraction({
          target: '[data-tour="deck-empty-slot"]',
          labelKey: 'OAK_TUTORIAL.DECK_GUIDE.EMPTY.SLOTS.TITLE',
          afterClick: () => {
            this.continueInDeckEditor(false);
          }
        })
    });
  }

  private continueInDeckEditor(isEditFlow: boolean): void {
    void this.waitForElement(() => this.queryElement('[data-tour="deck-editor-root"]')).then((editor) => {
      if (!editor) {
        return;
      }

      this.destroyActiveTour();
      const tour = driver({
        ...this.commonConfig(),
        steps: this.buildDeckBuilderSteps(isEditFlow, true)
      });
      this.activeTour = tour;
      tour.drive();
    });
  }

  private continuePokedexTutorialFromDeck(cardLink: HTMLElement): void {
    const href = cardLink?.getAttribute('href');
    const cardId = href ? new URL(href, this.document.baseURI).searchParams.get('cardId') : null;

    this.destroyActiveTour();
    void this.router.navigate(['/', APP_ROUTES.pokedex], {
      queryParams: {
        ...(cardId ? { cardId } : {}),
        startTutorial: 'pokedex',
        fromDeckTutorial: 'true'
      }
    });
  }

  private async continueInProfileHistory(): Promise<void> {
    const historyCard = await this.waitForElement(() => this.queryElement('[data-tour="profile-history-card"]'));
    if (!historyCard) {
      this.activeTour?.moveNext();
      return;
    }

    this.destroyActiveTour();
    const historyTour = driver({
      ...this.commonConfig(),
      steps: this.buildProfileHistorySteps(true)
    });
    this.activeTour = historyTour;
    historyTour.drive();
  }


  private homeDeckActionStep(): DriveStep {
    return {
      element: '[data-tour="choose-deck"]',
      disableActiveInteraction: false,
      onHighlighted: (_element, _step, options) => this.enableHomeDeckChoiceInteraction(options.driver),
      onDeselected: () => this.clearListeners(),
      popover: {
        description: this.buildOakDialog(
          this.t('OAK_TUTORIAL.STEPS.CHOOSE_DECK.TITLE'),
          this.t('OAK_TUTORIAL.STEPS.CHOOSE_DECK.DESCRIPTION')
        ),
        side: 'top',
        align: 'center',
        showButtons: ['previous', 'close']
      }
    };
  }

  private step(
    targetOrConfig: TutorialTarget | TutorialStepConfig,
    titleKey?: string,
    descriptionKey?: string,
    overrides: Omit<TutorialStepConfig, 'target' | 'titleKey' | 'descriptionKey'> = {}
  ): DriveStep {
    const config: TutorialStepConfig =
      typeof targetOrConfig === 'object' && !Array.isArray(targetOrConfig) && 'target' in targetOrConfig
        ? targetOrConfig
        : {
            target: targetOrConfig as TutorialTarget,
            titleKey: titleKey ?? '',
            descriptionKey: descriptionKey ?? '',
            ...overrides
          };

    const onHighlightStarted: DriverHook = (_element, _step, options) => {
      if (config.shouldShow && !config.shouldShow()) {
        queueMicrotask(() => options.driver.moveNext());
      }
    };

    return {
      element: () => this.resolveTarget(config.target),
      disableActiveInteraction: config.disableActiveInteraction ?? true,
      onHighlightStarted,
      onHighlighted: (_element, _step, options) => config.onHighlight?.(options.driver),
      onDeselected: () => this.clearListeners(),
      popover: {
        description: this.buildOakDialog(this.t(config.titleKey), this.t(config.descriptionKey)),
        side: 'bottom',
        align: 'center',
        showButtons: config.showButtons,
        onNextClick: (_element, _step, options) => {
          if (config.onNextClick) {
            config.onNextClick(options.driver);
            return;
          }

          options.driver.moveNext();
        }
      }
    };
  }

  private resolveTarget(target: TutorialTarget): Element {
    if (typeof target === 'string') {
      return this.queryElement(target) ?? this.document.body;
    }

    return target() ?? this.document.body;
  }

  private enableHomeDeckChoiceInteraction(tour: Driver): void {
    this.clearTourClickTargets();

    const chooseDeckButton = this.queryElement('[data-tour="choose-deck"]');
    const navDeckButton = this.queryElement('[data-tour="nav-deck"]');
    const navbar = navDeckButton?.closest<HTMLElement>('[data-tour="navbar"]');

    if (!chooseDeckButton || !navDeckButton || !navbar) {
      return;
    }

    chooseDeckButton.classList.add('oak-secondary-target');
    navDeckButton.classList.add('oak-secondary-target');
    navbar.classList.add('oak-secondary-target-layer');

    this.createTourClickProxy('oak-nav-deck-proxy', navDeckButton, this.t('OAK_TUTORIAL.STEPS.DECK_NAV.TITLE'), () => {
      this.clearTourClickTargets();
      tour.destroy();
      void this.router.navigate(['/', APP_ROUTES.deck]);
    });

    this.createTourClickProxy(
      'oak-choose-deck-proxy',
      chooseDeckButton,
      this.t('OAK_TUTORIAL.STEPS.CHOOSE_DECK.TITLE'),
      () => {
        this.clearTourClickTargets();
        tour.destroy();
        chooseDeckButton.click();
      }
    );
  }

  private enableProxyInteraction(config: { target: string; labelKey: string; afterClick: () => void }): void {
    this.clearTourClickTargets();

    const target = this.queryElement(config.target);
    if (!target) {
      return;
    }

    target.classList.add('oak-secondary-target');
    this.createTourClickProxy(
      `oak-proxy-${config.labelKey.replace(/[^a-z0-9]/gi, '-').toLowerCase()}`,
      target,
      this.t(config.labelKey),
      () => {
        this.clearTourClickTargets();
        target.click();
        config.afterClick();
      }
    );
  }

  private enableTargetInteraction(config: { target: string; afterClick: (target: HTMLElement) => void }): void {
    this.clearTourClickTargets();

    const target = this.queryElement(config.target);
    if (!target) {
      return;
    }

    target.classList.add('oak-secondary-target');
    const handleClick = (): void => {
      queueMicrotask(() => config.afterClick(target));
    };

    target.addEventListener('click', handleClick, { once: true });
    this.listenerCleanups.push(() => target.removeEventListener('click', handleClick));
  }

  private enableLayoutSafeHighlight(selector: string): void {
    const target = this.queryElement(selector);
    if (!target) {
      return;
    }

    target.classList.add('oak-layout-safe-highlight');
    this.listenerCleanups.push(() => target.classList.remove('oak-layout-safe-highlight'));
  }

  private createTourClickProxy(id: string, target: HTMLElement, label: string, onClick: () => void): void {
    const targetRect = target.getBoundingClientRect();
    const proxy = this.document.createElement('button');
    proxy.id = id;
    proxy.type = 'button';
    proxy.className = 'oak-tour-click-proxy';
    proxy.setAttribute('aria-label', label);
    proxy.style.left = `${targetRect.left}px`;
    proxy.style.top = `${targetRect.top}px`;
    proxy.style.width = `${targetRect.width}px`;
    proxy.style.height = `${targetRect.height}px`;
    proxy.addEventListener(
      'click',
      (event: MouseEvent) => {
        event.preventDefault();
        event.stopPropagation();
        onClick();
      },
      { once: true }
    );
    this.document.body.appendChild(proxy);
  }

  private waitForElement(
    resolver: () => Element | null,
    timeoutMs = 4000,
    intervalMs = 80
  ): Promise<Element | null> {
    return new Promise((resolve) => {
      const initial = resolver();
      if (initial) {
        resolve(initial);
        return;
      }

      const intervalId = window.setInterval(() => {
        const found = resolver();
        if (found) {
          window.clearInterval(intervalId);
          window.clearTimeout(timeoutId);
          resolve(found);
        }
      }, intervalMs);

      const timeoutId = window.setTimeout(() => {
        window.clearInterval(intervalId);
        resolve(null);
      }, timeoutMs);

      this.listenerCleanups.push(() => {
        window.clearInterval(intervalId);
        window.clearTimeout(timeoutId);
      });
    });
  }

  private resolveDeckTutorialMode(): DeckTutorialMode | null {
    const mode = this.document.querySelector<HTMLElement>('[data-tour-mode]')?.getAttribute('data-tour-mode');

    switch (mode) {
      case 'empty':
      case 'create':
      case 'with-decks':
      case 'edit':
        return mode;
      default:
        return null;
    }
  }

  private deckActivateDescriptionKey(): string {
    const button = this.queryElement('[data-tour="deck-activate-button"]');
    return button?.hasAttribute('disabled')
      ? 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVATE_ACTIVE.DESCRIPTION'
      : 'OAK_TUTORIAL.DECK_GUIDE.EXISTING.ACTIVATE.DESCRIPTION';
  }

  private buildOakDialog(title: string, description: string): string {
    const professorImage = this.getRandomProfessorImage();
    const professorAlt = this.escapeHtml(this.t('OAK_TUTORIAL.ACCESSIBILITY.PROFESSOR_ALT'));

    return `
      <div class="oak-professor-fixed">
        <div class="oak-professor-frame">
          <span class="oak-professor-fallback" aria-hidden="true">OAK</span>
          <img src="${professorImage}" class="oak-professor-image" alt="${professorAlt}">
        </div>
      </div>
      <div class="oak-dialog-position" data-oak-dialog>
        <div class="oak-dialog-card">
          <div class="oak-dialog-content">
            <strong class="oak-dialog-title">${this.escapeHtml(title)}</strong>
            <p class="oak-dialog-text">${this.escapeHtml(description)}</p>
          </div>
        </div>
      </div>
    `;
  }

  private decoratePopover(popover: PopoverDOM, tour: Driver): void {
    const dialogPosition = popover.description.querySelector<HTMLElement>('[data-oak-dialog]');
    const dialogCard = popover.description.querySelector<HTMLElement>('.oak-dialog-card');
    if (!dialogPosition || !dialogCard) {
      return;
    }

    dialogCard.append(popover.footer);
    dialogCard.append(popover.closeButton);
    popover.closeButton.setAttribute('aria-label', this.t('OAK_TUTORIAL.BUTTONS.CLOSE'));
    const activeElement = tour.getActiveElement() as HTMLElement | undefined;
    this.setupDialogPositioning(popover.wrapper, dialogPosition, dialogCard, activeElement);
    this.hideUnavailableImages(popover.wrapper);
  }

  private setupDialogPositioning(
    popover: HTMLElement,
    dialogPosition: HTMLElement,
    dialogCard: HTMLElement,
    activeElement?: HTMLElement
  ): void {
    let animationFrameId: number | null = null;
    const updatePosition = (): void => {
      animationFrameId = null;
      this.positionDialog(dialogPosition, dialogCard, activeElement);
    };
    const scheduleUpdate = (): void => {
      if (animationFrameId !== null) {
        window.cancelAnimationFrame(animationFrameId);
      }
      animationFrameId = window.requestAnimationFrame(updatePosition);
    };

    if (activeElement) {
      const targetRect = activeElement.getBoundingClientRect();
      const isOutsideViewport =
        targetRect.top < 0 ||
        targetRect.left < 0 ||
        targetRect.bottom > window.innerHeight ||
        targetRect.right > window.innerWidth;
      if (isOutsideViewport) {
        activeElement.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
      }
    }

    window.addEventListener('resize', scheduleUpdate);
    window.addEventListener('orientationchange', scheduleUpdate);
    window.addEventListener('scroll', scheduleUpdate, true);
    this.listenerCleanups.push(() => {
      window.removeEventListener('resize', scheduleUpdate);
      window.removeEventListener('orientationchange', scheduleUpdate);
      window.removeEventListener('scroll', scheduleUpdate, true);
      if (animationFrameId !== null) {
        window.cancelAnimationFrame(animationFrameId);
      }
    });

    if (typeof ResizeObserver !== 'undefined') {
      const resizeObserver = new ResizeObserver(scheduleUpdate);
      resizeObserver.observe(dialogCard);
      if (activeElement) {
        resizeObserver.observe(activeElement);
      }
      this.listenerCleanups.push(() => resizeObserver.disconnect());
    }

    popover.querySelectorAll('img').forEach((image) => {
      image.addEventListener('load', scheduleUpdate);
      this.listenerCleanups.push(() => image.removeEventListener('load', scheduleUpdate));
    });

    this.positionDialog(dialogPosition, dialogCard, activeElement);
    scheduleUpdate();
  }

  private positionDialog(dialogPosition: HTMLElement, dialogCard: HTMLElement, activeElement?: HTMLElement): void {
    if (!activeElement) {
      return;
    }

    const isMobile = this.isMobileTutorial();
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    const targetRect = activeElement.getBoundingClientRect();
    const cardRect = dialogCard.getBoundingClientRect();
    const professorRect = this.getProfessorRect();
    const viewportPadding = isMobile ? 8 : 16;
    const headerSafeTop = isMobile ? 72 : 0;
    const cardWidth = Math.min(cardRect.width || 620, viewportWidth - viewportPadding * 2);
    const cardHeight = Math.min(
      cardRect.height || 260,
      viewportHeight - viewportPadding * 2 - headerSafeTop
    );
    const safeLeft = viewportPadding;
    const safeRight = viewportPadding;
    const safeTop = viewportPadding + headerSafeTop;
    const safeBottom = viewportPadding;
    const margin = isMobile ? MOBILE_TARGET_GAP : 16;
    const targetCenterX = targetRect.left + targetRect.width / 2;
    const targetCenterY = targetRect.top + targetRect.height / 2;

    for (const direction of this.preferredDirections(
      targetCenterX,
      targetCenterY,
      viewportWidth,
      viewportHeight,
      isMobile
    )) {
      const candidate = this.positionForDirection(
        direction,
        cardWidth,
        cardHeight,
        safeLeft,
        safeRight,
        safeTop,
        safeBottom,
        margin,
        targetRect,
        viewportWidth,
        viewportHeight
      );

      if (
        candidate &&
        !this.rectanglesOverlap(candidate.left, candidate.top, cardWidth, cardHeight, targetRect, margin) &&
        !this.candidateOverlapsProfessor(candidate.left, candidate.top, cardWidth, cardHeight, professorRect, margin)
      ) {
        this.applyDialogPosition(dialogPosition, candidate.left, candidate.top);
        return;
      }
    }

    const cornerCandidates = [
      { left: safeLeft, top: safeTop },
      { left: viewportWidth - cardWidth - safeRight, top: safeTop },
      { left: safeLeft, top: viewportHeight - cardHeight - safeBottom },
      { left: viewportWidth - cardWidth - safeRight, top: viewportHeight - cardHeight - safeBottom }
    ];
    const safeCorner = cornerCandidates.find(
      (candidate) =>
        !this.rectanglesOverlap(candidate.left, candidate.top, cardWidth, cardHeight, targetRect, margin) &&
        !this.candidateOverlapsProfessor(candidate.left, candidate.top, cardWidth, cardHeight, professorRect, margin)
    );
    const fallback = safeCorner ?? cornerCandidates.reduce((best, candidate) =>
      this.combinedOverlapArea(candidate, cardWidth, cardHeight, targetRect, professorRect) <
      this.combinedOverlapArea(best, cardWidth, cardHeight, targetRect, professorRect)
        ? candidate
        : best
    );
    this.applyDialogPosition(dialogPosition, fallback.left, fallback.top);
  }

  private applyDialogPosition(dialogPosition: HTMLElement, left: number, top: number): void {
    dialogPosition.style.left = `${left}px`;
    dialogPosition.style.top = `${top}px`;
    dialogPosition.style.right = 'auto';
    dialogPosition.style.bottom = 'auto';
    dialogPosition.style.transform = 'none';
  }

  private dialogOverlapArea(
    candidate: { left: number; top: number },
    width: number,
    height: number,
    targetRect: DOMRect
  ): number {
    const overlapWidth = Math.max(
      0,
      Math.min(candidate.left + width, targetRect.right) - Math.max(candidate.left, targetRect.left)
    );
    const overlapHeight = Math.max(
      0,
      Math.min(candidate.top + height, targetRect.bottom) - Math.max(candidate.top, targetRect.top)
    );
    return overlapWidth * overlapHeight;
  }

  private preferredDirections(
    centerX: number,
    centerY: number,
    viewportWidth: number,
    viewportHeight: number,
    isMobile: boolean
  ): DialogDirection[] {
    if (isMobile) {
      // Mobile landscape: try the sides first (most free vertical room is scarce),
      // then top/bottom, then a free screen corner, before falling back to overlap minimization.
      const sideFirst: DialogDirection[] = centerX < viewportWidth * 0.5 ? ['right', 'left'] : ['left', 'right'];
      const verticalFirst: DialogDirection[] = centerY > viewportHeight * 0.5 ? ['top', 'bottom'] : ['bottom', 'top'];
      return [
        ...sideFirst,
        ...verticalFirst,
        'bottom-right',
        'bottom-left',
        'top-right',
        'top-left',
        'center'
      ];
    }

    if (centerY > viewportHeight * 0.62) {
      return ['top', centerX < viewportWidth * 0.45 ? 'right' : 'left', 'bottom', 'center'];
    }

    if (centerY < viewportHeight * 0.38) {
      return ['bottom', centerX < viewportWidth * 0.45 ? 'right' : 'left', 'top', 'center'];
    }

    if (centerX < viewportWidth * 0.5) {
      return ['right', 'top', 'bottom', 'left', 'center'];
    }

    return ['left', 'top', 'bottom', 'right', 'center'];
  }

  private positionForDirection(
    direction: DialogDirection,
    cardWidth: number,
    cardHeight: number,
    safeLeft: number,
    safeRight: number,
    safeTop: number,
    safeBottom: number,
    margin: number,
    targetRect: DOMRect,
    viewportWidth: number,
    viewportHeight: number
  ): { left: number; top: number } | null {
    const targetCenterX = targetRect.left + targetRect.width / 2;
    const targetCenterY = targetRect.top + targetRect.height / 2;

    switch (direction) {
      case 'bottom':
        return {
          left: this.clamp(targetCenterX - cardWidth / 2, safeLeft, viewportWidth - cardWidth - safeRight),
          top: this.clamp(targetRect.bottom + margin, safeTop, viewportHeight - cardHeight - safeBottom)
        };
      case 'top':
        return {
          left: this.clamp(targetCenterX - cardWidth / 2, safeLeft, viewportWidth - cardWidth - safeRight),
          top: this.clamp(targetRect.top - cardHeight - margin, safeTop, viewportHeight - cardHeight - safeBottom)
        };
      case 'right':
        return {
          left: this.clamp(targetRect.right + margin, safeLeft, viewportWidth - cardWidth - safeRight),
          top: this.clamp(targetCenterY - cardHeight / 2, safeTop, viewportHeight - cardHeight - safeBottom)
        };
      case 'left':
        return {
          left: this.clamp(targetRect.left - cardWidth - margin, safeLeft, viewportWidth - cardWidth - safeRight),
          top: this.clamp(targetCenterY - cardHeight / 2, safeTop, viewportHeight - cardHeight - safeBottom)
        };
      case 'center':
        return {
          left: this.clamp((viewportWidth - cardWidth) / 2, safeLeft, viewportWidth - cardWidth - safeRight),
          top: this.clamp((viewportHeight - cardHeight) / 2, safeTop, viewportHeight - cardHeight - safeBottom)
        };
      case 'top-left':
        return { left: safeLeft, top: safeTop };
      case 'top-right':
        return { left: viewportWidth - cardWidth - safeRight, top: safeTop };
      case 'bottom-left':
        return { left: safeLeft, top: viewportHeight - cardHeight - safeBottom };
      case 'bottom-right':
        return { left: viewportWidth - cardWidth - safeRight, top: viewportHeight - cardHeight - safeBottom };
    }
  }

  /**
   * Mobile-only flag used both to switch the tutorial to its compact layout/positioning
   * and to decide which set of steps to build (mobile vs desktop) for Deck and Pokedex.
   * Desktop always evaluates to false, so existing desktop behaviour is untouched.
   */
  private isMobileTutorial(): boolean {
    if (!isPlatformBrowser(this.platformId) || typeof window === 'undefined' || !window.matchMedia) {
      return false;
    }

    return window.matchMedia(MOBILE_TUTORIAL_MEDIA_QUERY).matches;
  }

  /**
   * Checks that an element is actually visible (not display:none/visibility:hidden and has a
   * non-zero box). Used to avoid pointing the tutorial at desktop-only targets that are still
   * present in the DOM but hidden/transformed off-screen on mobile (e.g. the deck side panel).
   */
  private isElementVisible(element: HTMLElement): boolean {
    const rect = element.getBoundingClientRect();
    const style = window.getComputedStyle(element);

    return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden';
  }

  /**
   * Like queryElement, but only returns the element if it is actually visible. Use this for
   * targets that exist in the DOM on both layouts but are only meaningfully visible on one.
   */
  private queryVisibleElement(selector: string): HTMLElement | null {
    const element = this.queryElement(selector);
    return element && this.isElementVisible(element) ? element : null;
  }

  /**
   * Mobile Deck builder only: the selected-cards panel ("desplegable") is always present in the
   * DOM and only slides off-screen via a transform, so a plain visibility check would report it
   * as visible even while closed. The component exposes whether it's open via data-panel-open.
   */
  private isMobileDeckPanelOpen(): boolean {
    return this.queryElement('[data-tour="deck-selected-list"]')?.getAttribute('data-panel-open') === 'true';
  }

  private rectanglesOverlap(
    left: number,
    top: number,
    width: number,
    height: number,
    targetRect: DOMRect,
    margin: number
  ): boolean {
    const right = left + width;
    const bottom = top + height;

    return !(
      right < targetRect.left - margin ||
      left > targetRect.right + margin ||
      bottom < targetRect.top - margin ||
      top > targetRect.bottom + margin
    );
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }

  private rectsOverlap(
    a: { left: number; top: number; right: number; bottom: number },
    b: { left: number; top: number; right: number; bottom: number },
    gap = 16
  ): boolean {
    return !(
      a.right + gap < b.left ||
      a.left - gap > b.right ||
      a.bottom + gap < b.top ||
      a.top - gap > b.bottom
    );
  }

  private candidateOverlapsProfessor(
    left: number,
    top: number,
    width: number,
    height: number,
    professorRect: { left: number; top: number; right: number; bottom: number } | null,
    gap: number
  ): boolean {
    if (!professorRect) {
      return false;
    }
    return this.rectsOverlap(
      { left, top, right: left + width, bottom: top + height },
      professorRect,
      gap
    );
  }

  private getProfessorRect(): { left: number; top: number; right: number; bottom: number } | null {
    const professorEl = this.document.querySelector<HTMLElement>('.oak-professor-fixed');
    if (!professorEl) {
      return null;
    }
    const style = window.getComputedStyle(professorEl);
    if (style.display === 'none' || style.visibility === 'hidden') {
      return null;
    }
    const rect = professorEl.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) {
      return null;
    }
    return { left: rect.left, top: rect.top, right: rect.right, bottom: rect.bottom };
  }

  private combinedOverlapArea(
    candidate: { left: number; top: number },
    width: number,
    height: number,
    targetRect: DOMRect,
    professorRect: { left: number; top: number; right: number; bottom: number } | null
  ): number {
    let total = this.dialogOverlapArea(candidate, width, height, targetRect);
    if (professorRect) {
      const overlapWidth = Math.max(
        0,
        Math.min(candidate.left + width, professorRect.right) - Math.max(candidate.left, professorRect.left)
      );
      const overlapHeight = Math.max(
        0,
        Math.min(candidate.top + height, professorRect.bottom) - Math.max(candidate.top, professorRect.top)
      );
      total += overlapWidth * overlapHeight;
    }
    return total;
  }

  private hideUnavailableImages(popover: HTMLElement): void {
    popover.querySelectorAll<HTMLImageElement>('img').forEach((image) => {
      const hideImage = (): void => {
        image.hidden = true;
      };

      if (image.complete && image.naturalWidth === 0) {
        hideImage();
        return;
      }

      if (image.complete) {
        return;
      }

      image.addEventListener('error', hideImage, { once: true });
      this.listenerCleanups.push(() => image.removeEventListener('error', hideImage));
    });
  }

  private getRandomProfessorImage(): string {
    const availableImages = this.professorImages.filter((image) => image !== this.lastProfessorImage);
    const selectedImage =
      availableImages[Math.floor(Math.random() * availableImages.length)] ?? this.professorImages[0];

    this.lastProfessorImage = selectedImage;
    return selectedImage;
  }

  private destroyActiveTour(): void {
    this.clearListeners();
    this.activeTour?.destroy();
    this.activeTour = null;
  }

  private finishTour(): void {
    this.clearListeners();
    this.activeTour = null;
  }

  private clearListeners(): void {
    this.listenerCleanups.forEach((cleanup) => cleanup());
    this.listenerCleanups = [];
    this.clearTourClickTargets();
  }

  private clearTourClickTargets(): void {
    this.document.querySelectorAll('.oak-tour-click-proxy').forEach((element) => element.remove());
    this.document.querySelectorAll('.oak-secondary-target').forEach((element) => {
      element.classList.remove('oak-secondary-target');
    });
    this.document.querySelectorAll('.oak-secondary-target-layer').forEach((element) => {
      element.classList.remove('oak-secondary-target-layer');
    });
    this.document.querySelectorAll('.oak-layout-safe-highlight').forEach((element) => {
      element.classList.remove('oak-layout-safe-highlight');
    });
  }

  private queryElement(selector: string): HTMLElement | null {
    return this.document.querySelector<HTMLElement>(selector);
  }

  private isDeckRoute(): boolean {
    return this.router.url.startsWith(`/${APP_ROUTES.deck}`);
  }

  private isHomeRoute(): boolean {
    return this.router.url.startsWith(`/${APP_ROUTES.home}`);
  }

  private isPokedexRoute(): boolean {
    return this.router.url.startsWith(`/${APP_ROUTES.pokedex}`);
  }

  private isProfileRoute(): boolean {
    return this.router.url.startsWith(`/${APP_ROUTES.profile}`);
  }

  private t(key: string): string {
    return this.languageService.t(key);
  }

  private escapeHtml(text: string): string {
    return text.replace(
      /[&<>'"]/g,
      (character) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character] ??
        character
    );
  }
}
