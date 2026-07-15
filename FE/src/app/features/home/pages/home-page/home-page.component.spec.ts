import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { STORAGE_KEYS } from '../../../../core/constants/storage/storage.constants';
import { UserRole } from '../../../../core/models/enums/user/user-role.enum';
import { UserStatus } from '../../../../core/models/enums/user/user-status.enum';
import { StorageService } from '../../../../core/storage/storage.service';
import { PrematchFacadeService } from '../../../play/services/prematch-facade.service';
import { HomePageComponent } from './home-page.component';

describe('HomePageComponent', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('renders the internal main menu and refreshes prematch state', () => {
    const prematchFacade = {
      checklistState: signal({
        status: 'ready',
        availableDecks: [
          {
            id: 'deck-1',
            ownerUserId: 'user-1',
            name: 'Kanto Rush',
            format: 'XY1_UNLIMITED',
            active: true,
            valid: true,
            validationErrors: [],
            cards: [],
            createdAt: '2026-05-27T00:00:00Z',
            updatedAt: '2026-05-27T00:00:00Z'
          }
        ],
        activeDeck: {
          id: 'deck-1',
          ownerUserId: 'user-1',
          name: 'Kanto Rush',
          format: 'XY1_UNLIMITED',
          active: true,
          valid: true,
          validationErrors: [],
          cards: [],
          createdAt: '2026-05-27T00:00:00Z',
          updatedAt: '2026-05-27T00:00:00Z'
        },
        validation: { deckId: 'deck-1', valid: true, errors: [] },
        queueStatus: null,
        errorMessage: '',
        feedbackMessage: '',
        checks: { sessionReady: true, activeDeckFound: true, deckValid: true }
      }),
      refreshChecklist: jasmine.createSpy('refreshChecklist').and.resolveTo()
    };

    const fixture = createComponent(true, prematchFacade);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('HOME.MAIN_MENU');
    expect(text).toContain('HOME.PRIMARY_SEARCH');
    expect(text).toContain('HOME.CUSTOM_MATCH');
    expect(text).toContain('HOME.HOW_TO_PLAY');
    expect(text).toContain('HOME.MANUAL');
    expect(prematchFacade.refreshChecklist).toHaveBeenCalled();
  });

  it('sends the user back to the lobby when the queue is already active', async () => {
    const prematchFacade = {
      checklistState: signal({
        status: 'queued' as const,
        availableDecks: [deck()],
        activeDeck: null,
        validation: null,
        queueStatus: {
          queued: true,
          queuedAt: '2026-05-27T00:00:00Z',
          queueSize: 1,
          matchedUserId: null,
          gameId: null
        },
        errorMessage: '',
        feedbackMessage: '',
        checks: { sessionReady: true, activeDeckFound: true, deckValid: true }
      }),
      refreshChecklist: jasmine.createSpy('refreshChecklist').and.resolveTo(),
      joinQueue: jasmine.createSpy('joinQueue')
    };

    const fixture = createComponent(true, prematchFacade);
    const router = TestBed.inject(Router);
    const navigateByUrlSpy = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    await fixture.componentInstance.primaryAction();

    expect(navigateByUrlSpy).toHaveBeenCalledOnceWith(`/${APP_ROUTES.playRoom}`);
  });

  it('lands in the lobby right after queue creation from home', async () => {
    const prematchFacade = {
      checklistState: signal({
        status: 'ready' as const,
        availableDecks: [deck()],
        activeDeck: null,
        validation: null,
        queueStatus: null,
        errorMessage: '',
        feedbackMessage: '',
        checks: { sessionReady: true, activeDeckFound: true, deckValid: true }
      }),
      refreshChecklist: jasmine.createSpy('refreshChecklist').and.resolveTo(),
      joinQueue: jasmine.createSpy('joinQueue').and.resolveTo({
        queued: true,
        queuedAt: '2026-05-27T00:00:00Z',
        queueSize: 1,
        matchedUserId: null,
        gameId: null
      })
    };

    const fixture = createComponent(true, prematchFacade);
    const router = TestBed.inject(Router);
    const navigateByUrlSpy = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    await fixture.componentInstance.primaryAction();

    expect(prematchFacade.joinQueue).toHaveBeenCalled();
    expect(navigateByUrlSpy).toHaveBeenCalledOnceWith(`/${APP_ROUTES.playRoom}`, {
      state: { matchedUserId: null, gameId: null }
    });
  });

  it('navigates straight to the game when matchmaking already returns gameId from home', async () => {
    const prematchFacade = {
      checklistState: signal({
        status: 'ready' as const,
        availableDecks: [deck()],
        activeDeck: null,
        validation: null,
        queueStatus: null,
        errorMessage: '',
        feedbackMessage: '',
        checks: { sessionReady: true, activeDeckFound: true, deckValid: true }
      }),
      refreshChecklist: jasmine.createSpy('refreshChecklist').and.resolveTo(),
      joinQueue: jasmine.createSpy('joinQueue').and.resolveTo({
        queued: false,
        queuedAt: null,
        queueSize: 0,
        matchedUserId: null,
        gameId: 'game-77'
      })
    };

    const fixture = createComponent(true, prematchFacade);
    const router = TestBed.inject(Router);
    const navigateByUrlSpy = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    await fixture.componentInstance.primaryAction();

    expect(prematchFacade.joinQueue).toHaveBeenCalled();
    expect(navigateByUrlSpy).toHaveBeenCalledOnceWith(`/${APP_ROUTES.playRoom}`, {
      state: { gameId: 'game-77' }
    });
  });

  it('renders the home panel with light theme styles when the stored theme is light', () => {
    localStorage.setItem(STORAGE_KEYS.appTheme, 'LIGHT');
    const prematchFacade = {
      checklistState: signal({
        status: 'ready' as const,
        availableDecks: [],
        activeDeck: null,
        validation: null,
        queueStatus: null,
        errorMessage: '',
        feedbackMessage: '',
        checks: { sessionReady: true, activeDeckFound: false, deckValid: false }
      }),
      refreshChecklist: jasmine.createSpy('refreshChecklist').and.resolveTo()
    };

    const fixture = createComponent(true, prematchFacade);
    const panel = fixture.nativeElement.querySelector('article') as HTMLElement | null;

    expect(panel?.className).toContain('bg-[#fff8e3]/78');
  });


  function createComponent(isAuthenticated: boolean, prematchFacade: object): ComponentFixture<HomePageComponent> {
    const authSignal = signal(isAuthenticated);
    const userSignal = signal(
      isAuthenticated
        ? {
            id: 'user-1',
            email: 'ash@kanto.dev',
            username: 'ash',
            role: UserRole.User,
            status: UserStatus.Active,
            emailVerified: true,
            createdAt: '2026-05-27T00:00:00Z',
            updatedAt: '2026-05-27T00:00:00Z'
          }
        : null
    );

    const prematchFacadeWithDefaults = {
      hasActiveGame: signal(false),
      ...prematchFacade
    };

    TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        provideRouter([]),
        {
          provide: StorageService,
          useValue: {
            isAuthenticated: authSignal.asReadonly(),
            currentUser: userSignal.asReadonly()
          }
        },
        { provide: PrematchFacadeService, useValue: prematchFacadeWithDefaults }
      ]
    });

    const fixture = TestBed.createComponent(HomePageComponent);
    fixture.detectChanges();
    return fixture;
  }

  function deck() {
    return {
      id: 'deck-1',
      ownerUserId: 'user-1',
      name: 'Kanto Rush',
      format: 'XY1_UNLIMITED',
      active: true,
      valid: true,
      validationErrors: [],
      cards: [],
      createdAt: '2026-05-27T00:00:00Z',
      updatedAt: '2026-05-27T00:00:00Z'
    };
  }
});
