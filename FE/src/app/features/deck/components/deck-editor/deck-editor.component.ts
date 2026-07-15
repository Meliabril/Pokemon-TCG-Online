import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import {
  DeckTypeFilter,
  DeckTypeFilterOption,
  DeckValidationHint,
  DraftDeckItem
} from '../../models/deck-builder.types';

const DECK_DRAFT_QUANTITY_BADGE_BASE_CLASS =
  'inline-flex min-w-10 items-center justify-center rounded-full border px-2 py-1 text-xs font-extrabold';

const DECK_ROW_BASE_CLASS =
  'flex items-center gap-3 rounded-xl border p-2 transition-colors';

const DECK_QUANTITY_BADGE_TYPE_CLASSES: Record<string, string> = {
  Grass:
    'bg-lime-500 text-green-950 border-lime-200 shadow-[0_0_18px_rgba(132,204,22,0.55)]',
  Fire:
    'bg-orange-500 text-red-950 border-orange-200 shadow-[0_0_18px_rgba(249,115,22,0.55)]',
  Water:
    'bg-cyan-500 text-sky-950 border-cyan-100 shadow-[0_0_18px_rgba(34,211,238,0.55)]',
  Lightning:
    'bg-[#D9A441] text-yellow-950 border-[#e7c57a] shadow-[0_0_18px_rgba(217,164,65,0.35)]',
  Psychic:
    'bg-fuchsia-500 text-purple-950 border-fuchsia-100 shadow-[0_0_18px_rgba(217,70,239,0.55)]',
  Fighting:
    'bg-orange-700 text-orange-50 border-orange-300 shadow-[0_0_18px_rgba(194,65,12,0.55)]',
  Darkness:
    'bg-slate-800 text-slate-50 border-slate-400 shadow-[0_0_18px_rgba(100,116,139,0.50)]',
  Metal:
    'bg-zinc-400 text-zinc-950 border-zinc-100 shadow-[0_0_18px_rgba(161,161,170,0.50)]',
  Fairy:
    'bg-pink-400 text-pink-950 border-pink-100 shadow-[0_0_18px_rgba(244,114,182,0.55)]',
  Dragon:
    'bg-violet-500 text-indigo-950 border-violet-100 shadow-[0_0_18px_rgba(139,92,246,0.55)]',
  Colorless:
    'bg-stone-300 text-stone-950 border-stone-100 shadow-[0_0_18px_rgba(214,211,209,0.45)]'
};

const DEFAULT_DECK_QUANTITY_BADGE_TYPE_CLASS =
  'bg-blue-600 text-white border-blue-200 shadow-[0_0_18px_rgba(37,99,235,0.50)]';

const LIGHT_DECK_QUANTITY_BADGE_TYPE_CLASSES: Record<string, string> = {
  Grass: 'bg-lime-200 text-green-950 border-lime-300',
  Fire: 'bg-orange-200 text-red-950 border-orange-300',
  Water: 'bg-cyan-200 text-sky-950 border-cyan-300',
  Lightning: 'bg-[#f0d49d] text-yellow-950 border-[#d9a441]',
  Psychic: 'bg-fuchsia-200 text-purple-950 border-fuchsia-300',
  Fighting: 'bg-orange-300 text-orange-950 border-orange-400',
  Darkness: 'bg-slate-300 text-slate-950 border-slate-400',
  Metal: 'bg-zinc-200 text-zinc-950 border-zinc-300',
  Fairy: 'bg-pink-200 text-pink-950 border-pink-300',
  Dragon: 'bg-violet-200 text-indigo-950 border-violet-300',
  Colorless: 'bg-stone-200 text-stone-950 border-stone-300'
};

const LIGHT_DEFAULT_DECK_QUANTITY_BADGE_TYPE_CLASS = 'bg-blue-200 text-blue-950 border-blue-300';

