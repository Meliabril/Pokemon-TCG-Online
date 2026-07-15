import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { APP_CONFIG } from '../../../core/constants/app/app.constants';
import { API_ENDPOINTS } from '../../../core/constants/api/api-endpoints.constants';
import { DeckCardRequest, DeckUpsertRequest } from '../../../core/models/interfaces/deck/deck-command.interface';
import {
  DeckActivationResponse,
  DeckSummary,
  DeckValidationResponse
} from '../../../core/models/interfaces/deck/deck-summary.interface';

@Injectable({ providedIn: 'root' })
export class DeckApiService {
  private readonly http = inject(HttpClient);

  getDecks(): Observable<DeckSummary[]> {
    return this.http.get<DeckSummary[]>(this.buildUrl(API_ENDPOINTS.deck.list));
  }

  getActiveDeck(): Observable<DeckSummary | null> {
    return this.getDecks().pipe(map((decks) => decks.find((deck) => deck.active) ?? null));
  }

  createDeck(request: DeckUpsertRequest): Observable<DeckSummary> {
    return this.http.post<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.list), request);
  }

  createRandomDeck(): Observable<DeckSummary> {
    return this.http.post<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.createRandom), {});
  }

  getDeck(deckId: string): Observable<DeckSummary> {
    return this.http.get<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.detail(deckId)));
  }

  replaceDeck(deckId: string, request: DeckUpsertRequest): Observable<DeckSummary> {
    return this.http.put<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.detail(deckId)), request);
  }

  addCard(deckId: string, request: DeckCardRequest): Observable<DeckSummary> {
    return this.http.post<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.cards(deckId)), request);
  }

  removeCard(deckId: string, cardId: string): Observable<DeckSummary> {
    return this.http.delete<DeckSummary>(this.buildUrl(`${API_ENDPOINTS.deck.cards(deckId)}/${cardId}`));
  }

  validateDeck(deckId: string): Observable<DeckValidationResponse> {
    return this.http.get<DeckValidationResponse>(this.buildUrl(API_ENDPOINTS.deck.validation(deckId)));
  }

  activateDeck(deckId: string): Observable<DeckActivationResponse> {
    return this.http.put<DeckActivationResponse>(this.buildUrl(API_ENDPOINTS.deck.activate(deckId)), {});
  }

  randomizeDeck(deckId: string): Observable<DeckSummary> {
    return this.http.put<DeckSummary>(this.buildUrl(API_ENDPOINTS.deck.randomize(deckId)), {});
  }

  deleteDeck(deckId: string): Observable<void> {
    return this.http.delete<void>(this.buildUrl(API_ENDPOINTS.deck.detail(deckId)));
  }

  private buildUrl(path: string): string {
    return `${APP_CONFIG.apiBaseUrl}${path}`;
  }
}
