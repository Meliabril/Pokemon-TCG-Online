import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { BoardVisualOwner, BoardZone } from '../../../../core/models/interfaces/game/game-visual-event.interface';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';
import { BoardCardViewModel } from '../../domain/board/board-game-view-model.interface';
import {
  CardHoverVisualTarget,
  cardHoverKey
} from '../../domain/cards/card-hover-visual-target.interface';

interface VisibleZoneCard {
  index: number;
  card: BoardCardViewModel | null;
}

const MAX_VISIBLE_DISCARD_CARDS = 1;

@Component({
  selector: 'app-card-zone-stack',
  templateUrl: './card-zone-stack.component.html',
  styleUrl: './card-zone-stack.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardZoneStackComponent {
  private readonly hostElement = inject(ElementRef<HTMLElement>);
  private readonly languageService = inject(LanguageService);

  readonly label = input.required<string>();
  readonly count = input.required<number>();
  readonly cards = input<BoardCardViewModel[]>([]);
  readonly variant = input<'deck' | 'discard' | 'prize'>('deck');
  readonly showDrawAction = input(false);
  readonly drawEnabled = input(false);
  readonly drawDisabledReason = input<string | null>(null);
  readonly animationAnchorId = input<string | null>(null);
  readonly visualOwner = input<BoardVisualOwner>('SELF');
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly cardInspected = output<BoardCardViewModel>();
  readonly drawRequested = output<void>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
  readonly deckSelected = signal(false);
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);
  readonly emptyLabel = computed(() => {
    const zoneEmptyLabel = this.t('GAME.ZONE_EMPTY');
    return zoneEmptyLabel === 'GAME.ZONE_EMPTY' ? this.t('COMMON.EMPTY') : zoneEmptyLabel;
  });
  readonly isEmpty = computed(() => this.count() === 0);
  readonly visibleCards = computed<VisibleZoneCard[]>(() => {
    const cards = this.cards();
    const visibleCount = this.visibleCardCount(cards.length);
    const visibleSource =
      this.variant() === 'discard'
        ? cards.slice(Math.max(cards.length - visibleCount, 0))
        : cards.slice(0, visibleCount);

    return Array.from({ length: visibleCount }, (_, index) => ({
      index,
      card: visibleSource[index] ?? null
    }));
  });

  private visibleCardCount(cardCount: number): number {
    if (this.count() === 0) {
      return this.variant() === 'discard' ? 1 : 0;
    }

    if (this.variant() === 'prize') {
      return Math.min(this.count(), 6);
    }

    if (this.variant() === 'discard') {
      return Math.min(Math.max(this.count(), cardCount), MAX_VISIBLE_DISCARD_CARDS);
    }

    return 1;
  }

  inspectCard(card: BoardCardViewModel | null): void {
    if (!card || card.faceDown || card.visibility !== 'visible') {
      return;
    }

    this.cardInspected.emit(card);
  }

  isRemoteHovered(visibleCard: VisibleZoneCard): boolean {
    const hoverKey = this.hoverKey(visibleCard);
    return hoverKey !== null && this.remoteHoveredCardKey() === hoverKey;
  }

  emitCardHover(visibleCard: VisibleZoneCard, hovered: boolean): void {
    if (this.visualOwner() !== 'SELF') {
      return;
    }

    const zone = this.hoverZone();
    if (!zone) {
      return;
    }

    this.cardHoverChanged.emit({
      zone,
      owner: this.visualOwner(),
      visualIndex: visibleCard.index,
      cardInstanceId: null,
      hovered
    });
  }

  prizeOffset(index: number): number {
    return (index - 2.5) * 10;
  }

  discardOffset(index: number): number {
    return index * 0;
  }

  handleCardClick(event: MouseEvent, card: BoardCardViewModel | null): void {
    if (this.variant() === 'deck') {
      event.preventDefault();
      event.stopPropagation();
      this.handleDeckAction();
      return;
    }

    this.inspectCard(card);
  }

  private handleDeckAction(): void {
    if (this.showDrawAction()) {
      if (this.drawEnabled()) {
        this.drawRequested.emit();
      }
      this.deckSelected.set(false);
      return;
    }

    this.deckSelected.update((isSelected) => !isSelected);
  }

  handleCardKeydown(event: KeyboardEvent, card: BoardCardViewModel | null): void {
    if (event.key !== 'Enter' && event.key !== ' ') {
      return;
    }

    if (this.variant() === 'deck') {
      event.preventDefault();
      event.stopPropagation();
      this.handleDeckAction();
      return;
    }

    if (event.key === ' ') {
      event.preventDefault();
    }
    this.inspectCard(card);
  }

  @HostListener('document:click', ['$event'])
  handleDocumentClick(event: MouseEvent): void {
    if (this.hostElement.nativeElement.contains(event.target as Node)) {
      return;
    }

    this.deckSelected.set(false);
  }

  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    this.deckSelected.set(false);
  }

  private hoverKey(visibleCard: VisibleZoneCard): string | null {
    const zone = this.hoverZone();
    if (!zone) {
      return null;
    }

    return cardHoverKey({
      owner: this.visualOwner(),
      zone,
      visualIndex: visibleCard.index,
      cardInstanceId: null
    });
  }

  private hoverZone(): BoardZone | null {
    if (this.variant() === 'deck') {
      return 'DECK';
    }

    if (this.variant() === 'prize') {
      return 'PRIZES';
    }

    if (this.variant() === 'discard') {
      return 'DISCARD';
    }

    return null;
  }
}
