import { DOCUMENT } from '@angular/common';
import { fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardAnimationService } from '../../services/board-animation.service';
import { BoardAnimationLayerComponent } from './board-animation-layer.component';

describe('BoardAnimationLayerComponent mobile draw', () => {
  let animationService: BoardAnimationService;
  let document: Document;
  let matchMediaSpy: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BoardAnimationLayerComponent],
      providers: [{ provide: LanguageService, useValue: { t: (key: string) => key } }]
    });
    animationService = TestBed.inject(BoardAnimationService);
    document = TestBed.inject(DOCUMENT);
    matchMediaSpy = spyOn(window, 'matchMedia');
    matchMediaSpy.and.callFake((query: string) => mediaQuery(query.includes('max-width')));
  });

  afterEach(() => {
    animationService.clear();
    document.querySelectorAll('[data-test-animation-anchor]').forEach((element) => element.remove());
  });

  it('moves a card back between the real source and mobile hand button bounds', fakeAsync(() => {
    anchor('local-deck', new DOMRect(10, 20, 40, 56));
    anchor('local-mobile-hand-button', new DOMRect(300, 200, 120, 40));
    const fixture = TestBed.createComponent(BoardAnimationLayerComponent);
    fixture.detectChanges();
    flushMicrotasks();

    animationService.enqueue([{
      type: 'PLAYER_DRAW_CARD',
      fromAnchor: 'local-deck',
      toAnchor: 'local-hand',
      mobileToAnchor: 'local-mobile-hand-button'
    }]);
    fixture.detectChanges();
    flushMicrotasks();

    const startTransform = fixture.componentInstance.floatingCard()?.transform;
    expect(fixture.componentInstance.floatingCard()?.isBack).toBeTrue();

    tick(16);
    const travellingCard = fixture.componentInstance.floatingCard();
    expect(travellingCard?.transform).not.toBe(startTransform);
    expect(travellingCard?.transition).toContain('450ms');

    tick(466);
    expect(fixture.componentInstance.floatingCard()).toBeNull();
    expect(animationService.activeAnimation()).toBeNull();
  }));

  it('skips mobile draw animation when reduced motion is requested', fakeAsync(() => {
    matchMediaSpy.and.callFake((query: string) => mediaQuery(
      query.includes('max-width') || query.includes('prefers-reduced-motion')
    ));
    anchor('opponent-deck', new DOMRect(10, 20, 40, 56));
    anchor('opponent-mobile-hand-button', new DOMRect(300, 20, 120, 40));
    const fixture = TestBed.createComponent(BoardAnimationLayerComponent);
    fixture.detectChanges();
    flushMicrotasks();

    animationService.enqueue([{
      type: 'OPPONENT_DRAW_CARD',
      fromAnchor: 'opponent-deck',
      toAnchor: 'opponent-hand',
      mobileToAnchor: 'opponent-mobile-hand-button'
    }]);
    fixture.detectChanges();
    flushMicrotasks();

    expect(fixture.componentInstance.floatingCard()).toBeNull();
    expect(animationService.activeAnimation()).toBeNull();
  }));

  it('completes normally when a mobile source or target element is missing', fakeAsync(() => {
    const fixture = TestBed.createComponent(BoardAnimationLayerComponent);
    fixture.detectChanges();
    flushMicrotasks();

    animationService.enqueue([{
      type: 'PLAYER_DRAW_CARD',
      fromAnchor: 'missing-deck',
      toAnchor: 'local-hand',
      mobileToAnchor: 'missing-hand-button'
    }]);
    fixture.detectChanges();
    flushMicrotasks();

    expect(fixture.componentInstance.floatingCard()).toBeNull();
    expect(animationService.activeAnimation()).toBeNull();
  }));

  function anchor(anchorId: string, rect: DOMRect): void {
    const element = document.createElement('div');
    element.dataset['boardAnchor'] = anchorId;
    element.dataset['testAnimationAnchor'] = 'true';
    spyOn(element, 'getBoundingClientRect').and.returnValue(rect);
    document.body.appendChild(element);
  }
});

function mediaQuery(matches: boolean): MediaQueryList {
  return {
    matches,
    media: '',
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false
  };
}
