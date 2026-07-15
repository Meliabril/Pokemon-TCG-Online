import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { DeckSummary } from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { LanguageService } from '../../../../core/services/language.service';

type DeckActionKind = 'activating' | 'randomizing' | null;

@Component({
  selector: 'app-deck-selection-panel',
  imports: [RouterLink],
  templateUrl: './deck-selection-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckSelectionPanelComponent {
  private readonly languageService = inject(LanguageService);

  readonly routes = APP_ROUTES;
  readonly decks = input.required<DeckSummary[]>();
  readonly status = input.required<string>();
  readonly errorMessage = input('');
  readonly deckActionKind = input<DeckActionKind>(null);
  readonly deckActionDeckId = input<string | null>(null);
  readonly isDeckChangeLocked = input(false);
  readonly showDeckEditorLink = input(true);
  readonly showCreateDeckButton = input(false);
  readonly isLightTheme = input(false);

  readonly activateDeck = output<string>();
  readonly randomizeDeck = output<string>();
  readonly createDeck = output<void>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  deckCardClasses(deck: DeckSummary): string {
    const base = 'rounded-[22px] border px-4 py-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.04)]';

    if (this.isLightTheme()) {
      return deck.active
        ? `${base} border-[#d6903f]/85 bg-[#fff1cf]/76`
        : `${base} border-[#e7b76f]/85 bg-[#fff8e3]/72`;
    }

    return deck.active
      ? `${base} border-[#facc15]/32 bg-[#5d0b0f]/68`
      : `${base} border-[#8a4b20]/65 bg-[#2a0805]/72`;
  }

  deckStatusBadgeClasses(deck: DeckSummary): string {
    if (this.isLightTheme()) {
      if (deck.active) {
        return 'border-[#2563eb]/30 bg-blue-50 text-blue-800';
      }

      return deck.valid
        ? 'border-[#22c55e]/35 bg-green-50 text-green-800'
        : 'border-[#d6903f] bg-[#fff1cf] text-[#8b1b1f]';
    }

    if (deck.active) {
      return 'border-[#60a5fa]/40 bg-[#1e3a8a]/40 text-[#dbeafe]';
    }

    return deck.valid
      ? 'border-[#22c55e]/35 bg-[#14532d]/35 text-[#bbf7d0]'
      : 'border-[#f59e0b]/35 bg-[#451a03]/72 text-[#fde68a]';
  }

  isDeckActionPending(): boolean {
    return this.deckActionKind() !== null;
  }

  isDeckChangeDisabled(): boolean {
    return this.isDeckActionPending() || this.status() === 'joining' || this.isDeckChangeLocked();
  }

  deckStatusLabel(deck: DeckSummary): string {
    if (deck.active) {
      return this.t('HOME.ACTIVE_DECK');
    }

    return deck.valid ? this.t('COMMON.VALID') : this.t('HOME.REQUIRES_ADJUSTMENTS');
  }

  deckActionLabel(deckId: string, kind: Exclude<DeckActionKind, null>, idleKey: string): string {
    if (this.deckActionKind() === kind && this.deckActionDeckId() === deckId) {
      return kind === 'activating' ? this.t('HOME.ACTIVATING') : this.t('HOME.RANDOMIZING');
    }

    return this.t(idleKey);
  }

  panelClasses(): string {
    return this.isLightTheme()
      ? 'rounded-[24px] border border-[#d6903f]/85 bg-[#fff8e3]/78 p-5 text-[#3e1f12] shadow-[0_20px_48px_rgba(96,50,18,0.2)] backdrop-blur-md sm:p-6'
      : 'rounded-[24px] border border-[#8a4b20]/70 bg-[#3b0905]/80 p-5 shadow-[0_20px_48px_rgba(0,0,0,0.28)] backdrop-blur-md sm:p-6';
  }

  titleClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-sm tracking-[0.18em] text-[#8b1b1f]'
      : 'font-pixel text-sm tracking-[0.18em] text-[#fde68a]';
  }

  descriptionClasses(): string {
    return this.isLightTheme()
      ? 'mt-2 text-sm leading-7 text-[#7b4a2a]'
      : 'mt-2 text-sm leading-7 text-[#d8b982]';
  }

  countBadgeClasses(): string {
    return this.isLightTheme()
      ? 'rounded-full border border-[#e7b76f]/85 bg-[#fff1cf]/72 px-3 py-1 text-xs text-[#7b4a2a]'
      : 'rounded-full border border-[#facc15]/28 bg-[#facc15]/10 px-3 py-1 text-xs text-[#fde68a]';
  }

  emptyStateClasses(): string {
    return this.isLightTheme()
      ? 'mt-4 rounded-[22px] border border-[#e7b76f]/85 bg-[#fff1cf]/72 px-4 py-4 text-sm leading-7 text-[#7b4a2a]'
      : 'mt-4 rounded-[22px] border border-[#8a4b20]/65 bg-[#2a0805]/72 px-4 py-4 text-sm leading-7 text-[#d8b982]';
  }

  deckNameClasses(): string {
    return this.isLightTheme() ? 'text-lg font-semibold text-[#3e1f12]' : 'text-lg font-semibold text-[#fff7d6]';
  }

  validationErrorClasses(): string {
    return this.isLightTheme() ? 'mt-3 text-sm text-[#8b1b1f]' : 'mt-3 text-sm text-[#fde68a]';
  }

  primaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#d6903f] bg-[#8b1b1f] px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#fff7df] transition hover:bg-[#651014] disabled:cursor-not-allowed disabled:opacity-65'
      : 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#facc15]/30 bg-[#8b070c] px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#fff7d6] transition hover:bg-[#650408] disabled:cursor-not-allowed disabled:opacity-65';
  }

  secondaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#d6903f] bg-[#fff8e3]/90 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014] disabled:cursor-not-allowed disabled:opacity-65'
      : 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#8a4b20] bg-[#2a0805]/78 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#fff7d6] transition hover:bg-[#3b0905] disabled:cursor-not-allowed disabled:opacity-65';
  }

  editorLinkClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#d6903f] bg-[#fff8e3]/80 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#7b4a2a] no-underline transition hover:bg-[#fff1cf] hover:text-[#8b1b1f]'
      : 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#8a4b20] bg-[#2a0805]/56 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#d8b982] no-underline transition hover:bg-[#3b0905]';
  }

  createButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#d6903f] bg-[#fff8e3]/90 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014]'
      : 'font-pixel inline-flex min-h-11 items-center justify-center rounded-2xl border border-[#60a5fa]/35 bg-[#1d4ed8]/22 px-4 py-2 text-[0.72rem] tracking-[0.12em] text-[#dbeafe] transition hover:bg-[#1d4ed8]/35';
  }
}
