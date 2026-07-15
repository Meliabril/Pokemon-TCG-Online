import { ChangeDetectionStrategy, Component, HostListener, computed, inject, input, output, signal } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { BoardCardViewModel } from '../../domain/board/board-game-view-model.interface';
import {
  EvosodaEvolutionOption,
  GreatBallPokemonOption,
  ProfessorLetterEnergyOption
} from '../../domain/trainers/trainer-preview.interface';

const TRAINER_CATEGORY_MAP: Record<string, 'item' | 'supporter' | 'stadium' | 'tool' | 'ace_spec'> = {
  ITEM_TRAINER:        'item',
  ACE_SPEC_TRAINER:    'ace_spec',
  SUPPORTER_TRAINER:   'supporter',
  STADIUM_TRAINER:     'stadium',
  POKEMON_TOOL_TRAINER:'tool'
};

const BENCH_TARGET_EXTERNAL_IDS = new Set(['xy1-128', 'xy1-116', 'xy1-115']);
const MAX_REVIVE_EXTERNAL_ID = 'xy1-120';
const EVOSODA_EXTERNAL_ID = 'xy1-116';
const PROFESSOR_LETTER_EXTERNAL_ID = 'xy1-123';

@Component({
  selector: 'app-trainer-selection-modal',
  templateUrl: './trainer-selection-modal.component.html',
  styleUrl: './trainer-selection-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TrainerSelectionModalComponent {
  private readonly languageService = inject(LanguageService);
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly isOpen = input(false);
  readonly card = input<BoardCardViewModel | null>(null);
  readonly actionPending = input(false);
  readonly trainerToolTargetOptions = input<{ pokemonInPlayId: string; label: string }[]>([]);
  readonly trainerBenchTargetOptions = input<{ pokemonInPlayId: string; label: string }[]>([]);
  readonly trainerDiscardPokemonOptions = input<{
    cardInstanceId: string;
    label: string;
    imageSmallUrl?: string | null;
    imageLargeUrl?: string | null;
  }[]>([]);
  readonly trainerDeckPokemonOptions = input<GreatBallPokemonOption[]>([]);
  readonly greatBallCardsLookedAt = input(0);
  readonly greatBallPreviewLoading = input(false);
  readonly greatBallPreviewReady = input(false);
  readonly greatBallPreviewError = input<string | null>(null);
  readonly evosodaEvolutionOptions = input<EvosodaEvolutionOption[]>([]);
  readonly evosodaPreviewLoading = input(false);
  readonly evosodaPreviewError = input<string | null>(null);
  readonly professorLetterEnergyOptions = input<ProfessorLetterEnergyOption[]>([]);
  readonly professorLetterMaxSelectable = input(2);
  readonly professorLetterPreviewLoading = input(false);
  readonly professorLetterPreviewError = input<string | null>(null);

  readonly confirmed = output<{
    targetPokemonInPlayId?: string;
    targetCardInstanceId?: string;
    selectedEvolutionExternalId?: string;
    selectedCardInstanceId?: string;
    selectedCardIds?: string[];
  }>();
  readonly cancelled = output<void>();

  readonly selectedTargetPokemonInPlayId = signal<string | null>(null);
  readonly selectedRevivePokemonInstanceId = signal<string | null>(null);
  readonly selectedDeckPokemonInstanceId = signal<string | null>(null);
  readonly selectedEvolutionExternalId = signal<string | null>(null);
  readonly selectedProfessorLetterCounts = signal<Record<string, number>>({});

  readonly trainerType = computed(() => {
    const cat = this.card()?.category;
    return cat ? (TRAINER_CATEGORY_MAP[cat] ?? null) : null;
  });

  readonly isToolTrainer = computed(() => this.trainerType() === 'tool');

  readonly requiresBenchTarget = computed(() => {
    if (this.isToolTrainer()) {
      return false;
    }
    return BENCH_TARGET_EXTERNAL_IDS.has(this.card()?.externalId ?? '');
  });

  readonly requiresReviveTarget = computed(() =>
    this.card()?.externalId === MAX_REVIVE_EXTERNAL_ID
  );

  readonly requiresDeckTarget = computed(() =>
    this.card()?.externalId === 'xy1-118'
  );

  readonly isEvosoda = computed(() => this.card()?.externalId === EVOSODA_EXTERNAL_ID);
  readonly isProfessorLetter = computed(() => this.card()?.externalId === PROFESSOR_LETTER_EXTERNAL_ID);

  readonly professorLetterSelectedTotal = computed(() =>
    Object.values(this.selectedProfessorLetterCounts()).reduce((total, count) => total + count, 0)
  );

  readonly compatibleEvosodaTargets = computed(() => {
    const selectedExternalId = this.selectedEvolutionExternalId();
    if (!selectedExternalId) {
      return [];
    }

    const evolution = this.evosodaEvolutionOptions().find(
      (option) => option.externalId === selectedExternalId
    );
    if (!evolution) {
      return [];
    }

    return this.trainerBenchTargetOptions().filter((target) =>
      evolution.validTargetPokemonInPlayIds.includes(target.pokemonInPlayId)
    );
  });

  readonly canConfirm = computed(() => {
    if (this.actionPending()) {
      return false;
    }
    if (this.isToolTrainer()) {
      return this.selectedTargetPokemonInPlayId() !== null;
    }
    if (this.requiresBenchTarget()) {
      if (this.isEvosoda()) {
        return this.selectedEvolutionExternalId() !== null && this.selectedTargetPokemonInPlayId() !== null;
      }
      return this.selectedTargetPokemonInPlayId() !== null;
    }
    if (this.requiresReviveTarget()) {
      return this.selectedRevivePokemonInstanceId() !== null;
    }
    if (this.requiresDeckTarget()) {
      if (!this.greatBallPreviewReady() || this.greatBallPreviewLoading() || this.greatBallPreviewError()) {
        return false;
      }
      return this.selectedDeckPokemonInstanceId() !== null || this.trainerDeckPokemonOptions().length === 0;
    }
    if (this.isProfessorLetter()) {
      if (this.professorLetterPreviewLoading() || this.professorLetterPreviewError()) {
        return false;
      }
      const selectedTotal = this.professorLetterSelectedTotal();
      return selectedTotal > 0 && selectedTotal <= this.professorLetterMaxSelectable();
    }
    return true;
  });

  readonly benchTargetLabel = computed(() => {
    const externalId = this.card()?.externalId;
    if (externalId === 'xy1-128') {
      return this.t('GAME.TRAINER.CHOOSE_HEAL_TARGET');
    }
    if (externalId === 'xy1-116') {
      return this.t('GAME.TRAINER.CHOOSE_EVOLVE_TARGET');
    }
    if (externalId === 'xy1-115') {
      return this.t('GAME.TRAINER.CHOOSE_CASSIUS_TARGET');
    }
    return this.t('GAME.TRAINER.CHOOSE_BENCH_TARGET');
  });

  readonly benchTargetEmptyLabel = computed(() => {
    const externalId = this.card()?.externalId;
    if (externalId === 'xy1-128') {
      return this.t('GAME.TRAINER.NO_HEAL_TARGETS');
    }
    if (externalId === 'xy1-116') {
      return this.t('GAME.TRAINER.NO_EVOLVE_TARGETS');
    }
    return this.t('GAME.TRAINER.NO_BENCH_TARGETS');
  });

  readonly confirmLabel = computed(() =>
    this.isToolTrainer()
      ? this.t('GAME.TRAINER.EQUIP')
      : this.t('GAME.TRAINER.PLAY')
  );

  readonly imageUrl = computed(() =>
    this.card()?.imageLargeUrl ?? this.card()?.imageSmallUrl ?? null
  );

  readonly displayName = computed(() => this.card()?.label ?? '');

  selectTarget(pokemonInPlayId: string): void {
    this.selectedTargetPokemonInPlayId.set(
      this.selectedTargetPokemonInPlayId() === pokemonInPlayId ? null : pokemonInPlayId
    );
  }

  selectEvolution(externalId: string): void {
    const nextExternalId = this.selectedEvolutionExternalId() === externalId ? null : externalId;
    this.selectedEvolutionExternalId.set(nextExternalId);

    const selectedEvolution = this.evosodaEvolutionOptions().find(
      (option) => option.externalId === nextExternalId
    );
    const validTargetIds = selectedEvolution?.validTargetPokemonInPlayIds ?? [];
    const currentTargetId = this.selectedTargetPokemonInPlayId();
    if (!currentTargetId || !validTargetIds.includes(currentTargetId)) {
      this.selectedTargetPokemonInPlayId.set(validTargetIds.length === 1 ? validTargetIds[0] : null);
    }
  }

  selectRevivePokemon(cardInstanceId: string): void {
    this.selectedRevivePokemonInstanceId.set(
      this.selectedRevivePokemonInstanceId() === cardInstanceId ? null : cardInstanceId
    );
  }

  selectDeckPokemon(cardInstanceId: string): void {
    this.selectedDeckPokemonInstanceId.set(
      this.selectedDeckPokemonInstanceId() === cardInstanceId ? null : cardInstanceId
    );
  }

  professorLetterQuantity(cardId: string): number {
    return this.selectedProfessorLetterCounts()[cardId] ?? 0;
  }

  incrementProfessorLetterEnergy(option: ProfessorLetterEnergyOption): void {
    const currentQuantity = this.professorLetterQuantity(option.cardId);
    if (
      currentQuantity >= option.availableInDeck ||
      this.professorLetterSelectedTotal() >= this.professorLetterMaxSelectable()
    ) {
      return;
    }
    this.selectedProfessorLetterCounts.update((counts) => ({
      ...counts,
      [option.cardId]: currentQuantity + 1
    }));
  }

  decrementProfessorLetterEnergy(cardId: string): void {
    const currentQuantity = this.professorLetterQuantity(cardId);
    if (currentQuantity <= 0) {
      return;
    }
    this.selectedProfessorLetterCounts.update((counts) => {
      const nextCounts = { ...counts };
      if (currentQuantity === 1) {
        delete nextCounts[cardId];
      } else {
        nextCounts[cardId] = currentQuantity - 1;
      }
      return nextCounts;
    });
  }

  professorLetterSelectedCardIds(): string[] {
    const selectedCardIds: string[] = [];
    for (const option of this.professorLetterEnergyOptions()) {
      const quantity = this.professorLetterQuantity(option.cardId);
      for (let index = 0; index < quantity; index++) {
        selectedCardIds.push(option.cardId);
      }
    }
    return selectedCardIds;
  }

  confirm(): void {
    if (!this.canConfirm()) {
      return;
    }
    const targetPokemonInPlayId = (this.isToolTrainer() || this.requiresBenchTarget())
      ? (this.selectedTargetPokemonInPlayId() ?? undefined)
      : undefined;
    const targetCardInstanceId = this.requiresReviveTarget()
      ? (this.selectedRevivePokemonInstanceId() ?? undefined)
      : this.requiresDeckTarget()
      ? (this.selectedDeckPokemonInstanceId() ?? undefined)
      : undefined;
    const selectedEvolutionExternalId = this.isEvosoda()
      ? (this.selectedEvolutionExternalId() ?? undefined)
      : undefined;
    const selectedCardIds = this.isProfessorLetter()
      ? this.professorLetterSelectedCardIds()
      : undefined;
    this.confirmed.emit({ targetPokemonInPlayId, targetCardInstanceId, selectedEvolutionExternalId, selectedCardIds });
    this.clearSelections();
  }

  cancel(): void {
    this.clearSelections();
    this.cancelled.emit();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen()) {
      this.cancel();
    }
  }

  private clearSelections(): void {
    this.selectedTargetPokemonInPlayId.set(null);
    this.selectedRevivePokemonInstanceId.set(null);
    this.selectedDeckPokemonInstanceId.set(null);
    this.selectedEvolutionExternalId.set(null);
    this.selectedProfessorLetterCounts.set({});
  }
}