const DECK_ROW_TYPE_CLASSES: Record<string, string> = {
  Grass:
    'bg-gradient-to-r from-lime-950/35 via-green-950/25 to-lime-700/25 border-lime-500/35 shadow-[inset_0_0_24px_rgba(132,204,22,0.08)]',
  Fire:
    'bg-gradient-to-r from-red-950/35 via-orange-950/25 to-orange-600/25 border-orange-500/35 shadow-[inset_0_0_24px_rgba(249,115,22,0.09)]',
  Water:
    'bg-gradient-to-r from-sky-950/35 via-blue-950/25 to-cyan-600/25 border-cyan-500/35 shadow-[inset_0_0_24px_rgba(34,211,238,0.09)]',
  Lightning:
    'bg-gradient-to-r from-yellow-950/35 via-amber-950/25 to-[#B8792E]/25 border-[#B8792E]/35 shadow-[inset_0_0_24px_rgba(184,121,46,0.09)]',
  Psychic:
    'bg-gradient-to-r from-purple-950/35 via-violet-950/25 to-fuchsia-600/25 border-fuchsia-500/35 shadow-[inset_0_0_24px_rgba(217,70,239,0.09)]',
  Fighting:
    'bg-gradient-to-r from-stone-950/35 via-red-950/25 to-orange-700/25 border-orange-700/35 shadow-[inset_0_0_24px_rgba(194,65,12,0.09)]',
  Darkness:
    'bg-gradient-to-r from-zinc-950/40 via-slate-950/30 to-slate-700/25 border-slate-500/30 shadow-[inset_0_0_24px_rgba(100,116,139,0.08)]',
  Metal:
    'bg-gradient-to-r from-zinc-950/35 via-slate-900/25 to-zinc-400/20 border-zinc-400/30 shadow-[inset_0_0_24px_rgba(161,161,170,0.08)]',
  Fairy:
    'bg-gradient-to-r from-pink-950/35 via-rose-950/25 to-pink-500/25 border-pink-400/35 shadow-[inset_0_0_24px_rgba(244,114,182,0.09)]',
  Dragon:
    'bg-gradient-to-r from-indigo-950/35 via-violet-950/25 to-violet-500/25 border-violet-500/35 shadow-[inset_0_0_24px_rgba(139,92,246,0.09)]',
  Colorless:
    'bg-gradient-to-r from-stone-950/35 via-neutral-900/25 to-stone-400/20 border-stone-300/30 shadow-[inset_0_0_24px_rgba(214,211,209,0.07)]'
};

const DEFAULT_DECK_ROW_TYPE_CLASS =
  'bg-gradient-to-r from-red-950/35 via-red-950/25 to-orange-900/20 border-orange-700/30 shadow-[inset_0_0_24px_rgba(234,88,12,0.07)]';

const LIGHT_DECK_ROW_TYPE_CLASSES: Record<string, string> = {
  Grass: 'bg-gradient-to-r from-lime-100 via-green-50 to-lime-200 border-lime-300',
  Fire: 'bg-gradient-to-r from-red-100 via-orange-50 to-orange-200 border-orange-300',
  Water: 'bg-gradient-to-r from-sky-100 via-cyan-50 to-cyan-200 border-cyan-300',
  Lightning: 'bg-gradient-to-r from-[#f4e0b8] via-amber-50 to-[#eed2a1] border-[#d9a441]',
  Psychic: 'bg-gradient-to-r from-fuchsia-100 via-violet-50 to-pink-200 border-fuchsia-300',
  Fighting: 'bg-gradient-to-r from-orange-100 via-stone-50 to-orange-200 border-orange-400',
  Darkness: 'bg-gradient-to-r from-slate-200 via-zinc-100 to-slate-300 border-slate-400',
  Metal: 'bg-gradient-to-r from-zinc-100 via-slate-50 to-zinc-200 border-zinc-300',
  Fairy: 'bg-gradient-to-r from-pink-100 via-rose-50 to-pink-200 border-pink-300',
  Dragon: 'bg-gradient-to-r from-indigo-100 via-violet-50 to-violet-200 border-violet-300',
  Colorless: 'bg-gradient-to-r from-stone-100 via-neutral-50 to-stone-200 border-stone-300'
};

const LIGHT_DEFAULT_DECK_ROW_TYPE_CLASS =
  'bg-gradient-to-r from-orange-100 via-amber-50 to-stone-200 border-[#d2ae76]';

