import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { UserRole } from '../../../../core/models/enums/user/user-role.enum';
import { UserStatus } from '../../../../core/models/enums/user/user-status.enum';
import { MatchFoundMessage } from '../../../../core/models/interfaces/matchmaking/matchmaking-realtime.interface';
import { QueueRoomState } from '../../../../core/models/interfaces/play/prematch.interface';
import { StorageService } from '../../../../core/storage/storage.service';
import { DeckApiService } from '../../../../infrastructure/api/deck/deck-api.service';
import { MatchmakingApiService } from '../../../../infrastructure/api/matchmaking/matchmaking-api.service';
import { PrematchFacadeService } from '../../services/prematch-facade.service';
import { MatchmakingRealtimeService } from '../../services/matchmaking-realtime.service';
import { PlayRoomPageComponent } from './play-room-page.component';

describe('PlayRoomPageComponent', () => {
  let fixture: ComponentFixture<PlayRoomPageComponent>;
  let router: Router;

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('redirects back home when the room is opened without an active queue', async () => {
    const prematchFacade = {
      roomState: signal({
        status: 'idle',
        activeDeck: null,
        queueStatus: null,
        errorMessage: '',
        infoMessage: '',
        matchFound: false,
        gameId: null,
        opponentUserId: null,
        matchedAt: null
      }),
      refreshRoomState: jasmine.createSpy('refreshRoomState').and.resolveTo(false),
      showMatchedRoom: jasmine.createSpy('showMatchedRoom'),
      leaveQueue: jasmine.createSpy('leaveQueue').and.resolveTo(true)
    };

    TestBed.configureTestingModule({
      imports: [PlayRoomPageComponent],
      providers: [
        provideRouter([]),
        { provide: PrematchFacadeService, useValue: prematchFacade },
        { provide: MatchmakingRealtimeService, useValue: { watchMatchmaking: () => of() } }
      ]
    });

    fixture = TestBed.createComponent(PlayRoomPageComponent);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledOnceWith(
      `/${APP_ROUTES.home}`
    );
  });

  it('navigates to the game route when the refreshed room state includes a gameId', async () => {
    const roomState = signal<QueueRoomState>({
      status: 'queued' as const,
      activeDeck: null,
      queueStatus: null,
      errorMessage: '',
      infoMessage: '',
      matchFound: false,
      gameId: null,
      opponentUserId: null,
      matchedAt: null
    });
    const prematchFacade = {
      roomState,
      refreshRoomState: jasmine.createSpy('refreshRoomState').and.callFake(async () => {
        roomState.set({
          status: 'queued',
          activeDeck: null,
          queueStatus: {
            queued: false,
            queuedAt: '2026-05-27T10:00:00Z',
            queueSize: 0,
            matchedUserId: 'user-2',
            gameId: 'game-77'
          },
          errorMessage: '',
          infoMessage: '',
          matchFound: true,
          gameId: 'game-77',
          opponentUserId: 'user-2',
          matchedAt: null
        });
        return true;
      }),
      showMatchedRoom: jasmine.createSpy('showMatchedRoom'),
      leaveQueue: jasmine.createSpy('leaveQueue').and.resolveTo(true)
    };

    TestBed.configureTestingModule({
      imports: [PlayRoomPageComponent],
      providers: [
        provideRouter([]),
        { provide: PrematchFacadeService, useValue: prematchFacade },
        { provide: MatchmakingRealtimeService, useValue: { watchMatchmaking: () => of() } }
      ]
    });

    fixture = TestBed.createComponent(PlayRoomPageComponent);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    spyOn(window, 'setInterval').and.returnValue(1);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.navigate).toHaveBeenCalledOnceWith(['/', APP_ROUTES.game, 'game-77']);
  });

  it('keeps the waiting room active when navigation state has matchedUserId without gameId', async () => {
    const prematchFacade = {
      roomState: signal({
        status: 'queued' as const,
        activeDeck: null,
        queueStatus: {
          queued: false,
          queuedAt: null,
          queueSize: null,
          matchedUserId: 'user-2',
          gameId: null
        },
        errorMessage: '',
        infoMessage: '',
        matchFound: true,
        gameId: null,
        opponentUserId: 'user-2',
        matchedAt: null
      }),
      refreshRoomState: jasmine.createSpy('refreshRoomState').and.resolveTo(true),
      showMatchedRoom: jasmine.createSpy('showMatchedRoom').and.resolveTo(undefined),
      leaveQueue: jasmine.createSpy('leaveQueue').and.resolveTo(true)
    };

    TestBed.configureTestingModule({
      imports: [PlayRoomPageComponent],
      providers: [
        provideRouter([]),
        { provide: PrematchFacadeService, useValue: prematchFacade },
        { provide: MatchmakingRealtimeService, useValue: { watchMatchmaking: () => of() } }
      ]
    });

    router = TestBed.inject(Router);
    spyOn(router, 'getCurrentNavigation').and.returnValue({
      extras: { state: { matchedUserId: 'user-2' } }
    } as never);
    spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const setIntervalSpy = spyOn(window, 'setInterval').and.returnValue(1);

    fixture = TestBed.createComponent(PlayRoomPageComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(prematchFacade.showMatchedRoom).toHaveBeenCalledOnceWith('user-2', null);
    expect(setIntervalSpy).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalledWith(['/', APP_ROUTES.game, jasmine.any(String)]);
    expect(fixture.nativeElement.textContent).toContain('PLAY.FOUND_DESCRIPTION');
  });

  it('does not redirect player 1 during a transient empty handoff poll', async () => {
    const deckApi = jasmine.createSpyObj<DeckApiService>('DeckApiService', ['getActiveDeck']);
    const matchmakingApi = jasmine.createSpyObj<MatchmakingApiService>('MatchmakingApiService', [
      'getMyQueueStatus',
      'leaveQueue'
    ]);
    const isAuthenticated = signal(true);
    const currentUser = signal({
      id: 'user-1',
      email: 'ash@kanto.dev',
      username: 'ash',
      role: UserRole.User,
      status: UserStatus.Active,
      emailVerified: true,
      createdAt: '2026-05-27T00:00:00Z',
      updatedAt: '2026-05-27T00:00:00Z'
    });

    deckApi.getActiveDeck.and.returnValues(of(deck()), of(deck()));
    matchmakingApi.getMyQueueStatus.and.returnValue(
      of({ queued: false, queuedAt: null, queueSize: null, matchedUserId: null, gameId: null })
    );
    matchmakingApi.leaveQueue.and.returnValue(of(void 0));

    TestBed.configureTestingModule({
      imports: [PlayRoomPageComponent],
      providers: [
        provideRouter([]),
        PrematchFacadeService,
        { provide: DeckApiService, useValue: deckApi },
        { provide: MatchmakingApiService, useValue: matchmakingApi },
        {
          provide: StorageService,
          useValue: {
            isAuthenticated: isAuthenticated.asReadonly(),
            currentUser: currentUser.asReadonly()
          }
        },
        { provide: MatchmakingRealtimeService, useValue: { watchMatchmaking: () => of() } }
      ]
    });

    const prematchFacade = TestBed.inject(PrematchFacadeService);
    await prematchFacade.showMatchedRoom('user-2');

    router = TestBed.inject(Router);
    const navigateByUrlSpy = spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
    spyOn(window, 'setInterval').and.returnValue(1);

    fixture = TestBed.createComponent(PlayRoomPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(navigateByUrlSpy).not.toHaveBeenCalledWith(
      `/${APP_ROUTES.home}`
    );
    expect(prematchFacade.roomState().matchFound).toBeTrue();
  });

  it('navigates to the game route when realtime match-found payload arrives', async () => {
    const matchmakingMessages$ = new Subject<MatchFoundMessage>();
    const prematchFacade = {
      roomState: signal({
        status: 'queued' as const,
        activeDeck: null,
        queueStatus: {
          queued: true,
          queuedAt: '2026-05-27T10:00:00Z',
          queueSize: 1,
          matchedUserId: null,
          gameId: null
        },
        errorMessage: '',
        infoMessage: '',
        matchFound: false,
        gameId: null,
        opponentUserId: null,
        matchedAt: null
      }),
      refreshRoomState: jasmine.createSpy('refreshRoomState').and.resolveTo(true),
      showMatchedRoom: jasmine.createSpy('showMatchedRoom'),
      leaveQueue: jasmine.createSpy('leaveQueue').and.resolveTo(true)
    };
    const matchmakingRealtime = {
      watchMatchmaking: jasmine
        .createSpy('watchMatchmaking')
        .and.returnValue(matchmakingMessages$.asObservable())
    };

    TestBed.configureTestingModule({
      imports: [PlayRoomPageComponent],
      providers: [
        provideRouter([]),
        { provide: PrematchFacadeService, useValue: prematchFacade },
        { provide: MatchmakingRealtimeService, useValue: matchmakingRealtime }
      ]
    });

    fixture = TestBed.createComponent(PlayRoomPageComponent);
    router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    spyOn(window, 'setInterval').and.returnValue(1);
    spyOn(window, 'setTimeout').and.callFake((handler: TimerHandler) => {
      if (typeof handler === 'function') {
        handler();
      }
      return 1;
    });

    fixture.detectChanges();
    await Promise.resolve();
    await Promise.resolve();

    matchmakingMessages$.next({
      opponentUserId: 'user-2',
      gameId: 'game-99',
      matchedAt: '2026-05-27T10:00:05Z'
    });
    await Promise.resolve();
    await Promise.resolve();

    // The component enters the match-found transition and starts a 4s setTimeout
    // before navigating; assert the transition was armed instead of waiting on
    // real wall-clock time.
    expect(fixture.componentInstance['isMatchTransitioning']()).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalled();
  });

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
