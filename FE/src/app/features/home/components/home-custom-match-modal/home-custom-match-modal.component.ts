import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';


@Component({
  selector: 'app-home-custom-match-modal',
  templateUrl: './home-custom-match-modal.component.html',
  styleUrl: './home-custom-match-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeCustomMatchModalComponent {
  private readonly languageService = inject(LanguageService);

  readonly roomCode = input.required<string>();
  readonly isLightTheme = input(false);
  readonly close = output<void>();
  readonly createRoom = output<void>();
  readonly joinRoom = output<string>();
  readonly joinCode = signal('');
  readonly copied = signal(false);
  readonly joinAttempted = signal(false);
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);

  panelClasses(): string {
    return this.isLightTheme()
      ? 'relative z-10 flex w-full max-w-3xl flex-col overflow-hidden rounded-[22px] border-2 border-[#d6903f]/85 bg-[#fff8e3]/82 text-[#3e1f12] shadow-[10px_10px_0_rgba(96,50,18,0.22)]'
      : 'relative z-10 flex w-full max-w-3xl flex-col overflow-hidden rounded-[22px] border-2 border-[#8a4b20]/85 bg-[#2a0805]/96 shadow-[10px_10px_0_rgba(20,4,3,0.74)]';
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


  descriptionBoxClasses(): string {
    return this.isLightTheme()
      ? 'rounded-[16px] border-2 border-[#e7b76f]/85 bg-[#fff1cf]/72 p-4 text-center text-[0.8rem] text-[#7b4a2a] backdrop-blur-sm'
      : 'rounded-[16px] border-2 border-[#8a4b20]/40 bg-[#1a0806]/60 p-4 text-[0.8rem] text-center text-[#d8b982]/90 backdrop-blur-sm';
  }

  optionCardClasses(): string {
    return this.isLightTheme()
      ? 'flex flex-col justify-between rounded-[16px] border-2 border-[#e7b76f]/85 bg-[#fff1cf]/72 p-5 transition-all hover:border-[#d6903f]'
      : 'flex flex-col justify-between rounded-[16px] border-2 border-[#8a4b20]/50 bg-[#1a0806]/80 p-5 shadow-[4px_4px_0_rgba(20,4,3,0.3)] transition-all hover:border-[#8a4b20]/80';
  }

  labelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.6rem] uppercase tracking-[0.2em] text-[#6f3517]'
      : 'font-pixel text-[0.6rem] uppercase tracking-[0.2em] text-[#fde68a]';
  }

  fieldLabelClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel text-[0.5rem] uppercase tracking-[0.15em] text-[#7b4a2a]'
      : 'font-pixel text-[0.5rem] uppercase tracking-[0.15em] text-[#d8b982]';
  }

  codeBoxClasses(): string {
    return this.isLightTheme()
      ? 'flex-1 rounded-[12px] border border-[#d6903f] bg-[#fff8e3]/95 px-3 py-2 text-center font-pixel text-sm tracking-[0.2em] text-[#3e1f12]'
      : 'flex-1 rounded-[12px] border border-[#8a4b20]/40 bg-[#2a0805]/50 px-3 py-2 text-center font-pixel text-sm tracking-[0.2em] text-[#fff7d6]';
  }

  copyButtonClasses(): string {
    return this.isLightTheme()
      ? 'group rounded-[12px] border border-[#d6903f] bg-[#fff8e3]/90 px-3 py-2 text-xs text-[#8b1b1f] transition hover:bg-[#fff1cf] hover:text-[#651014]'
      : 'group rounded-[12px] border border-[#8a4b20]/60 bg-[#3b0905]/60 px-3 py-2 text-xs text-[#d8b982] transition hover:bg-[#4a0b07] hover:text-[#fff7d6]';
  }

  primaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mt-5 inline-flex min-h-[2.75rem] w-full items-center justify-center rounded-[12px] border border-[#8a4b20]/60 bg-gradient-to-r from-[#a5252a] to-[#8b1b1f] px-4 py-2 text-[0.6rem] uppercase tracking-[0.15em] text-[#fff7df] shadow-[0_4px_15px_rgba(96,50,18,0.24)] transition-all hover:-translate-y-0.5 hover:shadow-[0_6px_20px_rgba(96,50,18,0.32)]'
      : 'font-pixel mt-5 inline-flex min-h-[2.75rem] w-full items-center justify-center rounded-[12px] border border-[#8a4b20]/60 bg-gradient-to-r from-[#b91c1c] to-[#7f1d1d] px-4 py-2 text-[0.6rem] uppercase tracking-[0.15em] text-[#fff7d6] shadow-[0_4px_15px_rgba(185,28,28,0.3)] transition-all hover:-translate-y-0.5 hover:shadow-[0_6px_20px_rgba(185,28,28,0.5)]';
  }

  inputClasses(): string {
    return this.isLightTheme()
      ? 'w-full rounded-[12px] border border-[#d6903f] bg-[#fff8e3]/95 px-3 py-2.5 text-center font-pixel text-sm tracking-[0.2em] text-[#3e1f12] placeholder:text-[#7b4a2a]/60 transition-colors focus:border-[#8b1b1f] focus:outline-none focus:ring-4 focus:ring-[#d6903f]/20'
      : 'w-full rounded-[12px] border border-[#8a4b20]/40 bg-[#2a0805]/50 px-3 py-2.5 text-center font-pixel text-sm tracking-[0.2em] text-[#fff7d6] placeholder:text-[#8f7758]/50 focus:border-[#facc15]/50 focus:bg-[#2a0805]/80 focus:outline-none transition-colors';
  }

  secondaryButtonClasses(): string {
    return this.isLightTheme()
      ? 'font-pixel mt-5 inline-flex min-h-[2.75rem] w-full items-center justify-center rounded-[12px] border border-[#d6903f] bg-[#fff8e3]/90 px-4 py-2 text-[0.6rem] uppercase tracking-[0.15em] text-[#8b1b1f] transition-all hover:bg-[#fff1cf] hover:text-[#651014] disabled:opacity-50 disabled:hover:bg-[#fff8e3]/90 disabled:hover:text-[#8b1b1f]'
      : 'font-pixel mt-5 inline-flex min-h-[2.75rem] w-full items-center justify-center rounded-[12px] border border-[#8a4b20] bg-[#3b0905]/80 px-4 py-2 text-[0.6rem] uppercase tracking-[0.15em] text-[#d8b982] transition-all hover:bg-[#4a0b07] hover:text-[#fff7d6] disabled:opacity-50 disabled:hover:bg-[#3b0905]/80 disabled:hover:text-[#d8b982]';
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


  async copyCode(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.roomCode());
      this.copied.set(true);
      window.setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.copied.set(false);
    }
  }

  updateJoinCode(event: Event): void {
    this.joinCode.set((event.target as HTMLInputElement).value.toUpperCase());
  }

  attemptJoin(): void {
    if (!this.joinCode().trim()) {
      return;
    }

    this.joinAttempted.set(true);
    this.joinRoom.emit(this.joinCode().trim());
  }


}
