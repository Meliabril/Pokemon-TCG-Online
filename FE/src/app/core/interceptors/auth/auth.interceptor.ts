import {
  HttpClient,
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, shareReplay, switchMap, throwError } from 'rxjs';
import { API_ENDPOINTS } from '../../constants/api/api-endpoints.constants';
import { APP_CONFIG } from '../../constants/app/app.constants';
import { APP_ROUTES } from '../../constants/routing/routes.constants';
import { AuthSession } from '../../models/interfaces/auth/auth-session.interface';
import { StorageService } from '../../storage/storage.service';

const PUBLIC_PATHS = [
  '/api/auth/login',
  '/api/auth/refresh',
  '/api/auth/verify-account',
  '/api/auth/resend-verification-code',
  '/api/auth/password/forgot',
  '/api/auth/password/reset',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/users',
  '/api/users/create'
];

const RETRY_BYPASS_PATHS = [
  '/api/auth/login',
  '/api/auth/refresh',
  '/api/auth/me'
];

const AUTH_RETRY_CONTEXT = new HttpContextToken<boolean>(() => false);

let refreshSessionRequest$: Observable<AuthSession> | null = null;

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(APP_CONFIG.apiBaseUrl)) {
    return next(request);
  }

  const storageService = inject(StorageService);
  const http = inject(HttpClient);
  const requestPath = new URL(request.url, APP_CONFIG.apiBaseUrl).pathname;
  const isPublicRequest = PUBLIC_PATHS.includes(requestPath);
  const router = inject(Router);
  const requestWithCredentials = addCredentialsIfNeeded(request, requestPath);
  const accessToken = storageService.getAccessToken();

  if (isPublicRequest) {
    return next(requestWithCredentials);
  }

  if (!accessToken) {
    if (storageService.authStatus() !== 'authenticated' || shouldBypassRetry(requestPath, requestWithCredentials)) {
      return next(requestWithCredentials);
    }

    return refreshSession(http, storageService).pipe(
      catchError((error: unknown) => handleRefreshFailure(storageService, router, error)),
      switchMap((session) => sendAuthorizedRequest(requestWithCredentials, session.accessToken, next, true))
    );
  }

  if (storageService.isAccessTokenExpired()) {
    return refreshSession(http, storageService).pipe(
      catchError((error: unknown) => handleRefreshFailure(storageService, router, error)),
      switchMap((session) => sendAuthorizedRequest(requestWithCredentials, session.accessToken, next, true))
    );
  }

  return sendAuthorizedRequest(requestWithCredentials, accessToken, next).pipe(
    catchError((error: unknown) => {
      if (
        !(error instanceof HttpErrorResponse) ||
        error.status !== 401 ||
        shouldBypassRetry(requestPath, requestWithCredentials)
      ) {
        return throwError(() => error);
      }

      return refreshSession(http, storageService).pipe(
        catchError((refreshError: unknown) => handleRefreshFailure(storageService, router, refreshError)),
        switchMap((session) => sendAuthorizedRequest(requestWithCredentials, session.accessToken, next, true))
      );
    })
  );
};

function sendAuthorizedRequest(
  request: HttpRequest<unknown>,
  accessToken: string,
  next: Parameters<HttpInterceptorFn>[1],
  alreadyRetried = false
) {
  return next(addAuthorization(request, accessToken, alreadyRetried));
}

function addAuthorization(
  request: HttpRequest<unknown>,
  accessToken: string,
  alreadyRetried: boolean
): HttpRequest<unknown> {
  return request.clone({
    context: request.context.set(AUTH_RETRY_CONTEXT, alreadyRetried),
    setHeaders: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

function refreshSession(http: HttpClient, storageService: StorageService): Observable<AuthSession> {
  if (refreshSessionRequest$) {
    return refreshSessionRequest$;
  }

  refreshSessionRequest$ = http
    .post<Pick<AuthSession, 'accessToken' | 'tokenType' | 'expiresIn'>>(
      `${APP_CONFIG.apiBaseUrl}${API_ENDPOINTS.auth.refresh}`,
      {},
      { withCredentials: true }
    )
    .pipe(
      map((tokens) => {
        return storageService.updateAccessToken(tokens);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
      finalize(() => {
        refreshSessionRequest$ = null;
      })
    );

  return refreshSessionRequest$;
}

function addCredentialsIfNeeded(request: HttpRequest<unknown>, requestPath: string): HttpRequest<unknown> {
  void requestPath;
  if (request.withCredentials) {
    return request;
  }

  return request.clone({ withCredentials: true });
}

function shouldBypassRetry(requestPath: string, request: HttpRequest<unknown>): boolean {
  return RETRY_BYPASS_PATHS.includes(requestPath) || request.context.get(AUTH_RETRY_CONTEXT);
}

function handleRefreshFailure(storageService: StorageService, router: Router, error: unknown) {
  storageService.clearSession();
  void router.navigate([`/${APP_ROUTES.login}`]);
  return throwError(() => error);
}
