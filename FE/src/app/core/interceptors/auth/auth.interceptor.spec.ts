import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../../constants/app/app.constants';
import { UserRole } from '../../models/enums/user/user-role.enum';
import { UserStatus } from '../../models/enums/user/user-status.enum';
import { StorageService } from '../../storage/storage.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.inject(HttpTestingController).verify();
  });

  it('attaches the bearer token when a session exists', () => {
    const storageService = TestBed.inject(StorageService);
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    storageService.saveSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 'user-id',
        email: 'misty@kanto.dev',
        username: 'misty',
        role: UserRole.User,
        status: UserStatus.Active,
        emailVerified: true,
        createdAt: '2026-05-24T00:00:00Z',
        updatedAt: '2026-05-24T00:00:00Z'
      }
    });

    const resourceUrl = `${APP_CONFIG.apiBaseUrl}/resource`;
    http.get(resourceUrl).subscribe();

    const request = httpMock.expectOne(resourceUrl);
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({});
  });

  it('authenticates trainer preview exactly like protected game actions', () => {
    const storageService = TestBed.inject(StorageService);
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    storageService.saveSession({
      accessToken: 'game-access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 'player-id',
        email: 'player@kanto.dev',
        username: 'player',
        role: UserRole.User,
        status: UserStatus.Active,
        emailVerified: true,
        createdAt: '2026-06-27T00:00:00Z',
        updatedAt: '2026-06-27T00:00:00Z'
      }
    });

    const gameId = 'game-id';
    const protectedUrls = [
      `${APP_CONFIG.apiBaseUrl}/api/games/${gameId}/actions`,
      `${APP_CONFIG.apiBaseUrl}/api/games/${gameId}/trainer-preview`
    ];

    for (const url of protectedUrls) {
      http.post(url, {}).subscribe();

      const request = httpMock.expectOne(url);
      expect(request.request.headers.get('Authorization')).toBe('Bearer game-access-token');
      expect(request.request.withCredentials).toBeTrue();
      request.flush({});
    }
  });

  it('does not attach the authorization header when there is no session', () => {
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    http.get('/public-resource').subscribe();

    const request = httpMock.expectOne('/public-resource');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('does not attach the authorization header to public auth and register endpoints', () => {
    const storageService = TestBed.inject(StorageService);
    const http = TestBed.inject(HttpClient);
    const httpMock = TestBed.inject(HttpTestingController);

    storageService.saveSession({
      accessToken: 'access-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: {
        id: 'user-id',
        email: 'brock@kanto.dev',
        username: 'brock',
        role: UserRole.User,
        status: UserStatus.Active,
        emailVerified: true,
        createdAt: '2026-05-24T00:00:00Z',
        updatedAt: '2026-05-24T00:00:00Z'
      }
    });

    const publicUrls = [
      'http://localhost:8080/api/auth/login',
      'http://localhost:8080/api/auth/refresh',
      'http://localhost:8080/api/auth/verify-account',
      'http://localhost:8080/api/auth/resend-verification-code',
      'http://localhost:8080/api/users',
      'http://localhost:8080/api/users/create'
    ];

    for (const url of publicUrls) {
      http.post(url, {}).subscribe();

      const request = httpMock.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBeFalse();
      if (url.includes('/api/auth/login') || url.includes('/api/auth/refresh') || url.includes('/api/auth/verify-account')) {
        expect(request.request.withCredentials).toBeTrue();
      }
      request.flush({});
    }
  });
});
