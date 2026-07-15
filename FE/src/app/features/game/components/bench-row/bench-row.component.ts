import { ChangeDetectionStrategy, Component, HostListener, effect, inject, input, output, signal } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardVisualOwner } from '../../../../core/models/interfaces/game/game-visual-event.interface';
import { BoardCardViewModel, BoardSlotViewModel } from '../../domain/board/board-game-view-model.interface';
import {
  CardHoverVisualTarget,
  cardHoverKey
} from '../../domain/cards/card-hover-visual-target.interface';
import { StatusOverlayComponent } from '../status-overlay/status-overlay.component';
import { HardenShieldBadgeComponent } from '../harden-shield-badge/harden-shield-badge.component';
import { PokemonToolBadgeComponent, PokemonToolBadgeType } from '../pokemon-tool-badge/pokemon-tool-badge.component';
import { DAMAGE_PREVENTION_SHIELD_EFFECT_TYPE } from '../../domain/cards/board-pokemon-view-model.interface';
import { MUSCLE_BAND_EXTERNAL_ID, HARD_CHARM_EXTERNAL_ID } from '../../domain/cards/pokemon-tool-ids.constants';

export type BenchRowOrientation = 'normal' | 'inverted';
type BenchRowSize = 'normal' | 'compact';
interface EnergyStackViewModel {
  key: string;
  count: number;
  icon: string;
}
export interface BenchCardDrop {
  cardId: string;
  source: string;
  slotId: string;
}

@Component({
  selector: 'app-bench-row',
  templateUrl: './bench-row.component.html',
  styleUrl: './bench-row.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusOverlayComponent, HardenShieldBadgeComponent, PokemonToolBadgeComponent]
})
export class BenchRowComponent {
  private readonly languageService = inject(LanguageService);

  readonly label = input.required<string>();
  readonly slots = input.required<BoardSlotViewModel[]>();
  readonly orientation = input<BenchRowOrientation>('normal');
  readonly size = input<BenchRowSize>('normal');
  readonly dropEnabled = input(false);
  readonly validDropSlotIds = input<string[]>([]);
  readonly pokemonToolDropSlotIds = input<string[]>([]);
  readonly selectedPokemonInPlayId = input<string | null>(null);
  readonly selectablePokemonInPlayIds = input<string[]>([]);
  readonly draggablePokemonInPlayIds = input<string[]>([]);
  readonly animationAnchorPrefix = input<string | null>(null);
  readonly visualOwner = input<BoardVisualOwner>('SELF');
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly cardDropped = output<BenchCardDrop>();
  readonly benchPokemonDragStarted = output<string>();
  readonly benchPokemonDragEnded = output<void>();
  readonly pokemonSelected = output<string>();
  readonly cardInspected = output<{ card: BoardCardViewModel; slot: BoardSlotViewModel }>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
  readonly benchPokemonPointerDropped = output<{ cardId: string; source: string }>();
  readonly dragOverSlotId = signal<string | null>(null);

  readonly pointerDraggingPokemonId = signal<string | null>(null);
  readonly pointerDragX = signal(0);
  readonly pointerDragY = signal(0);
  readonly pointerClientX = signal(0);
  readonly pointerClientY = signal(0);
  private pointerDragState: {
    pokemonId: string;
    captureTarget: HTMLElement;
    pointerId: number;
    startX: number;
    startY: number;
    started: boolean;
  } | null = null;
  private suppressNextClick = false;

  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly damagePopupsBySlot = signal<Record<string, { id: number; amount: number; kind: 'damage' | 'heal' }[]>>({});
  private readonly previousRemainingHpByPokemonId = new Map<string, number>();
  private nextPopupId = 0;

