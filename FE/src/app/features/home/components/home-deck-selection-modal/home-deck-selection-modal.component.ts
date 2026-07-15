import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { PreMatchChecklistState } from '../../../../core/models/interfaces/play/prematch.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { DeckSelectionPanelComponent } from '../../../play/components/deck-selection-panel/deck-selection-panel.component';

type DeckActionKind = 'activating' | 'randomizing' | null;

@Component({
  selector: 'app-home-deck-selection-modal',
  imports: [DeckSelectionPanelComponent],
  templateUrl: './home-deck-selection-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeDeckSelectionModalComponent {
  private readonly languageService = inject(LanguageService);

  readonly state = input.required<PreMatchChecklistState>();
  readonly deckActionKind = input<DeckActionKind>(null);
  readonly deckActionDeckId = input<string | null>(null);
  readonly isDeckChangeLocked = input(false);
  readonly isLightTheme = input(false);

  readonly close = output<void>();
  readonly activateDeck = output<string>();
  readonly randomizeDeck = output<string>();
  readonly createDeck = output<void>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  panelClasses(): string {
    return this.isLightTheme()
      ? 'relative z-10 flex w-full max-w-4xl flex-col overflow-hidden rounded-[22px] border-2 border-[#d6903f]/85 bg-[#fff8e3]/82 text-[#3e1f12] shadow-[10px_10px_0_rgba(96,50,18,0.22)]'
      : 'relative z-10 flex w-full max-w-4xl flex-col overflow-hidden rounded-[22px] border-2 border-[#8a4b20]/85 bg-[#2a0805]/96 shadow-[10px_10px_0_rgba(20,4,3,0.74)]';
  }

  headerClasses(): string {
    return this.isLightTheme()
      ? 'flex items-center justify-between border-b-2 border-[#d6903f]/85 bg-[#fff1cf]/74 px-5 py-4'
      : 'flex items-center justify-between border-b-2 border-[#8a4b20]/75 bg-[#3b0905]/82 px-5 py-4';
  }

  eyebrowClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.58rem] uppercase tracking-[0.22em] text-[#8b1b1f]'
      : 'font-pixel text-[0.58rem] uppercase tracking-[0.22em] text-[#fde68a]';
  }

  titleClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.92rem] uppercase tracking-[0.16em] text-[#3e1f12]'
      : 'font-pixel text-[0.92rem] uppercase tracking-[0.16em] text-[#fff7d6]';
  }

  closeButtonClasses(): string {
    return this.isLightTheme()
      ? 'grid h-10 w-10 place-items-center rounded-[12px] border-2 border-[#d6903f] bg-[#fff8e3]/90 text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014]'
      : 'grid h-10 w-10 place-items-center rounded-[12px] border-2 border-[#8a4b20] bg-[#170706]/72 text-[#fde68a] transition hover:bg-[#3b0905] hover:text-[#fff7d6]';
  }

  feedbackClasses(): string {
    return this.isLightTheme()
      ? 'mb-4 rounded-2xl border border-[#2563eb]/25 bg-blue-50/82 px-4 py-3 text-sm text-blue-800'
      : 'mb-4 rounded-2xl border border-[#2563eb]/35 bg-[#172554]/70 px-4 py-3 text-sm text-[#bfdbfe]';
  }

  errorClasses(): string {
    return this.isLightTheme()
      ? 'mb-4 rounded-2xl border border-[#dc2626]/35 bg-red-50/82 px-4 py-3 text-sm text-red-800'
      : 'mb-4 rounded-2xl border border-[#dc2626]/45 bg-[#450a0a]/75 px-4 py-3 text-sm text-[#fecaca]';
  }

  lockNoticeClasses(): string {
    return this.isLightTheme()
      ? 'mb-4 rounded-2xl border border-[#d6903f]/70 bg-[#fff1cf]/82 px-4 py-3 text-sm leading-6 text-[#7b4a2a]'
      : 'mb-4 rounded-2xl border border-[#facc15]/30 bg-[#451a03]/70 px-4 py-3 text-sm leading-6 text-[#fde68a]';
  }
}
