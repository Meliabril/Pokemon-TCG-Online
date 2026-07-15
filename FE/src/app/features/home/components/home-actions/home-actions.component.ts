import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { DeckSummary } from '../../../../core/models/interfaces/deck/deck-summary.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { AppCardComponent, AppCardTone } from '../../../../shared/ui/layout/app-card/app-card.component';

@Component({
  selector: 'app-home-actions',
  imports: [AppCardComponent],
  templateUrl: './home-actions.component.html',
  styleUrl: './home-actions.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeActionsComponent {
  private readonly languageService = inject(LanguageService);

  readonly primaryLabel = input.required<string>();
  readonly isPrimaryDisabled = input.required<boolean>();
  readonly activeDeck = input<DeckSummary | null>(null);
  readonly hasNoDecks = input<boolean>(false);
  readonly isDeckChangeLocked = input(false);
  readonly isLightTheme = input(false);
  readonly tone = input<AppCardTone>('dark');

  readonly primaryClick = output<void>();
  readonly openConfig = output<void>();
  readonly openDeckSelection = output<void>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  deckLabelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.65rem] uppercase tracking-[0.2em] text-[#7b4a2a]'
      : 'font-pixel text-[0.65rem] uppercase tracking-[0.2em] text-[#d8b982]/80';
  }

  deckStatusClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex items-center gap-2 text-[0.55rem] font-semibold tracking-wider text-[#7b4a2a]'
      : 'font-pixel inline-flex items-center gap-2 text-[0.55rem] font-semibold tracking-wider text-[#d8b982]';
  }

  deckNameClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.9rem] uppercase tracking-[0.1em] text-[#3e1f12]'
      : 'font-pixel text-[0.9rem] uppercase tracking-[0.1em] text-[#fff7d6] drop-shadow-md';
  }

  noDeckTextClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.65rem] uppercase tracking-[0.1em] text-[#7b4a2a] italic'
      : 'font-pixel text-[0.65rem] uppercase tracking-[0.1em] text-[#d8b982]/80 italic';
  }

  deckButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel group inline-flex items-center gap-2 rounded-lg border border-[#d6903f] bg-[#fff8e3]/90 px-3 py-1.5 text-[0.65rem] font-bold uppercase tracking-[0.15em] text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014] disabled:cursor-not-allowed disabled:opacity-55 whitespace-nowrap'
      : 'font-pixel group inline-flex items-center gap-2 rounded-lg border border-[#8a4b20]/60 bg-[#2a0c0a] px-3 py-1.5 text-[0.65rem] font-bold uppercase tracking-[0.15em] text-[#fde68a] transition hover:bg-[#3d120f] hover:text-[#fff7d6] disabled:cursor-not-allowed disabled:opacity-55 whitespace-nowrap';
  }

  selectDeckButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel group inline-flex items-center gap-2 rounded-lg border border-[#d6903f]/60 bg-[#fff8e3]/90 px-3 py-1.5 text-[0.65rem] font-bold uppercase tracking-[0.15em] text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014] disabled:cursor-not-allowed disabled:opacity-55 shadow-[0_0_10px_rgba(214,144,63,0.2)] whitespace-nowrap'
      : 'font-pixel group inline-flex items-center gap-2 rounded-lg border border-[#8a4b20]/60 bg-[#2a0c0a] px-3 py-1.5 text-[0.65rem] font-bold uppercase tracking-[0.15em] text-[#facc15] transition hover:bg-[#3d120f] hover:text-[#fff7d6] disabled:cursor-not-allowed disabled:opacity-55 shadow-[0_0_10px_rgba(250,204,21,0.2)] whitespace-nowrap';
  }

  deckLockMessageClasses(): string {
    return this.isLightTheme()
      ? 'mt-3 text-xs leading-5 text-[#8b1b1f]'
      : 'mt-3 text-xs leading-5 text-[#fde68a]';
  }

  primaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'group relative w-full overflow-hidden rounded-[20px] bg-gradient-to-b from-[#b43a3d] to-[#8b1b1f] p-[3px] shadow-[0_8px_30px_rgba(96,50,18,0.28)] transition-all hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(96,50,18,0.34)] disabled:pointer-events-none disabled:opacity-60'
      : 'group relative w-full overflow-hidden rounded-[20px] bg-gradient-to-b from-[#b91c1c] to-[#7f1d1d] p-[3px] shadow-[0_8px_30px_rgba(185,28,28,0.4)] transition-all hover:-translate-y-1 hover:shadow-[0_12px_40px_rgba(185,28,28,0.6)] disabled:pointer-events-none disabled:opacity-60';
  }

  primaryButtonInnerClasses(): string {
    return this.isLightTheme()
      ? 'relative flex min-h-[4.5rem] w-full items-center justify-center gap-4 rounded-[17px] bg-[linear-gradient(to_bottom,rgba(255,248,227,0.16),rgba(101,16,20,0.18))] px-6 py-4 transition-colors group-hover:bg-[linear-gradient(to_bottom,rgba(255,248,227,0.22),rgba(101,16,20,0.12))]'
      : 'relative flex min-h-[4.5rem] w-full items-center justify-center gap-4 rounded-[17px] bg-[linear-gradient(to_bottom,rgba(0,0,0,0.1),rgba(0,0,0,0.4))] px-6 py-4 backdrop-blur-sm transition-colors group-hover:bg-[linear-gradient(to_bottom,rgba(0,0,0,0.0),rgba(0,0,0,0.3))]';
  }

  primaryIconClasses(): string {
    return this.isLightTheme()
      ? 'flex h-10 w-10 items-center justify-center rounded-full bg-[#fff7df]/20 text-[#fff7df] shadow-[0_0_15px_rgba(96,50,18,0.18)] transition-transform group-hover:scale-110 group-hover:bg-[#fff7df]/30 group-hover:shadow-[0_0_20px_rgba(96,50,18,0.26)]'
      : 'flex h-10 w-10 items-center justify-center rounded-full bg-[#fde68a]/20 text-[#fde68a] shadow-[0_0_15px_rgba(253,230,138,0.3)] transition-transform group-hover:scale-110 group-hover:bg-[#fde68a]/30 group-hover:text-white group-hover:shadow-[0_0_20px_rgba(253,230,138,0.5)]';
  }

  primaryLabelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[1.1rem] uppercase tracking-[0.2em] text-[#fff7df] drop-shadow-[0_2px_4px_rgba(96,50,18,0.38)]'
      : 'font-pixel text-[1.1rem] uppercase tracking-[0.2em] text-[#fff7d6] drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]';
  }

  secondaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'group flex min-h-[3.5rem] w-full items-center justify-center gap-3 rounded-[16px] border-2 border-[#d6903f]/50 bg-[#fff8e3]/72 px-5 py-3 text-[#8b1b1f] transition-all hover:border-[#d6903f] hover:bg-[#fff1cf]/86 hover:shadow-[0_4px_20px_rgba(96,50,18,0.22)] hover:text-[#651014] disabled:pointer-events-none disabled:opacity-60'
      : 'group flex min-h-[3.5rem] w-full items-center justify-center gap-3 rounded-[16px] border-2 border-[#8a4b20]/50 bg-[#2a0805]/60 px-5 py-3 transition-all hover:border-[#8a4b20] hover:bg-[#3b0905]/80 hover:shadow-[0_4px_20px_rgba(138,75,32,0.3)] disabled:pointer-events-none disabled:opacity-60';
  }

  secondaryIconClasses(): string {
    return this.isLightTheme()
      ? 'text-[#8b1b1f] transition-colors group-hover:text-[#651014]'
      : 'text-[#d8b982] transition-colors group-hover:text-[#fde68a]';
  }

  secondaryLabelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.7rem] uppercase tracking-[0.15em] text-[#8b1b1f] transition-colors group-hover:text-[#651014]'
      : 'font-pixel text-[0.7rem] uppercase tracking-[0.15em] text-[#d8b982] transition-colors group-hover:text-[#fff7d6]';
  }
}