  constructor() {
    effect(() => {
      const slots = this.slots();
      const seenPokemonIds = new Set<string>();

      for (const slot of slots) {
        const pokemonId = slot.pokemon?.id;
        const remaining = this.remainingHp(slot);
        if (!pokemonId || remaining === null) {
          continue;
        }

        seenPokemonIds.add(pokemonId);
        const previous = this.previousRemainingHpByPokemonId.get(pokemonId);
        if (previous !== undefined && remaining !== previous) {
          const amount = Math.abs(remaining - previous);
          const kind: 'damage' | 'heal' = remaining < previous ? 'damage' : 'heal';
          const id = ++this.nextPopupId;
          this.damagePopupsBySlot.update((bySlot) => ({
            ...bySlot,
            [pokemonId]: [...(bySlot[pokemonId] ?? []), { id, amount, kind }]
          }));
          setTimeout(() => {
            this.damagePopupsBySlot.update((bySlot) => ({
              ...bySlot,
              [pokemonId]: (bySlot[pokemonId] ?? []).filter((popup) => popup.id !== id)
            }));
          }, 1200);
        }

        this.previousRemainingHpByPokemonId.set(pokemonId, remaining);
      }

      for (const pokemonId of Array.from(this.previousRemainingHpByPokemonId.keys())) {
        if (!seenPokemonIds.has(pokemonId)) {
          this.previousRemainingHpByPokemonId.delete(pokemonId);
        }
      }
    });

    effect(() => {
      const draggableIds = this.draggablePokemonInPlayIds();
      const currentDraggedId = this.pointerDraggingPokemonId();
      if (currentDraggedId && !draggableIds.includes(currentDraggedId)) {
        if (this.pointerDragState?.started) {
          this.benchPokemonDragEnded.emit();
        }
        this.resetPointerDrag();
      }
    }, { allowSignalWrites: true });
  }

  popupsFor(slot: BoardSlotViewModel): { id: number; amount: number; kind: 'damage' | 'heal' }[] {
    const pokemonId = slot.pokemon?.id;
    return pokemonId ? this.damagePopupsBySlot()[pokemonId] ?? [] : [];
  }

  isPokemonSelectable(slot: BoardSlotViewModel): boolean {
    const pokemonId = slot.pokemon?.id;
    return typeof pokemonId === 'string' && this.selectablePokemonInPlayIds().includes(pokemonId);
  }

  isPokemonSelected(slot: BoardSlotViewModel): boolean {
    return this.selectedPokemonInPlayId() === slot.pokemon?.id;
  }

  animationAnchorForSlot(index: number): string | null {
    const prefix = this.animationAnchorPrefix();
    return prefix ? `${prefix}-${index}` : null;
  }

  isCardInspectable(slot: BoardSlotViewModel): boolean {
    const card = slot.card;
    return Boolean(card && !card.faceDown && card.visibility === 'visible');
  }

  isRemoteHovered(slot: BoardSlotViewModel, index: number): boolean {
    const hoverKey = hoverKeyForSlot(this.visualOwner(), slot, index);
    return hoverKey !== null && this.remoteHoveredCardKey() === hoverKey;
  }

  isBenchPokemonDraggable(slot: BoardSlotViewModel): boolean {
    const pokemonId = slot.pokemon?.id;
    return typeof pokemonId === 'string' && this.draggablePokemonInPlayIds().includes(pokemonId);
  }

  selectPokemon(slot: BoardSlotViewModel): void {
    if (this.suppressNextClick) {
      this.suppressNextClick = false;
      return;
    }

    const pokemonId = slot.pokemon?.id;
    if (pokemonId && this.isPokemonSelectable(slot)) {
      this.pokemonSelected.emit(pokemonId);
      return;
    }

    const card = slot.card;
    if (card && this.isCardInspectable(slot)) {
      this.cardInspected.emit({ card, slot });
    }
  }

  canDropOnSlot(slot: BoardSlotViewModel): boolean {
    const validDropSlotIds = this.validDropSlotIds();
    if (validDropSlotIds.length > 0) {
      return this.dropEnabled() && validDropSlotIds.includes(slot.id);
    }

    return this.dropEnabled() && !slot.occupied;
  }

  isPokemonToolDropTarget(slot: BoardSlotViewModel): boolean {
    return Boolean(slot.pokemon?.id && this.dropEnabled() && this.pokemonToolDropSlotIds().includes(slot.id));
  }

  dropZoneForSlot(slot: BoardSlotViewModel): 'bench' | 'pokemon-tool-target' | null {
    if (!this.canDropOnSlot(slot)) {
      return null;
    }

    return this.isPokemonToolDropTarget(slot) ? 'pokemon-tool-target' : 'bench';
  }

  pokemonInPlayIdForDrop(slot: BoardSlotViewModel): string | null {
    return this.isPokemonToolDropTarget(slot) ? slot.pokemon?.id ?? null : null;
  }

