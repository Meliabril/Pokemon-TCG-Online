import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { HealthStatus } from '../../../core/models/interfaces/system/health-status.interface';

@Injectable({ providedIn: 'root' })
export class HealthApiService {
  private readonly http = inject(HttpClient);

  getHealth(): Observable<HealthStatus> {
    return this.http.get<HealthStatus>(this.buildUrl(API_ENDPOINTS.health));
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
