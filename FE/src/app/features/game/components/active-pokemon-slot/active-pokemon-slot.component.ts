import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
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
import { BoardAnimationService } from '../../services/board-animation.service';
import { DAMAGE_PREVENTION_SHIELD_EFFECT_TYPE } from '../../domain/cards/board-pokemon-view-model.interface';
import { MUSCLE_BAND_EXTERNAL_ID, HARD_CHARM_EXTERNAL_ID } from '../../domain/cards/pokemon-tool-ids.constants';

export type ActivePokemonSlotOrientation = 'normal' | 'inverted';
export type ActivePokemonDropZone = 'active' | 'pokemon-tool-target';
export interface ActivePokemonCardDrop {
  cardId: string;
  source: string;
}

interface AttachedEnergySummary {
  key: string;
  label: string;
  imageUrl: string | null;
  count: number;
}

@Component({
  selector: 'app-active-pokemon-slot',
  templateUrl: './active-pokemon-slot.component.html',
  styleUrl: './active-pokemon-slot.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusOverlayComponent, HardenShieldBadgeComponent, PokemonToolBadgeComponent]
})
export class ActivePokemonSlotComponent {
  private readonly languageService = inject(LanguageService);
  private readonly boardAnimationService = inject(BoardAnimationService);

  readonly slot = input.required<BoardSlotViewModel>();
  readonly orientation = input<ActivePokemonSlotOrientation>('normal');
  readonly dropEnabled = input(false);
  readonly dropZone = input<ActivePokemonDropZone>('active');
  readonly validDropCardIds = input<string[]>([]);
  readonly selectedPokemonInPlayId = input<string | null>(null);
  readonly selectablePokemonInPlayIds = input<string[]>([]);
  readonly animationAnchorId = input<string | null>(null);
  readonly visualOwner = input<BoardVisualOwner>('SELF');
  readonly remoteHoveredCardKey = input<string | null>(null);
  readonly cardDropped = output<ActivePokemonCardDrop>();
  readonly pokemonSelected = output<string>();
  readonly cardInspected = output<BoardCardViewModel>();
  readonly cardHoverChanged = output<CardHoverVisualTarget>();
  readonly dragOverActive = signal(false);
  readonly attachedEnergySummary = computed<AttachedEnergySummary[]>(() => {
    const groupedEnergies = new Map<string, AttachedEnergySummary>();

    for (const energy of this.slot().pokemon?.attachedEnergyCards ?? []) {
      const key = energy.cardId ?? energy.externalId ?? energy.label;
      const currentSummary = groupedEnergies.get(key);
      if (currentSummary) {
        currentSummary.count += 1;
        continue;
      }

      groupedEnergies.set(key, {
        key,
        label: energy.pokemonType ?? energy.label,
        imageUrl: energy.imageSmallUrl ?? energy.imageLargeUrl ?? null,
        count: 1
      });
    }

    return [...groupedEnergies.values()];
  });
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly isSelected = () => this.selectedPokemonInPlayId() === this.slot().pokemon?.id;
  readonly isSelectable = () => {
    const pokemonId = this.slot().pokemon?.id;
    return typeof pokemonId === 'string' && this.selectablePokemonInPlayIds().includes(pokemonId);
  };
  readonly isDropAvailable = () => this.dropEnabled();
  readonly isCardInspectable = () => {
    const card = this.slot().card;
    return Boolean(card && !card.faceDown && card.visibility === 'visible');
  };
  readonly isRemoteHovered = () => {
    const hoverKey = this.hoverKey();
    return hoverKey !== null && this.remoteHoveredCardKey() === hoverKey;
  };
  /**
   * Whether the active Pokemon currently has a backend-reported damage-prevention shield
   * (Harden). This never recomputes the rule itself — it only checks for the presence of the
   * effect entry the backend already decided to expose on the snapshot.
   */
  readonly hasHardenShield = () =>
    (this.slot().pokemon?.visualEffects ?? []).some((effect) => effect.type === DAMAGE_PREVENTION_SHIELD_EFFECT_TYPE);
  readonly hasHardCharm = () =>
    (this.slot().pokemon?.attachedTrainerCards ?? []).some(t => t.externalId === HARD_CHARM_EXTERNAL_ID);
  readonly shieldActive = () => this.hasHardenShield() || this.hasHardCharm();
  readonly shieldValue = (): number | null => this.hasHardCharm() ? 20 : null;
  readonly attachedToolBadge = computed<{ type: PokemonToolBadgeType; value: number } | null>(() => {
    const tools = this.slot().pokemon?.attachedTrainerCards ?? [];
    if (tools.some(t => t.externalId === MUSCLE_BAND_EXTERNAL_ID)) return { type: 'attack', value: 20 };
    return null;
  });
  readonly remainingHp = () => {
    const card = this.slot().card;
    if (!card || card.faceDown || !card.hp) {
      return null;
    }

    const damageCounters = this.slot().pokemon?.damageCounters ?? 0;
    const liveRemaining = Math.max(card.hp - damageCounters * 10, 0);

    const pokemonId = this.slot().pokemon?.id;
    if (
      pokemonId &&
      this.previousRemainingHp !== null &&
      this.boardAnimationService.heldDamagePokemonIds().has(pokemonId)
    ) {
      return this.previousRemainingHp;
    }

    return liveRemaining;
  };
  readonly damagePopups = signal<{ id: number; amount: number; kind: 'damage' | 'heal' }[]>([]);
  private previousRemainingHp: number | null = null;
  private nextDamagePopupId = 0;

