import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { CardImportStatus, CardSummary } from '../../../core/models/interfaces/card/card-summary.interface';

@Injectable({ providedIn: 'root' })
export class CardsApiService {
  private readonly http = inject(HttpClient);

  getCards(setCode?: string): Observable<CardSummary[]> {
    const params = setCode ? new HttpParams().set('setCode', setCode) : undefined;
    return this.http.get<CardSummary[]>(this.buildUrl(API_ENDPOINTS.cards.list), { params });
  }

  getCardById(cardId: string): Observable<CardSummary> {
    return this.http.get<CardSummary>(this.buildUrl(`${API_ENDPOINTS.cards.list}/${cardId}`));
  }

  searchCards(name: string, setCode?: string): Observable<CardSummary[]> {
    const params = setCode
      ? new HttpParams().set('setCode', setCode).set('name', name)
      : new HttpParams().set('name', name);
    return this.http.get<CardSummary[]>(this.buildUrl(API_ENDPOINTS.cards.search), { params });
  }

  getImportStatus(): Observable<CardImportStatus> {
    return this.http.get<CardImportStatus>(this.buildUrl(API_ENDPOINTS.cards.importStatusXy1));
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
