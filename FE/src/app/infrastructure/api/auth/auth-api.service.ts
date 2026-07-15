import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, switchMap, tap } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import {
  AuthSession,
  LoginRequest,
  LogoutResponse
} from '../../../core/models/interfaces/auth/auth-session.interface';
import {
  ForgotPasswordRequest,
  PasswordCodeVerificationResponse,
  VerifiedPasswordResetRequest,
  VerifyPasswordResetCodeRequest
} from '../../../core/models/interfaces/auth/password-recovery.interface';
import {
  GenericMessageResponse,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationCodeRequest,
  VerifyAccountRequest
} from '../../../core/models/interfaces/auth/register-user.interface';
import { CurrentUser } from '../../../core/models/interfaces/user/current-user.interface';
import { StorageService } from '../../../core/storage/storage.service';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly storageService = inject(StorageService);
  private readonly authRequestOptions = { withCredentials: true };

  login(credentials: LoginRequest, rememberMe = true): Observable<AuthSession> {
    return this.http
      .post<AuthSession>(this.buildUrl(API_ENDPOINTS.auth.login), credentials, this.authRequestOptions)
      .pipe(
        tap((session) => this.storageService.saveSession(session, rememberMe)),
        switchMap(() => this.getCurrentUser().pipe(
          map(() => this.storageService.getSession() as AuthSession)
        ))
      );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(this.buildUrl(API_ENDPOINTS.users.register), request);
  }

  verifyAccount(payload: VerifyAccountRequest, rememberMe = true): Observable<AuthSession> {
    return this.http
      .post<AuthSession>(this.buildUrl(API_ENDPOINTS.auth.verifyAccount), payload, this.authRequestOptions)
      .pipe(
        tap((session) => this.storageService.saveSession(session, rememberMe)),
        switchMap(() => this.getCurrentUser().pipe(
          map(() => this.storageService.getSession() as AuthSession)
        ))
      );
  }

  resendVerificationCode(
    payload: ResendVerificationCodeRequest
  ): Observable<GenericMessageResponse> {
    return this.http.post<GenericMessageResponse>(
      this.buildUrl(API_ENDPOINTS.auth.resendVerificationCode),
      payload
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<GenericMessageResponse> {
    return this.http.post<GenericMessageResponse>(
      this.buildUrl(API_ENDPOINTS.auth.passwordForgot),
      request
    );
  }

  verifyPasswordResetCode(
    request: VerifyPasswordResetCodeRequest
  ): Observable<PasswordCodeVerificationResponse> {
    return this.http.post<PasswordCodeVerificationResponse>(
      this.buildUrl(API_ENDPOINTS.auth.passwordVerifyCode),
      request
    );
  }

  resetVerifiedPassword(request: VerifiedPasswordResetRequest): Observable<GenericMessageResponse> {
    return this.http.post<GenericMessageResponse>(
      this.buildUrl(API_ENDPOINTS.auth.passwordResetVerified),
      request
    );
  }

  refresh(): Observable<AuthSession> {
    return this.http
      .post<Pick<AuthSession, 'accessToken' | 'tokenType' | 'expiresIn'>>(
        this.buildUrl(API_ENDPOINTS.auth.refresh),
        {},
        this.authRequestOptions
      )
      .pipe(map((tokens) => this.storageService.updateAccessToken(tokens)));
  }

  initializeSession(): Observable<void> {
    this.storageService.markChecking();

    return this.refresh().pipe(
      switchMap(() => this.getCurrentUser()),
      map(() => undefined),
      catchError(() => {
        this.storageService.clearSession();
        return of(undefined);
      })
    );
  }

  logout(): Observable<LogoutResponse> {
    if (!this.storageService.getSession()) {
      this.storageService.clearSession();
      return of({ message: 'Local session cleared.' });
    }

    return this.http.post<LogoutResponse>(
      this.buildUrl(API_ENDPOINTS.auth.logout),
      {},
      this.authRequestOptions
    ).pipe(
      tap(() => this.storageService.clearSession())
    );
  }

  getCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(this.buildUrl(API_ENDPOINTS.auth.me), this.authRequestOptions).pipe(
      tap((user) => this.storageService.updateCurrentUser(user))
    );
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
