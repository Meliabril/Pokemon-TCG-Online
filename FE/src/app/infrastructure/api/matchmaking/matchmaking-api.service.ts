import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { MatchmakingQueueStatus } from '../../../core/models/interfaces/matchmaking/matchmaking-queue-status.interface';

@Injectable({ providedIn: 'root' })
export class MatchmakingApiService {
  private readonly http = inject(HttpClient);

  joinQueue(): Observable<MatchmakingQueueStatus> {
    return this.http.post<MatchmakingQueueStatus>(this.buildUrl(API_ENDPOINTS.matchmaking.queue), {});
  }

  createCustomQueue(): Observable<MatchmakingQueueStatus> {
    return this.http.post<MatchmakingQueueStatus>(this.buildUrl(API_ENDPOINTS.matchmaking.customQueue), {});
  }

  joinCustomQueue(code: string): Observable<MatchmakingQueueStatus> {
    return this.http.post<MatchmakingQueueStatus>(this.buildUrl(API_ENDPOINTS.matchmaking.joinCustomQueue(code)), {});
  }

  leaveQueue(): Observable<void> {
    return this.http.delete<void>(this.buildUrl(API_ENDPOINTS.matchmaking.queue));
  }

  getMyQueueStatus(): Observable<MatchmakingQueueStatus> {
    return this.http.get<MatchmakingQueueStatus>(this.buildUrl(API_ENDPOINTS.matchmaking.myStatus));
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
