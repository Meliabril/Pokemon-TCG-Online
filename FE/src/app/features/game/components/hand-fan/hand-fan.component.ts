import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  inject,
  input,
  output,
  signal
} from '@angular/core';
import { BoardVisualOwner } from '../../../../core/models/interfaces/game/game-visual-event.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardCardViewModel } from '../../domain/board/board-game-view-model.interface';
import {
  CardHoverVisualTarget,
  cardHoverKey
} from '../../domain/cards/card-hover-visual-target.interface';
import { cardReferenceId } from '../../domain/interaction/game-interaction.helpers';

type HandFanSize = 'compact' | 'large';
const MAX_FAN_ROTATION_DEGREES = 18;
const LARGE_HAND_SPACING = 42;
const COMPACT_HAND_SPACING = 28;
const LARGE_HAND_MIN_SPACING = 22;
const COMPACT_HAND_MIN_SPACING = 16;
const MOBILE_HAND_SPACING_FACTOR = 0.38;
const MOBILE_HAND_ROTATION_FACTOR = 0.55;

export interface HandCardDragStart {
  card: BoardCardViewModel;
  source: 'current-hand';
}

export interface HandCardPointerDrop {
  cardId: string;
  source: 'current-hand';
  target: 'active' | 'bench' | 'trainer-play-area' | 'stadium-area' | 'pokemon-tool-target';
  slotId?: string;
  pokemonInPlayId?: string;
}

interface PointerDragState {
  card: BoardCardViewModel;
  captureTarget: HTMLElement;
  pointerId: number;
  startX: number;
  startY: number;
  started: boolean;
  scrolling: boolean;
}

const POINTER_DRAG_THRESHOLD_PX = 8;

