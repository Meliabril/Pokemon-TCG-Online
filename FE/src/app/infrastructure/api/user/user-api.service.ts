import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import {
  UpdateUserRequest,
  UserProfile
} from '../../../core/models/interfaces/user/update-user.interface';
import {
  ChangeCurrentUserPasswordRequest,
  VerifyPasswordChangeCodeRequest
} from '../../../core/models/interfaces/user/change-password.interface';
import {
  GenericMessageResponse
} from '../../../core/models/interfaces/auth/register-user.interface';
import {
  PasswordCodeVerificationResponse
} from '../../../core/models/interfaces/auth/password-recovery.interface';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);

  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.buildUrl(API_ENDPOINTS.users.profile));
  }

  updateMyProfile(request: UpdateUserRequest): Observable<UserProfile> {
    return this.http.patch<UserProfile>(this.buildUrl(API_ENDPOINTS.users.profile), request);
  }

  requestPasswordChangeCode(): Observable<GenericMessageResponse> {
    return this.http.post<GenericMessageResponse>(
      this.buildUrl(API_ENDPOINTS.users.passwordChangeCode),
      {}
    );
  }

  verifyPasswordChangeCode(
    request: VerifyPasswordChangeCodeRequest
  ): Observable<PasswordCodeVerificationResponse> {
    return this.http.post<PasswordCodeVerificationResponse>(
      this.buildUrl(API_ENDPOINTS.users.passwordChangeVerifyCode),
      request
    );
  }

  changeMyPassword(request: ChangeCurrentUserPasswordRequest): Observable<GenericMessageResponse> {
    return this.http.patch<GenericMessageResponse>(
      this.buildUrl(API_ENDPOINTS.users.password),
      request
    );
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
