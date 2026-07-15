import { computed, Injectable, signal } from '@angular/core';
import { LEGACY_AUTH_STORAGE_KEYS } from '../constants/storage/storage.constants';
import { AuthSession, AuthStatus } from '../models/interfaces/auth/auth-session.interface';
import { CurrentUser } from '../models/interfaces/user/current-user.interface';
import { UserProfile } from '../models/interfaces/user/update-user.interface';

/**
 * Holds the authenticated session (tokens + current user) strictly in memory,
 * for the lifetime of the current tab/app instance.
 *
 * SECURITY NOTE: access/refresh tokens and user session data are
 * intentionally never written to `localStorage`, `sessionStorage`, cookies
 * accessible to JS, or any other persistent client-side storage. Any of
 * those would be readable by an XSS payload, which would hand over full
 * account access. The refresh token now lives only in an HttpOnly cookie,
 * while the access token stays in memory and is rebuilt on app startup via
 * `/api/auth/refresh`. The `rememberMe` flag is kept for API compatibility
 * with the login form but does not change client-side persistence behavior.
 */
@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly sessionState = signal<AuthSession | null>(null);
  private readonly authStatusState = signal<AuthStatus>('checking');
  private authCheckDeferred = this.createAuthCheckDeferred();

  readonly session = this.sessionState.asReadonly();
  readonly authStatus = this.authStatusState.asReadonly();
  readonly currentUser = computed(() => this.sessionState()?.user ?? null);
  readonly isAuthenticated = computed(
    () => this.authStatusState() === 'authenticated' && Boolean(this.sessionState()?.accessToken)
  );

  constructor() {
    // One-time cleanup for users who still have a session/token persisted
    // by an older version of the app.
    this.purgeLegacyAuthStorage();
  }

  getSession(): AuthSession | null {
    return this.sessionState();
  }

  markChecking(): void {
    if (this.authStatusState() !== 'checking') {
      this.authCheckDeferred = this.createAuthCheckDeferred();
    }

    this.authStatusState.set('checking');
  }

  saveSession(session: AuthSession, _rememberMe = true, refreshExpiration = true): void {
    this.sessionState.set(this.normalizeSession(session, refreshExpiration));
    this.completeAuthCheck('authenticated');
  }

  updateAccessToken(tokens: Pick<AuthSession, 'accessToken' | 'tokenType' | 'expiresIn'>): AuthSession {
    const currentSession = this.sessionState();
    const nextSession: AuthSession = {
      accessToken: tokens.accessToken,
      tokenType: tokens.tokenType,
      expiresIn: tokens.expiresIn,
      user: currentSession?.user ?? null,
      ...(currentSession?.accessTokenExpiresAt !== undefined
        ? { accessTokenExpiresAt: currentSession.accessTokenExpiresAt }
        : {})
    };

    this.saveSession(nextSession);
    return this.sessionState() as AuthSession;
  }

  updateCurrentUser(user: CurrentUser): void {
    const currentSession = this.sessionState();

    if (!currentSession) {
      return;
    }

    this.saveSession({
      ...currentSession,
      user
    }, true, false);
  }

  updateCurrentUserProfile(profile: UserProfile): void {
    const currentSession = this.sessionState();

    if (!currentSession?.user) {
      return;
    }

    this.saveSession({
      ...currentSession,
      user: {
        ...currentSession.user,
        email: profile.email,
        username: profile.username,
        emailVerified: profile.emailVerified,
        ...(profile.avatar !== undefined ? { avatar: profile.avatar } : {})
      }
    }, true, false);
  }

  getAccessToken(): string | null {
    const session = this.sessionState();

    if (!session?.accessToken) {
      return null;
    }

    return session.accessToken;
  }

  isAccessTokenExpired(skewMs = 30_000): boolean {
    const expiresAt = this.sessionState()?.accessTokenExpiresAt;

    if (typeof expiresAt !== 'number') {
      return true;
    }

    return Date.now() + skewMs >= expiresAt;
  }

  clearSession(): void {
    this.sessionState.set(null);
    this.completeAuthCheck('unauthenticated');
    this.purgeLegacyAuthStorage();
  }

  awaitAuthCheck(): Promise<AuthStatus> {
    if (this.authStatusState() !== 'checking') {
      return Promise.resolve(this.authStatusState());
    }

    return this.authCheckDeferred.promise;
  }

  private purgeLegacyAuthStorage(): void {
    const storages = [
      typeof localStorage === 'undefined' ? null : localStorage,
      typeof sessionStorage === 'undefined' ? null : sessionStorage
    ];

    for (const storage of storages) {
      for (const key of LEGACY_AUTH_STORAGE_KEYS) {
        storage?.removeItem(key);
      }
    }
  }

  private normalizeSession(session: AuthSession, refreshExpiration: boolean): AuthSession {
    return {
      ...session,
      accessTokenExpiresAt:
        refreshExpiration || typeof session.accessTokenExpiresAt !== 'number'
          ? Date.now() + session.expiresIn * 1000
          : session.accessTokenExpiresAt,
      user: session.user ? this.normalizeUser(session.user) : null
    };
  }

  private completeAuthCheck(status: AuthStatus): void {
    this.authStatusState.set(status);
    this.authCheckDeferred.resolve(status);
  }

  private createAuthCheckDeferred(): {
    promise: Promise<AuthStatus>;
    resolve: (status: AuthStatus) => void;
  } {
    let resolve = (_status: AuthStatus): void => {};
    const promise = new Promise<AuthStatus>((innerResolve) => {
      resolve = innerResolve;
    });

    return { promise, resolve };
  }

  private normalizeUser(user: NonNullable<AuthSession['user']>): NonNullable<AuthSession['user']> {
    return {
      id: user.id,
      email: user.email,
      username: user.username,
      role: user.role,
      status: user.status,
      emailVerified: user.emailVerified,
      ...(user.avatar !== undefined ? { avatar: user.avatar } : {}),
      ...(user.matchmakingCode !== undefined ? { matchmakingCode: user.matchmakingCode } : {}),
      ...(user.createdAt !== undefined ? { createdAt: user.createdAt } : {}),
      ...(user.updatedAt !== undefined ? { updatedAt: user.updatedAt } : {})
    };
  }
}
