import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { UserRole } from '../../../core/models/enums/user/user-role.enum';
import { UserStatus } from '../../../core/models/enums/user/user-status.enum';
import { StorageService } from '../../../core/storage/storage.service';
import { AuthApiService } from '../../../infrastructure/api/auth/auth-api.service';
import { AuthMediaService } from '../../../features/auth/data-access/auth-media.service';
import { MainLayoutComponent } from './main-layout.component';

describe('MainLayoutComponent', () => {
  it('renders the volume slider without a secondary indicator icon', () => {
    TestBed.configureTestingModule({
      imports: [MainLayoutComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthMediaService,
          useValue: {
            enableAudio: jasmine.createSpy('enableAudio'),
            playIfEnabled: jasmine.createSpy('playIfEnabled'),
            playBattleTrack: jasmine.createSpy('playBattleTrack'),
            stopBattleTrack: jasmine.createSpy('stopBattleTrack'),
            markAudioEnabled: jasmine.createSpy('markAudioEnabled'),
            setVolume: jasmine.createSpy('setVolume'),
            volume: () => 0.6
          }
        },
        {
          provide: AuthApiService,
          useValue: {
            logout: () => of({ message: 'ok' })
          }
        },
        {
          provide: StorageService,
          useValue: {
            clearSession: jasmine.createSpy('clearSession'),
            isAuthenticated: signal(true).asReadonly(),
            currentUser: signal({
              id: 'user-1',
              email: 'misty@cerulean.dev',
              username: 'misty',
              role: UserRole.User,
              status: UserStatus.Active,
              emailVerified: true,
              createdAt: '2026-05-27T00:00:00Z',
              updatedAt: '2026-05-27T00:00:00Z'
            }).asReadonly()
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.volume-control__slider')).not.toBeNull();
    expect(host.querySelector('[data-testid="volume-indicator-icon"]')).toBeNull();
  });
});