  constructor() {
    effect(() => {
      const remaining = this.remainingHp();
      if (remaining === null) {
        this.previousRemainingHp = null;
        return;
      }

      if (this.previousRemainingHp !== null && remaining !== this.previousRemainingHp) {
        const amount = Math.abs(remaining - this.previousRemainingHp);
        const kind: 'damage' | 'heal' = remaining < this.previousRemainingHp ? 'damage' : 'heal';
        const id = ++this.nextDamagePopupId;
        this.damagePopups.update((popups) => [...popups, { id, amount, kind }]);
        setTimeout(() => {
          this.damagePopups.update((popups) => popups.filter((popup) => popup.id !== id));
        }, 1200);
      }

      this.previousRemainingHp = remaining;
    });
  }

  selectPokemon(): void {
    const pokemonId = this.slot().pokemon?.id;
    if (pokemonId && this.isSelectable()) {
      this.pokemonSelected.emit(pokemonId);
      return;
    }

    const card = this.slot().card;
    if (card && this.isCardInspectable()) {
      this.cardInspected.emit(card);
    }
  }

  handleDragOver(event: DragEvent): void {
    if (!this.canAcceptDragEvent()) {
      return;
    }

    event.preventDefault();
    this.dragOverActive.set(true);

    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  handleDragLeave(): void {
    this.dragOverActive.set(false);
  }

  handleDrop(event: DragEvent): void {
    if (!this.canAcceptDragEvent()) {
      this.dragOverActive.set(false);
      return;
    }

    event.preventDefault();
    this.dragOverActive.set(false);

    const cardId = event.dataTransfer?.getData('application/x-pokemon-card-id');
    const source = event.dataTransfer?.getData('application/x-pokemon-card-source');

    if (!cardId || !source || !this.canAcceptDroppedCard(cardId)) {
      return;
    }

    this.cardDropped.emit({ cardId, source });
  }

  emitCardHover(hovered: boolean): void {
    const card = this.slot().card;
    if (this.visualOwner() !== 'SELF' || !card) {
      return;
    }

    this.cardHoverChanged.emit({
      zone: 'ACTIVE',
      owner: this.visualOwner(),
      visualIndex: 0,
      cardInstanceId: card.faceDown ? null : card.cardInstanceId ?? null,
      hovered
    });
  }

  private hoverKey(): string | null {
    const card = this.slot().card;
    if (!card) {
      return null;
    }

    return cardHoverKey({
      owner: this.visualOwner(),
      zone: 'ACTIVE',
      visualIndex: 0,
      cardInstanceId: card.faceDown ? null : card.cardInstanceId ?? null
    });
  }

  private canAcceptDragEvent(): boolean {
    return this.isDropAvailable();
  }

  private canAcceptDroppedCard(cardId: string): boolean {
    const validDropCardIds = this.validDropCardIds();
    return validDropCardIds.length === 0 || validDropCardIds.includes(cardId);
  }

  attachedEnergyCards(): BoardCardViewModel[] {
    return this.slot().pokemon?.attachedEnergyCards ?? [];
  }

  pokemonInPlayId(): string | null {
    return this.slot().pokemon?.id ?? null;
  }
}
