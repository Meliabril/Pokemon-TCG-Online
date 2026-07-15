import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { LanguageService } from '../../../../core/services/language.service';
import { AppCardComponent, AppCardTone } from '../../../../shared/ui/layout/app-card/app-card.component';

@Component({
  selector: 'app-how-to-play-card',
  imports: [AppCardComponent],
  templateUrl: './how-to-play-card.component.html',
  styleUrl: './how-to-play-card.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HowToPlayCardComponent {
  private readonly languageService = inject(LanguageService);

  readonly manualClick = output<void>();
  readonly isLightTheme = input(false);
  readonly tone = input<AppCardTone>('dark');
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  titleClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.9rem] uppercase tracking-[0.22em] text-[#8b1b1f]'
      : 'font-pixel text-[0.9rem] uppercase tracking-[0.22em] text-[#fde68a]';
  }

  cardHeadingClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mb-2 text-[0.56rem] uppercase tracking-[0.2em] text-[#6f3517]'
      : 'font-pixel mb-2 text-[0.56rem] uppercase tracking-[0.2em] text-[#fde68a]';
  }

  cardTextClasses(): string {
    return this.isLightTheme() ? 'text-sm leading-6 text-[#7b4a2a]' : 'text-sm leading-6 text-[#f8e7b0]';
  }

  buttonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mt-auto inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-[14px] border-2 border-[#d6903f] bg-[#fff8e3]/72 px-4 py-3 text-[0.66rem] uppercase tracking-[0.18em] text-[#8b1b1f] shadow-[3px_3px_0_rgba(96,50,18,0.22)] transition hover:translate-x-[1px] hover:translate-y-[1px] hover:bg-[#fff1cf]/86 hover:text-[#651014] hover:shadow-[2px_2px_0_rgba(96,50,18,0.22)]'
      : 'font-pixel mt-auto inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-[14px] border-2 border-[#8a4b20] bg-[#3b0905]/78 px-4 py-3 text-[0.66rem] uppercase tracking-[0.18em] text-[#fff7d6] shadow-[3px_3px_0_rgba(20,4,3,0.65)] transition hover:translate-x-[1px] hover:translate-y-[1px] hover:bg-[#4a0b07] hover:shadow-[2px_2px_0_rgba(20,4,3,0.65)]';
  }
}
