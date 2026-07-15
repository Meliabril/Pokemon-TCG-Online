import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { PageResponse } from '../../../core/models/interfaces/common/page-response.interface';
import {
  MatchHistory,
  MatchHistoryFilter,
  PlayerStats
} from '../../../core/models/interfaces/match-history/match-history.interface';

@Injectable({ providedIn: 'root' })
export class MatchHistoryService {
  private readonly http = inject(HttpClient);

  getPlayerStats(): Observable<PlayerStats> {
    return this.http.get<PlayerStats>(this.buildUrl(API_ENDPOINTS.matches.stats));
  }

  getHistory(
    filter: MatchHistoryFilter,
    page: number,
    size: number
  ): Observable<PageResponse<MatchHistory>> {
    const params = new HttpParams()
      .set('filter', filter)
      .set('page', page)
      .set('size', size);

    return this.http.get<PageResponse<MatchHistory>>(this.buildUrl(API_ENDPOINTS.matches.history), {
      params
    });
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