  handleDragOver(event: DragEvent, slot: BoardSlotViewModel): void {
    if (!this.canDropOnSlot(slot)) {
      return;
    }

    event.preventDefault();
    this.dragOverSlotId.set(slot.id);

    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  handleDragLeave(slot: BoardSlotViewModel): void {
    if (this.dragOverSlotId() === slot.id) {
      this.dragOverSlotId.set(null);
    }
  }

  handleDrop(event: DragEvent, slot: BoardSlotViewModel): void {
    if (!this.canDropOnSlot(slot)) {
      this.dragOverSlotId.set(null);
      return;
    }

    event.preventDefault();
    this.dragOverSlotId.set(null);

    const cardId = event.dataTransfer?.getData('application/x-pokemon-card-id');
    const source = event.dataTransfer?.getData('application/x-pokemon-card-source');

    if (!cardId || !source) {
      return;
    }

    this.cardDropped.emit({ cardId, source, slotId: slot.id });
  }

  handleBenchPokemonDragStart(event: DragEvent, slot: BoardSlotViewModel): void {
    const pokemonId = slot.pokemon?.id;
    if (!pokemonId || !this.isBenchPokemonDraggable(slot)) {
      event.preventDefault();
      return;
    }

    event.dataTransfer?.setData('application/x-pokemon-card-id', pokemonId);
    event.dataTransfer?.setData('application/x-pokemon-card-source', 'local-bench');
    event.dataTransfer?.setData('text/plain', pokemonId);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }

    this.benchPokemonDragStarted.emit(pokemonId);
    this.emitCardHover(slot, this.slots().indexOf(slot), false);
  }

  handleBenchPokemonDragEnd(): void {
    this.benchPokemonDragEnded.emit();
  }

  emitCardHover(slot: BoardSlotViewModel, index: number, hovered: boolean): void {
    if (this.visualOwner() !== 'SELF' || !slot.card) {
      return;
    }

    this.cardHoverChanged.emit({
      zone: 'BENCH',
      owner: this.visualOwner(),
      visualIndex: index,
      cardInstanceId: slot.card.faceDown ? null : slot.card.cardInstanceId ?? null,
      hovered
    });
  }

  remainingHp(slot: BoardSlotViewModel): number | null {
    const card = slot.card;
    if (!card || card.faceDown || !card.hp) {
      return null;
    }

    const damageCounters = slot.pokemon?.damageCounters ?? 0;
    return Math.max(card.hp - damageCounters * 10, 0);
  }

  energyStacks(slot: BoardSlotViewModel): EnergyStackViewModel[] {
    const stacks = new Map<string, EnergyStackViewModel>();

    for (const card of slot.pokemon?.attachedEnergyCards ?? []) {
      const key = energyKey(card.label);
      const currentStack = stacks.get(key);
      if (currentStack) {
        currentStack.count += 1;
      } else {
        stacks.set(key, { key, count: 1, icon: energyIcon(card.label) });
      }
    }

    return Array.from(stacks.values());
  }

  specialConditionsForSlot(slot: BoardSlotViewModel) {
    return slot.pokemon?.specialConditions ?? [];
  }

  /**
   * Whether the bench Pokemon in `slot` currently has a backend-reported damage-prevention
   * shield (Harden). Purely cosmetic: never recomputes the rule, only checks the presence of
   * the effect entry the backend already decided to expose on the snapshot.
   */
  hasHardenShield(slot: BoardSlotViewModel): boolean {
    return (slot.pokemon?.visualEffects ?? []).some(
      (effect) => effect.type === DAMAGE_PREVENTION_SHIELD_EFFECT_TYPE
    );
  }

  private hasHardCharm(slot: BoardSlotViewModel): boolean {
    return (slot.pokemon?.attachedTrainerCards ?? []).some(t => t.externalId === HARD_CHARM_EXTERNAL_ID);
  }

  showShieldBadge(slot: BoardSlotViewModel): boolean {
    return this.hasHardenShield(slot) || this.hasHardCharm(slot);
  }

  shieldValueForSlot(slot: BoardSlotViewModel): number | null {
    return this.hasHardCharm(slot) ? 20 : null;
  }

  toolBadgeForSlot(slot: BoardSlotViewModel): { type: PokemonToolBadgeType; value: number } | null {
    const tools = slot.pokemon?.attachedTrainerCards ?? [];
    if (tools.some(t => t.externalId === MUSCLE_BAND_EXTERNAL_ID)) return { type: 'attack', value: 20 };
    return null;
  }

  startPointerDrag(event: PointerEvent, slot: BoardSlotViewModel): void {
    if (event.pointerType === 'mouse' || !event.isPrimary || !this.isBenchPokemonDraggable(slot)) {
      return;
    }
    const pokemonId = slot.pokemon?.id;
    if (!pokemonId) {
      return;
    }

    const captureTarget = event.currentTarget as HTMLElement;
    this.pointerDragState = {
      pokemonId,
      captureTarget,
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      started: false
    };
    this.pointerClientX.set(event.clientX);
    this.pointerClientY.set(event.clientY);
    
    const index = this.slots().indexOf(slot);
    this.emitCardHover(slot, index, false);
    
    try {
      captureTarget.setPointerCapture(event.pointerId);
    } catch (e) {
      // Ignore errors if pointer capture setup fails
    }
  }