@Component({
  selector: 'app-hand-fan',
  templateUrl: './hand-fan.component.html',
  styleUrl: './hand-fan.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HandFanComponent {
  private readonly languageService = inject(LanguageService);

  readonly label = input.required<string>();
  readonly cards = input.required<BoardCardViewModel[]>();
  readonly hoverable = input(false);
  readonly size = input<HandFanSize>('compact');
  readonly draggableCards = input(false);
  readonly selectedCardInstanceId = input<string | null>(null);
  readonly setupActiveCardInstanceId = input<string | null>(null);
  readonly setupBenchCardInstanceIds = input<string[]>([]);
  readonly selectableCardInstanceIds = input<string[]>([]);
  readonly playableCardIds = input<string[]>([]);
  readonly animationAnchorId = input<string | null>(null);
  readonly visualOwner = input<BoardVisualOwner>('SELF');
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly endTurnEnabled = input(false);
  readonly endTurnDisabledReason = input<string | null>(null);
  readonly setupPhase = input(false);
  readonly setupSubmitted = input(false);
  readonly setupSubmitEnabled = input(false);
  readonly setupDisabledReason = input<string | null>(null);
  readonly setupBusy = input(false);
  /** True when the local player has a pending Mulligan barrier; the primary action becomes "Iniciar mulligan". */
  readonly mulliganActionPending = input(false);
  readonly cardDragStarted = output<HandCardDragStart>();
  readonly cardDragEnded = output<void>();
  readonly cardPointerDropped = output<HandCardPointerDrop>();
  readonly cardSelected = output<string>();
  readonly cardInspected = output<BoardCardViewModel>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
  readonly endTurnRequested = output<void>();
  readonly setupSubmitRequested = output<void>();
  readonly setupBenchClearRequested = output<void>();
  readonly mulliganStartRequested = output<void>();
  readonly draggingCardId = signal<string | null>(null);
  readonly draggingOutsideHand = signal(false);
  readonly pointerDraggingCardId = signal<string | null>(null);
  readonly pointerDragX = signal(0);
  readonly pointerDragY = signal(0);
  readonly pointerClientX = signal(0);
  readonly pointerClientY = signal(0);
  readonly mobileDrawerOpen = signal(false);
  readonly pointerDraggingCard = computed(() => {
    const draggingCardId = this.pointerDraggingCardId();
    return this.cards().find((card) => this.cardReferenceId(card) === draggingCardId) ?? null;
  });
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  private pointerDragState: PointerDragState | null = null;
  private suppressNextClick = false;
  private readonly touchOnlyPointer = window.matchMedia('(hover: none), (pointer: coarse)');

  cardReferenceId(card: BoardCardViewModel): string {
    return cardReferenceId(card);
  }

  isSelected(card: BoardCardViewModel): boolean {
    return this.selectedCardInstanceId() === this.cardReferenceId(card);
  }

  isSetupActive(card: BoardCardViewModel): boolean {
    return this.setupActiveCardInstanceId() === this.cardReferenceId(card);
  }

  isSetupBench(card: BoardCardViewModel): boolean {
    return this.setupBenchCardInstanceIds().includes(this.cardReferenceId(card));
  }

  isSelectable(card: BoardCardViewModel): boolean {
    return this.selectableCardInstanceIds().includes(this.cardReferenceId(card));
  }

  isPlayableHighlight(card: BoardCardViewModel): boolean {
    if (card.playable === true && !card.disabledReason) {
      return true;
    }
    if (!card.playable) {
      const refId = cardReferenceId(card);
      return this.isInspectable(card) && this.playableCardIds().includes(refId);
    }
    return false;
  }

  isInspectable(card: BoardCardViewModel): boolean {
    return !card.faceDown && card.visibility === 'visible';
  }

  isDraggable(card: BoardCardViewModel): boolean {
    return this.draggableCards() && this.playableCardIds().includes(this.cardReferenceId(card));
  }

  isRemoteHovered(card: BoardCardViewModel, index: number): boolean {
    return this.remoteHoveredCardKey() === this.hoverKey(card, index);
  }

  selectCard(card: BoardCardViewModel): void {
    if (this.suppressNextClick) {
      this.suppressNextClick = false;
      return;
    }

    if (!this.isInspectable(card)) {
      return;
    }

    if (this.touchOnlyPointer.matches) {
      this.closeMobileDrawer();
    }
    this.cardInspected.emit(card);
  }

  openMobileDrawer(): void {
    this.mobileDrawerOpen.set(true);
  }

  closeMobileDrawer(): void {
    if (this.pointerDragState?.started) {
      return;
    }
    this.mobileDrawerOpen.set(false);
  }

  selectMobileCard(card: BoardCardViewModel): void {
    if (this.suppressNextClick) {
      this.suppressNextClick = false;
      return;
    }
    if (this.isInspectable(card)) {
      this.mobileDrawerOpen.set(false);
      this.cardInspected.emit(card);
    }
  }

  cardOffset(card: BoardCardViewModel, index: number): number {
    const visibleIndex = this.cardVisualIndex(card, index);
    const centerIndex = (this.visibleCardCount() - 1) / 2;
    const cardSpacing = this.cardSpacing();

    return (visibleIndex - centerIndex) * cardSpacing;
  }

  cardRotation(card: BoardCardViewModel, index: number): number {
    const visibleCards = this.visibleCardCount();
    if (visibleCards <= 1) {
      return card.rotation;
    }

    const visibleIndex = this.cardVisualIndex(card, index);
    const centerIndex = (visibleCards - 1) / 2;
    const fanStep = Math.min(5, (MAX_FAN_ROTATION_DEGREES * 2) / Math.max(visibleCards - 1, 1));

    return card.rotation + (visibleIndex - centerIndex) * fanStep;
  }

  cardStackOrder(card: BoardCardViewModel, index: number): number {
    return this.cardVisualIndex(card, index) + 1;
  }

  isDraggingOutsideHand(card: BoardCardViewModel): boolean {
    return this.draggingOutsideHand() && this.draggingCardId() === this.cardReferenceId(card);
  }

  startCardDrag(event: DragEvent, card: BoardCardViewModel): void {
    if (!this.isDraggable(card)) {
      event.preventDefault();
      return;
    }

    this.draggingCardId.set(this.cardReferenceId(card));
    this.draggingOutsideHand.set(false);
    this.emitCardHover(card, this.cards().indexOf(card), false);
    event.dataTransfer?.setData('application/x-pokemon-card-id', this.cardReferenceId(card));
    event.dataTransfer?.setData('application/x-pokemon-card-source', 'current-hand');

    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }

    this.cardDragStarted.emit({ card, source: 'current-hand' });
  }

  trackCardDrag(event: DragEvent, handElement: HTMLElement): void {
    if (!this.draggingCardId() || (event.clientX === 0 && event.clientY === 0)) {
      return;
    }

    const handBounds = handElement.getBoundingClientRect();
    const isOutsideHand =
      event.clientX < handBounds.left ||
      event.clientX > handBounds.right ||
      event.clientY < handBounds.top ||
      event.clientY > handBounds.bottom;

    if (this.draggingOutsideHand() !== isOutsideHand) {
      this.draggingOutsideHand.set(isOutsideHand);
    }
  }

  finishCardDrag(): void {
    this.draggingCardId.set(null);
    this.draggingOutsideHand.set(false);
    this.cardDragEnded.emit();
  }

  mobileCardOffset(card: BoardCardViewModel, index: number): number {
    return this.cardOffset(card, index) * MOBILE_HAND_SPACING_FACTOR;
  }

  mobileCardRotation(card: BoardCardViewModel, index: number): number {
    return this.cardRotation(card, index) * MOBILE_HAND_ROTATION_FACTOR;
  }

  startPointerDrag(event: PointerEvent, card: BoardCardViewModel): void {
    if (event.pointerType === 'mouse' || !event.isPrimary || !this.isDraggable(card)) {
      return;
    }

    const captureTarget = event.currentTarget as HTMLElement;
    this.pointerDragState = {
      card,
      captureTarget,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      started: false,
      scrolling: false
    };
    this.pointerClientX.set(event.clientX);
    this.pointerClientY.set(event.clientY);
    this.emitCardHover(card, this.cards().indexOf(card), false);
    captureTarget.setPointerCapture(event.pointerId);
  }

  movePointerDrag(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    const offsetX = event.clientX - dragState.startX;
    const offsetY = event.clientY - dragState.startY;
    if (dragState.scrolling) {
      return;
    }
    if (!dragState.started && Math.hypot(offsetX, offsetY) < POINTER_DRAG_THRESHOLD_PX) {
      return;
    }

    if (
      !dragState.started &&
      Math.abs(offsetX) > Math.abs(offsetY) &&
      (event.currentTarget as HTMLElement).closest('.mobile-hand-drawer__grid')
    ) {
      dragState.scrolling = true;
      return;
    }

    event.preventDefault();
    if (!dragState.started) {
      dragState.started = true;
      const cardId = this.cardReferenceId(dragState.card);
      this.draggingCardId.set(cardId);
      this.pointerDraggingCardId.set(cardId);
      this.emitCardHover(dragState.card, this.cards().indexOf(dragState.card), false);
      this.cardDragStarted.emit({ card: dragState.card, source: 'current-hand' });
    }

    this.pointerDragX.set(offsetX);
    this.pointerDragY.set(offsetY);
    this.pointerClientX.set(event.clientX);
    this.pointerClientY.set(event.clientY);
  }

  finishPointerDrag(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    if (dragState.scrolling) {
      this.suppressNextClick = true;
      setTimeout(() => {
        this.suppressNextClick = false;
      });
      this.resetPointerDrag(event);
      return;
    }

    const pointerTarget = dragState.started
      ? this.findPointerDropTarget(event.clientX, event.clientY)
      : null;

    if (dragState.started) {
      event.preventDefault();
      this.suppressNextClick = true;
      setTimeout(() => {
        this.suppressNextClick = false;
      });
      this.emitPointerDrop(pointerTarget, dragState.card);
      this.cardDragEnded.emit();
    }

    this.resetPointerDrag(event);
  }

  cancelPointerDrag(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    if (dragState.started) {
      this.cardDragEnded.emit();
    }
    this.resetPointerDrag(event);
  }

  @HostListener('window:pointerup', ['$event'])
  @HostListener('window:pointercancel', ['$event'])
  resetPointerDragFromWindow(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    if (dragState.started) {
      this.cardDragEnded.emit();
    }
    this.resetPointerDrag();
  }

  @HostListener('window:blur')
  @HostListener('document:visibilitychange')
  resetPointerDragAfterInterruption(): void {
    const dragState = this.pointerDragState;
    if (!dragState) {
      return;
    }

    if (dragState.started) {
      this.cardDragEnded.emit();
    }
    this.resetPointerDrag();
  }

  emitCardHover(card: BoardCardViewModel, index: number, hovered: boolean): void {
    if (this.visualOwner() !== 'SELF' || (hovered && this.touchOnlyPointer.matches)) {
      return;
    }

    this.cardHoverChanged.emit({
      zone: 'HAND',
      owner: this.visualOwner(),
      visualIndex: index,
      cardInstanceId: null,
      hovered
    });
  }

  private visibleCardCount(): number {
    return this.draggingOutsideHand() && this.draggingCardId()
      ? Math.max(this.cards().length - 1, 0)
      : this.cards().length;
  }

  private cardSpacing(): number {
    const cardCount = this.visibleCardCount();
    const baseSpacing = this.size() === 'large' ? LARGE_HAND_SPACING : COMPACT_HAND_SPACING;
    const minimumSpacing = this.size() === 'large' ? LARGE_HAND_MIN_SPACING : COMPACT_HAND_MIN_SPACING;

    if (cardCount <= 7) {
      return baseSpacing;
    }

    return Math.max(minimumSpacing, baseSpacing - (cardCount - 7) * 2.5);
  }

  private cardVisualIndex(card: BoardCardViewModel, index: number): number {
    const draggedCardId = this.draggingCardId();

    if (!this.draggingOutsideHand() || !draggedCardId || draggedCardId === this.cardReferenceId(card)) {
      return index;
    }

    const draggedCardIndex = this.cards().findIndex(
      (handCard) => this.cardReferenceId(handCard) === draggedCardId
    );

    return draggedCardIndex >= 0 && index > draggedCardIndex ? index - 1 : index;
  }

  private hoverKey(card: BoardCardViewModel, index: number): string | null {
    return cardHoverKey({
      owner: this.visualOwner(),
      zone: 'HAND',
      visualIndex: index,
      cardInstanceId: null
    });
  }

  private emitPointerDrop(target: HTMLElement | null, card: BoardCardViewModel): void {
    const dropZone = target?.dataset['dropZone'];
    if (dropZone === 'active') {
      this.mobileDrawerOpen.set(false);
      this.cardPointerDropped.emit({
        cardId: this.cardReferenceId(card),
        source: 'current-hand',
        target: 'active'
      });
      return;
    }

    const slotId = target?.dataset['dropSlotId'];
    if (dropZone === 'bench' && slotId) {
      this.mobileDrawerOpen.set(false);
      this.cardPointerDropped.emit({
        cardId: this.cardReferenceId(card),
        source: 'current-hand',
        target: 'bench',
        slotId
      });
      return;
    }

    if (dropZone === 'pokemon-tool-target') {
      const pokemonInPlayId = target?.dataset['pokemonInPlayId'];
      if (!pokemonInPlayId) {
        return;
      }

      this.mobileDrawerOpen.set(false);
      this.cardPointerDropped.emit({
        cardId: this.cardReferenceId(card),
        source: 'current-hand',
        target: 'pokemon-tool-target',
        pokemonInPlayId
      });
      return;
    }

    if (dropZone === 'stadium-area') {
      this.mobileDrawerOpen.set(false);
      this.cardPointerDropped.emit({
        cardId: this.cardReferenceId(card),
        source: 'current-hand',
        target: 'stadium-area'
      });
      return;
    }

    if (dropZone === 'trainer-play-area') {
      this.mobileDrawerOpen.set(false);
      this.cardPointerDropped.emit({
        cardId: this.cardReferenceId(card),
        source: 'current-hand',
        target: 'trainer-play-area'
      });
    }
  }

  private findPointerDropTarget(clientX: number, clientY: number): HTMLElement | null {
    for (const element of document.elementsFromPoint(clientX, clientY)) {
      const htmlElement = element as HTMLElement;
      if (htmlElement.closest('.mobile-hand-drawer, .mobile-card-drag-preview')) {
        continue;
      }

      const dropTarget = htmlElement.closest<HTMLElement>('[data-drop-zone]');
      if (dropTarget) {
        return dropTarget;
      }
    }

    return null;
  }

  private resetPointerDrag(event?: PointerEvent): void {
    const dragState = this.pointerDragState;
    const pointerId = event?.pointerId ?? dragState?.pointerId;
    if (dragState && pointerId !== undefined && dragState.captureTarget.hasPointerCapture(pointerId)) {
      dragState.captureTarget.releasePointerCapture(pointerId);
    }
    this.pointerDragState = null;
    this.draggingCardId.set(null);
    this.draggingOutsideHand.set(false);
    this.pointerDraggingCardId.set(null);
    this.pointerDragX.set(0);
    this.pointerDragY.set(0);
    this.pointerClientX.set(0);
    this.pointerClientY.set(0);
  }

  requestPrimaryAction(): void {
    if (this.setupPhase()) {
      if (this.setupSubmitEnabled() && !this.setupSubmitted()) {
        this.setupSubmitRequested.emit();
      }
      return;
    }

    if (this.endTurnEnabled()) {
      this.endTurnRequested.emit();
    }
  }

  clearSetupBench(): void {
    if (
      this.setupPhase() &&
      !this.setupSubmitted() &&
      !this.setupBusy() &&
      this.setupBenchCardInstanceIds().length > 0
    ) {
      this.setupBenchClearRequested.emit();
    }
  }

}
