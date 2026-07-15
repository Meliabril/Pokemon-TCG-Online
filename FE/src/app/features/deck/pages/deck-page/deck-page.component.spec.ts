import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject } from 'rxjs';
import { DeckActivationResponse, DeckSummary } from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { CardsApiService } from '../../../../infrastructure/api/card/cards-api.service';
import { DeckApiService } from '../../../../infrastructure/api/deck/deck-api.service';
import { DeckPageComponent } from './deck-page.component';

describe('DeckPageComponent', () => {
  let deckApi: jasmine.SpyObj<DeckApiService>;
  let cardsApi: jasmine.SpyObj<CardsApiService>;

  beforeEach(() => {
    deckApi = jasmine.createSpyObj<DeckApiService>('DeckApiService', ['getDecks', 'activateDeck']);
    cardsApi = jasmine.createSpyObj<CardsApiService>('CardsApiService', ['getCards']);
    deckApi.getDecks.and.returnValue(of([deck('deck-1', true), deck('deck-2', false)]));
    cardsApi.getCards.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [DeckPageComponent],
      providers: [
        { provide: DeckApiService, useValue: deckApi },
        { provide: CardsApiService, useValue: cardsApi },
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(convertToParamMap({})) }
        }
      ]
    });
  });

  it('updates activation optimistically and keeps the lightweight server result', () => {
    const activation = new Subject<DeckActivationResponse>();
    deckApi.activateDeck.and.returnValue(activation);
    const component = TestBed.createComponent(DeckPageComponent).componentInstance;

    component.activateDeck('deck-2');

    expect(component.activeDeck()?.id).toBe('deck-2');

    activation.next({ id: 'deck-2', active: true, valid: true, validationErrors: [] });
    activation.complete();

    expect(component.activeDeck()?.id).toBe('deck-2');
  });

  it('restores the previous active deck when activation fails', () => {
    const activation = new Subject<DeckActivationResponse>();
    deckApi.activateDeck.and.returnValue(activation);
    const component = TestBed.createComponent(DeckPageComponent).componentInstance;

    component.activateDeck('deck-2');
    expect(component.activeDeck()?.id).toBe('deck-2');

    activation.error(new Error('activation failed'));

    expect(component.activeDeck()?.id).toBe('deck-1');
  });

  function deck(id: string, active: boolean): DeckSummary {
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