  movePointerDrag(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    if (event.pointerType === 'mouse') {
      return;
    }

    const offsetX = event.clientX - dragState.startX;
    const offsetY = event.clientY - dragState.startY;

    // Movement threshold of 6px to avoid conflicts with tap/click
    if (!dragState.started && Math.hypot(offsetX, offsetY) < 6) {
      return;
    }

    event.preventDefault();
    if (!dragState.started) {
      dragState.started = true;
      this.pointerDraggingPokemonId.set(dragState.pokemonId);
      this.benchPokemonDragStarted.emit(dragState.pokemonId);
      
      const slot = this.slots().find(s => s.pokemon?.id === dragState.pokemonId);
      if (slot) {
        this.emitCardHover(slot, this.slots().indexOf(slot), false);
      }
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

    if (event.pointerType === 'mouse') {
      return;
    }

    if (dragState.started) {
      event.preventDefault();
      this.suppressNextClick = true;
      setTimeout(() => {
        this.suppressNextClick = false;
      });

      const pointerTarget = this.findPointerDropTarget(event.clientX, event.clientY);
      this.emitPointerDrop(pointerTarget, dragState.pokemonId);
      this.benchPokemonDragEnded.emit();
    }

    this.resetPointerDrag(event);
  }

  cancelPointerDrag(event: PointerEvent): void {
    const dragState = this.pointerDragState;
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    if (event.pointerType === 'mouse') {
      return;
    }

    if (dragState.started) {
      this.benchPokemonDragEnded.emit();
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
      this.benchPokemonDragEnded.emit();
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
      this.benchPokemonDragEnded.emit();
    }
    this.resetPointerDrag();
  }

  private findPointerDropTarget(clientX: number, clientY: number): HTMLElement | null {
    for (const element of document.elementsFromPoint(clientX, clientY)) {
      const dropTarget = (element as HTMLElement).closest<HTMLElement>('[data-drop-zone="active"]');
      if (dropTarget) {
        return dropTarget;
      }
    }
    return null;
  }

  private emitPointerDrop(target: HTMLElement | null, pokemonId: string): void {
    const dropZone = target?.dataset['dropZone'];
    if (dropZone === 'active') {
      this.benchPokemonPointerDropped.emit({
        cardId: pokemonId,
        source: 'local-bench'
      });
    }
  }

  private resetPointerDrag(event?: PointerEvent): void {
    const dragState = this.pointerDragState;
    const pointerId = event?.pointerId ?? dragState?.pointerId;
    if (dragState && pointerId !== undefined && dragState.captureTarget.hasPointerCapture(pointerId)) {
      try {
        dragState.captureTarget.releasePointerCapture(pointerId);
      } catch (e) {
        // Ignore capture release errors
      }
    }
    this.pointerDragState = null;
    this.pointerDraggingPokemonId.set(null);
    this.pointerDragX.set(0);
    this.pointerDragY.set(0);
    this.pointerClientX.set(0);
    this.pointerClientY.set(0);
  }
}

function hoverKeyForSlot(
  owner: BoardVisualOwner,
  slot: BoardSlotViewModel,
  index: number
): string | null {
  if (!slot.card) {
    return null;
  }

  return cardHoverKey({
    owner,
    zone: 'BENCH',
    visualIndex: index,
    cardInstanceId: slot.card.faceDown ? null : slot.card.cardInstanceId ?? null
  });
}

function energyKey(label: string): string {
  return label.toLowerCase().replace(/[^a-z0-9]+/g, '-');
}

function energyIcon(label: string): string {
  const normalizedLabel = label.toLowerCase();
  if (normalizedLabel.includes('fire')) {
    return '🔥';
  }
  if (normalizedLabel.includes('water')) {
    return '💧';
  }
  if (normalizedLabel.includes('lightning') || normalizedLabel.includes('electric')) {
    return '⚡';
  }
  if (normalizedLabel.includes('grass')) {
    return '🌿';
  }
  if (normalizedLabel.includes('psychic')) {
    return '🔮';
  }
  if (normalizedLabel.includes('fighting')) {
    return '✊';
  }
  if (normalizedLabel.includes('dark')) {
    return '🌑';
  }
  if (normalizedLabel.includes('metal')) {
    return '⚙';
  }
  return '⭐';
}