@Component({
  selector: 'app-deck-editor',
  templateUrl: './deck-editor.component.html',
  styleUrl: './deck-editor.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckEditorComponent {
  private readonly languageService = inject(LanguageService);
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly isDeckPanelOpen = signal(true);

  readonly draftName = input.required<string>();
  readonly draftTotal = input.required<number>();
  readonly requiredDeckSize = input.required<number>();
  readonly typeFilters = input.required<DeckTypeFilterOption[]>();
  readonly typeFilter = input.required<DeckTypeFilter>();
  readonly searchTerm = input.required<string>();
  readonly filteredCards = input.required<CardSummary[]>();
  readonly availableCards = input.required<CardSummary[]>();
  readonly draftDeckItems = input.required<DraftDeckItem[]>();
  readonly draftValidationHints = input.required<DeckValidationHint[]>();
  readonly isSaving = input.required<boolean>();
  readonly isRandomizing = input.required<boolean>();
  readonly isLoadingCards = input.required<boolean>();
  readonly cardsErrorMessage = input.required<string>();
  readonly canSaveDraft = input.required<boolean>();
  readonly canRandomize = input.required<boolean>();
  readonly cardQuantity = input.required<(cardId: string) => number>();
  readonly isCopyLimitReached = input.required<(card: CardSummary) => boolean>();
  readonly canAddCardToDraft = input.required<(card: CardSummary) => boolean>();
  readonly isLightTheme = input(false);

  readonly closeEditor = output<void>();
  readonly updateDraftName = output<string>();
  readonly updateSearchTerm = output<string>();
  readonly changeTypeFilter = output<DeckTypeFilter>();
  readonly randomizeEditingDeck = output<void>();
  readonly clearDraft = output<void>();
  readonly saveDraft = output<void>();
  readonly loadAvailableCards = output<void>();
  readonly addCardToDraft = output<CardSummary>();
  readonly removeCardFromDraft = output<string>();

  toggleDeckPanel(): void {
    this.isDeckPanelOpen.update((value) => !value);
  }

  visibleDraftValidationHints(): DeckValidationHint[] {
    return this.draftValidationHints().slice(2);
  }

  onDraftNameInput(event: Event): void {
    this.updateDraftName.emit(this.inputValue(event));
  }

  onSearchTermInput(event: Event): void {
    this.updateSearchTerm.emit(this.inputValue(event));
  }

  deckQuantityBadgeClass(type: string | null | undefined): string {
    const typeClass = this.isLightTheme()
      ? LIGHT_DECK_QUANTITY_BADGE_TYPE_CLASSES[type ?? ''] ?? LIGHT_DEFAULT_DECK_QUANTITY_BADGE_TYPE_CLASS
      : DECK_QUANTITY_BADGE_TYPE_CLASSES[type ?? ''] ?? DEFAULT_DECK_QUANTITY_BADGE_TYPE_CLASS;

    return `${DECK_DRAFT_QUANTITY_BADGE_BASE_CLASS} ${typeClass}`;
  }

  deckRowClass(type: string | null | undefined): string {
    const typeClass = this.isLightTheme()
      ? LIGHT_DECK_ROW_TYPE_CLASSES[type ?? ''] ?? LIGHT_DEFAULT_DECK_ROW_TYPE_CLASS
      : DECK_ROW_TYPE_CLASSES[type ?? ''] ?? DEFAULT_DECK_ROW_TYPE_CLASS;

    return `deck-quantity-row ${DECK_ROW_BASE_CLASS} ${typeClass}`;
  }

  panelClass(): string {
    return this.isLightTheme()
      ? 'grid min-h-0 content-start gap-3 rounded-2xl border-2 border-[rgba(117,72,32,0.55)] bg-[#fff8e3]/76 backdrop-blur-sm p-4 lg:h-full lg:overflow-hidden'
      : 'grid min-h-0 content-start gap-3 rounded-2xl border-2 border-[rgba(139,74,24,0.9)] bg-[rgba(43,18,6,0.82)] backdrop-blur-sm p-4 lg:h-full lg:overflow-hidden';
  }

  quietButtonClass(active = false): string {
    if (this.isLightTheme()) {
      return active
        ? 'min-h-10 rounded-xl bg-[#9f0710] px-2 font-bold text-[#fff3d2] hover:bg-[#b10d13] disabled:cursor-not-allowed disabled:bg-[#b97870] disabled:text-[#f8dfc9] disabled:opacity-100'
        : 'min-h-10 rounded-xl bg-[#f8e8bd] px-2 font-bold text-[#6b3a22] hover:bg-[#f6dfad] hover:text-[#3b1d12] disabled:cursor-not-allowed disabled:opacity-55';
    }

    return active
      ? 'min-h-10 rounded-xl bg-[#8b070c] px-2 font-bold text-white hover:bg-[#6f0509] disabled:cursor-not-allowed disabled:opacity-55'
      : 'min-h-10 rounded-xl bg-[rgba(26,13,2,0.72)] px-2 font-bold text-[#f9f0d0]/75 hover:bg-[#6f0509] hover:text-white disabled:cursor-not-allowed disabled:opacity-55';
  }

  inputClass(): string {
    return this.isLightTheme()
      ? 'min-h-10 w-full rounded-xl border border-[rgba(117,72,32,0.45)] bg-[#f8e8bd] px-3 text-[#3b1d12] outline-none focus:border-[#b8752a] focus:ring-4 focus:ring-[#b8752a]/10'
      : 'min-h-10 w-full rounded-xl border border-[rgba(139,74,24,0.8)] bg-[rgba(26,13,2,0.82)] px-3 text-[#fff4c7] outline-none focus:border-[#B8792E] focus:ring-4 focus:ring-[#D9A441]/15';
  }

  cardDisplayName(card: CardSummary, language: AppLanguage): string {
    return this.languageService.cardName(card, language);
  }

  cardDisplaySupertype(card: CardSummary, language: AppLanguage): string {
    return this.languageService.supertype(card, language);
  }

  cardDisplayCategory(card: CardSummary, language: AppLanguage): string {
    return this.languageService.category(card, language);
  }

  tutorialFilterTarget(filter: DeckTypeFilter): string | null {
    switch (filter) {
      case 'ALL':
        return 'deck-filter-all';
      case 'POKEMON':
        return 'deck-filter-pokemon';
      case 'ENERGY':
        return 'deck-filter-energy';
      case 'TRAINER':
        return 'deck-filter-trainer';
      default:
        return null;
    }
  }

  private inputValue(event: Event): string {
    return event.target instanceof HTMLInputElement ? event.target.value : '';
  }
}
