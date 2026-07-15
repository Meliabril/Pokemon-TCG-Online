import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { CardSummary } from '../../../../core/models/interfaces/card/card-summary.interface';
import { DeckSummary } from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { AppLanguage, LanguageService } from '../../../../core/services/language.service';
import { countCardsBySupertype, totalDeckCards } from '../../utils/deck-builder.utils';

@Component({
  selector: 'app-deck-overview',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './deck-overview.component.html',
  styleUrl: './deck-overview.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block h-full'
  }
})
export class DeckOverviewComponent {
  private readonly languageService = inject(LanguageService);
  readonly language = this.languageService.language;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly appRoutes = APP_ROUTES;
  readonly decks = input.required<DeckSummary[]>();
  readonly maxDecks = input.required<number>();
  readonly miniPreviewSlots = input.required<number[]>();
  readonly emptySlotIndexes = input.required<number[]>();
  readonly isSaving = input.required<boolean>();
  readonly isLightTheme = input(false);

  readonly startEditing = output<DeckSummary>();
  readonly activateDeck = output<string>();
  readonly deleteDeck = output<string>();
  readonly startCreating = output<void>();

  readonly deckTotal = totalDeckCards;
  readonly deckCounts = countCardsBySupertype;
  readonly activeDeck = computed(() => this.decks().find((deck) => deck.active) ?? null);

  // Pagination for active deck cards
  readonly itemsPerPage = 12; // 1 row of cards
  readonly currentPage = signal(0);
  readonly paginatedCards = computed(() => {
    const deck = this.activeDeck();
    if (!deck) return [];
    const startIndex = this.currentPage() * this.itemsPerPage;
    return deck.cards.slice(startIndex, startIndex + this.itemsPerPage);
  });
  readonly totalPages = computed(() => {
    const deck = this.activeDeck();
    return deck ? Math.ceil(deck.cards.length / this.itemsPerPage) : 0;
  });

  nextPage() {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.update(p => p + 1);
    }
  }

  prevPage() {
    if (this.currentPage() > 0) {
      this.currentPage.update(p => p - 1);
    }
  }

  resetPage() {
    this.currentPage.set(0);
  }

  cardDisplayName(card: CardSummary, language: AppLanguage): string {
    return this.languageService.cardName(card, language);
  }

  panelClass(active = false): string {
    const base = 'flex min-h-[23rem] flex-col gap-4 rounded-2xl border-2 p-4 backdrop-blur-sm';

    if (this.isLightTheme()) {
      const activeClasses = active
        ? 'border-[#2f63d8] shadow-[0_0_14px_rgba(47,99,216,0.22)]'
        : 'border-[rgba(117,72,32,0.55)]';
      return `${base} bg-[#fff8e3]/76 ${activeClasses}`;
    }

    const activeClasses = active
      ? 'border-blue-500 shadow-[0_0_14px_rgba(59,130,246,0.22)]'
      : 'border-[rgba(139,74,24,0.9)]';
    return `${base} bg-[rgba(43,18,6,0.82)] ${activeClasses}`;
  }

  statClass(): string {
    return this.isLightTheme()
      ? 'grid gap-1 rounded-xl border border-[rgba(117,72,32,0.45)] bg-[#f8e8bd] px-1 py-3 text-center text-xs text-[#6b3a22]'
      : 'grid gap-1 rounded-xl border border-[#B8792E]/15 bg-[rgba(26,13,2,0.5)] px-1 py-3 text-center text-xs text-[#f9f0d0]/65';
  }

  statValueClass(): string {
    return this.isLightTheme() ? 'text-2xl leading-none text-[#3b1d12]' : 'text-2xl leading-none text-[#fff4c7]';
  }

  previewSlotClass(): string {
    return this.isLightTheme()
      ? 'grid aspect-[3/4] place-items-center overflow-hidden rounded-lg bg-[#f6dfad] font-extrabold text-[#8c6240]'
      : 'grid aspect-[3/4] place-items-center overflow-hidden rounded-lg bg-[rgba(26,13,2,0.76)] font-extrabold text-[#f9f0d0]/60';
  }

  primaryButtonClass(): string {
    return this.isLightTheme()
      ? 'min-h-10 rounded-xl bg-[#9f0710] px-3 font-bold text-[#fff3d2] hover:bg-[#b10d13] disabled:cursor-not-allowed disabled:bg-[#b97870] disabled:text-[#f8dfc9] disabled:opacity-100'
      : 'min-h-10 rounded-xl bg-[#8b070c] px-3 font-bold text-white hover:bg-[#6f0509] disabled:cursor-not-allowed disabled:opacity-55';
  }

  emptySlotClass(): string {
    return this.isLightTheme()
      ? 'grid min-h-[23rem] content-center place-items-center gap-2 rounded-2xl border-2 border-dashed border-[#9c6a38] bg-[#fff8e3]/65 backdrop-blur-sm text-[#6b3a22] hover:bg-[#fff8e3]/78 disabled:cursor-not-allowed disabled:opacity-55'
      : 'grid min-h-[23rem] content-center place-items-center gap-2 rounded-2xl border-2 border-dashed border-[rgba(139,74,24,0.9)] bg-[rgba(43,18,6,0.82)] backdrop-blur-sm text-[#f9f0d0]/70 hover:bg-[rgba(43,18,6,0.92)] disabled:cursor-not-allowed disabled:opacity-55';
  }

  sectionClass(): string {
    return this.isLightTheme()
      ? 'mt-6 flex flex-col gap-3 rounded-2xl border-2 border-[rgba(117,72,32,0.55)] bg-[#fff8e3]/76 backdrop-blur-sm p-4 shadow-xl'
      : 'mt-6 flex flex-col gap-3 rounded-2xl border-2 border-[rgba(139,74,24,0.9)] bg-[rgba(43,18,6,0.82)] backdrop-blur-sm p-4 shadow-xl';
  }
}
