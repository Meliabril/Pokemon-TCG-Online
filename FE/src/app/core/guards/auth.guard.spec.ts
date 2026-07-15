import { TestBed } from '@angular/core/testing';
import { CanActivateFn, provideRouter, Router, UrlTree } from '@angular/router';
import { UserRole } from '../models/enums/user/user-role.enum';
import { UserStatus } from '../models/enums/user/user-status.enum';
import { StorageService } from '../storage/storage.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideRouter([])]
    });
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('allows access when a session exists', () => {
    const storageService = TestBed.inject(StorageService);

    storageService.saveSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 'user-id',
        email: 'ash@kanto.dev',
        username: 'ash',
        role: UserRole.User,
        status: UserStatus.Active,
        emailVerified: true,
        createdAt: '2026-05-24T00:00:00Z',
        updatedAt: '2026-05-24T00:00:00Z'
      }
    });

    expect(executeGuard({} as never, { url: '/play/config' } as never)).toBeTrue();
  });

  it('redirects to login when there is no session', () => {
    const router = TestBed.inject(Router);
    const storageService = TestBed.inject(StorageService);
    storageService.clearSession();
    const result = executeGuard({} as never, { url: '/play/config' } as never);

    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/auth/login?returnUrl=%2Fplay%2Fconfig');
  });
});
