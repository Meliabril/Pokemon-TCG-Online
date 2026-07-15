import { TestBed } from '@angular/core/testing';
import { LEGACY_AUTH_STORAGE_KEYS } from '../constants/storage/storage.constants';
import { UserRole } from '../models/enums/user/user-role.enum';
import { UserStatus } from '../models/enums/user/user-status.enum';
import { AuthSession } from '../models/interfaces/auth/auth-session.interface';
import { StorageService } from './storage.service';

describe('StorageService', () => {
  const session: AuthSession = {
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
  };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();

    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('stores and recovers the current session in memory only', () => {
    const service = TestBed.inject(StorageService);

    service.saveSession(session);

    expect(service.getSession()).toEqual(jasmine.objectContaining(session));
    expect(service.getSession()?.accessTokenExpiresAt).toEqual(jasmine.any(Number));
    expect(service.getAccessToken()).toBe('access-token');
    expect(service.authStatus()).toBe('authenticated');

    for (const key of LEGACY_AUTH_STORAGE_KEYS) {
      expect(localStorage.getItem(key)).toBeNull();
      expect(sessionStorage.getItem(key)).toBeNull();
    }
  });

  it('does not survive a fresh service instance (no persistence)', () => {
    const service = TestBed.inject(StorageService);
    service.saveSession(session);

    TestBed.resetTestingModule();
    const freshService = TestBed.inject(StorageService);

    expect(freshService.getSession()).toBeNull();
  });

  it('clears the in-memory session', () => {
    const service = TestBed.inject(StorageService);

    service.saveSession(session);
    service.clearSession();

    expect(service.getSession()).toBeNull();
    expect(service.getAccessToken()).toBeNull();
    expect(service.authStatus()).toBe('unauthenticated');
  });

  it('keeps the session in memory regardless of the remember me flag', () => {
    const service = TestBed.inject(StorageService);

    service.saveSession(session, false);

    expect(service.getSession()).toEqual(jasmine.objectContaining(session));
    expect(service.getSession()?.accessTokenExpiresAt).toEqual(jasmine.any(Number));

    for (const key of LEGACY_AUTH_STORAGE_KEYS) {
      expect(localStorage.getItem(key)).toBeNull();
      expect(sessionStorage.getItem(key)).toBeNull();
    }
  });

  it('purges any legacy session/token left over by an older app version', () => {
    for (const key of LEGACY_AUTH_STORAGE_KEYS) {
      localStorage.setItem(key, 'leftover-value');
      sessionStorage.setItem(key, 'leftover-value');
    }

    const service = TestBed.inject(StorageService);

    for (const key of LEGACY_AUTH_STORAGE_KEYS) {
      expect(localStorage.getItem(key)).toBeNull();
      expect(sessionStorage.getItem(key)).toBeNull();
    }

    expect(service.getSession()).toBeNull();
  });
});
