import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DeckApiService } from './deck-api.service';

describe('DeckApiService', () => {
  let service: DeckApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(DeckApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads the authenticated user decks collection', () => {
    let responseLength = 0;

    service.getDecks().subscribe((decks) => {
      responseLength = decks.length;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/me/decks');
    expect(request.request.method).toBe('GET');
    request.flush([deck('inactive-deck', false), deck('active-deck', true)]);

    expect(responseLength).toBe(2);
  });

  it('derives the active deck from backend truth', () => {
    const state = { activeDeckId: null as string | null };

    service.getActiveDeck().subscribe((deck) => {
      state.activeDeckId = deck?.id ?? null;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/me/decks');
    request.flush([deck('inactive-deck', false), deck('active-deck', true)]);

    expect(state.activeDeckId).toBe('active-deck');
  });

  it('validates a specific deck by id', () => {
    let isValid = false;

    service.validateDeck('deck-123').subscribe((response) => {
      isValid = response.valid;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/me/decks/deck-123/validation');
    expect(request.request.method).toBe('GET');
    request.flush({ deckId: 'deck-123', valid: true, errors: [] });

    expect(isValid).toBeTrue();
  });

  it('activates a deck with a lightweight response', () => {
    let responseHasCards = true;

    service.activateDeck('deck-123').subscribe((response) => {
      responseHasCards = 'cards' in response;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/me/decks/deck-123/activate');
    expect(request.request.method).toBe('PUT');
    request.flush({ id: 'deck-123', active: true, valid: true, validationErrors: [] });

    expect(responseHasCards).toBeFalse();
  });

  it('randomizes a specific deck by id', () => {
    let responseDeckId = '';

    service.randomizeDeck('deck-123').subscribe((deck) => {
      responseDeckId = deck.id;
    });

    const request = httpMock.expectOne('http://localhost:8080/api/me/decks/deck-123/randomize');
    expect(request.request.method).toBe('PUT');
    request.flush(deck('deck-123', true));

    expect(responseDeckId).toBe('deck-123');
  });

  function deck(id: string, active: boolean) {
    return {
      id,
      ownerUserId: 'user-1',
      name: `Deck ${id}`,
      format: 'XY1_UNLIMITED',
      active,
      valid: true,
      validationErrors: [],
      cards: [],
      createdAt: '2026-05-27T00:00:00Z',
      updatedAt: '2026-05-27T00:00:00Z'
    };
  }
});
