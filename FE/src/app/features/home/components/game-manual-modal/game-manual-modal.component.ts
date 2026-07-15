import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';

@Component({
  selector: 'app-game-manual-modal',
  templateUrl: './game-manual-modal.component.html',
  styleUrl: './game-manual-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameManualModalComponent {
  private readonly languageService = inject(LanguageService);

  readonly isLightTheme = input(false);
  readonly close = output<void>();

  readonly t = (key: string) => this.languageService.t(key);

  overlayClasses(): string {
    return 'fixed inset-0 z-40 flex items-start justify-center px-4 py-6 sm:items-center';
  }

  panelClasses(): string {
    return this.isLightTheme()
      ? 'relative z-10 flex w-full max-w-2xl flex-col overflow-hidden rounded-[22px] border-2 border-[#d6903f]/85 bg-[#fff8e3]/97 text-[#3e1f12] shadow-[10px_10px_0_rgba(96,50,18,0.22)]'
      : 'relative z-10 flex w-full max-w-2xl flex-col overflow-hidden rounded-[22px] border-2 border-[#8a4b20]/85 bg-[#2a0805]/97 shadow-[10px_10px_0_rgba(20,4,3,0.74)]';
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

  sectionTitleClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mb-3 text-[0.7rem] uppercase tracking-[0.2em] text-[#8b1b1f]'
      : 'font-pixel mb-3 text-[0.7rem] uppercase tracking-[0.2em] text-[#fde68a]';
  }

  sectionBodyClasses(): string {
    return this.isLightTheme()
      ? 'text-sm leading-7 text-[#7b4a2a]'
      : 'text-sm leading-7 text-[#f8e7b0]';
  }

  labelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mb-1 text-[0.58rem] uppercase tracking-[0.18em] text-[#6f3517]'
      : 'font-pixel mb-1 text-[0.58rem] uppercase tracking-[0.18em] text-[#fde68a]/80';
  }

  dividerClasses(): string {
    return this.isLightTheme()
      ? 'my-5 border-t border-[#d6903f]/40'
      : 'my-5 border-t border-[#8a4b20]/40';
  }

  trainerBadgeClasses(variant: 'item' | 'supporter' | 'stadium' | 'tool'): string {
    const base = 'font-pixel mb-1 inline-block rounded px-2 py-0.5 text-[0.52rem] uppercase tracking-[0.15em]';
    const colors: Record<typeof variant, string> = {
      item:      'bg-[#1d4ed8]/20 text-[#60a5fa]',
      supporter: 'bg-[#7c3aed]/20 text-[#a78bfa]',
      stadium:   'bg-[#065f46]/20 text-[#34d399]',
      tool:      'bg-[#92400e]/20 text-[#fbbf24]'
    };
    return `${base} ${colors[variant]}`;
  }

  conditionBadgeClasses(variant: 'asleep' | 'burned' | 'confused' | 'paralyzed' | 'poisoned'): string {
    const base = 'font-pixel mb-1 inline-block rounded px-2 py-0.5 text-[0.52rem] uppercase tracking-[0.15em]';
    const colors: Record<typeof variant, string> = {
      asleep:    'bg-[#1e3a5f]/30 text-[#93c5fd]',
      burned:    'bg-[#7f1d1d]/30 text-[#fca5a5]',
      confused:  'bg-[#4a1d96]/30 text-[#c4b5fd]',
      paralyzed: 'bg-[#713f12]/30 text-[#fcd34d]',
      poisoned:  'bg-[#14532d]/30 text-[#86efac]'
    };
    return `${base} ${colors[variant]}`;
  }

  footerClasses(): string {
    return this.isLightTheme()
      ? 'flex justify-end border-t-2 border-[#d6903f]/85 bg-[#fff1cf]/68 px-5 py-4'
      : 'flex justify-end border-t-2 border-[#8a4b20]/75 bg-[#170706]/72 px-5 py-4';
  }

  footerButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel inline-flex min-h-11 items-center justify-center rounded-[14px] border-2 border-[#d6903f] bg-[#fff8e3]/90 px-4 py-3 text-[0.66rem] uppercase tracking-[0.16em] text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014]'
      : 'font-pixel inline-flex min-h-11 items-center justify-center rounded-[14px] border-2 border-[#8a4b20] bg-[#2a0805]/72 px-4 py-3 text-[0.66rem] uppercase tracking-[0.16em] text-[#fff7d6] transition hover:bg-[#3b0905]';
  }
}
