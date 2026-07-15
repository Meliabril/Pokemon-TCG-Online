import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { LanguageService } from '../../../../core/services/language.service';
import {
  PasswordCodeFormGroup,
  PasswordFormVariant
} from '../password-form.types';

@Component({
  selector: 'app-password-code-form',
  imports: [ReactiveFormsModule],
  templateUrl: './password-code-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PasswordCodeFormComponent {
  private readonly languageService = inject(LanguageService);

  readonly form = input.required<PasswordCodeFormGroup>();
  readonly variant = input<PasswordFormVariant>('auth');
  readonly submitted = input(false);
  readonly isSubmitting = input(false);
  readonly isResending = input(false);
  readonly submitDisabled = input(false);
  readonly resendDisabled = input(false);
  readonly showTimer = input(false);
  readonly showResend = input(true);
  readonly formattedExpiration = input('');
  readonly resendCooldownSeconds = input(0);
  readonly description = input('');
  readonly submitLabel = input('');
  readonly submittingLabel = input('');
  readonly cancelLabel = input('');
  readonly externalError = input('');
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly formSubmitted = output<void>();
  readonly resendRequested = output<void>();
  readonly cancelled = output<void>();

  codeError(): string {
    if (this.externalError()) {
      return this.externalError();
    }

    const control = this.form().controls.code;

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return '';
    }

    if (control.hasError('required')) {
      return this.t('AUTH.VALIDATION.CODE_REQUIRED');
    }

    if (/[^0-9]/.test(control.value)) {
      return this.t('AUTH.VALIDATION.CODE_NUMERIC');
    }

    if (control.hasError('pattern')) {
      return this.t('AUTH.VALIDATION.CODE_PATTERN');
    }

    return this.t('AUTH.VALIDATION.CODE_VALID');
  }

  readonly theme = input<'LIGHT' | 'DARK'>('LIGHT');

  labelClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'flex items-center gap-1.5 text-sm font-semibold text-[#4A2416]'
        : 'flex items-center gap-1.5 text-sm font-semibold text-[#fff7d6]';
    }
    return this.theme() === 'DARK'
      ? 'flex items-center gap-1.5 text-sm font-semibold text-white'
      : 'flex items-center gap-1.5 text-sm font-semibold text-slate-900';
  }

  inputClasses(): string {
    const base =
      'h-9 w-full rounded-md border px-3 text-sm shadow-sm outline-none transition-colors focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-70';

    if (this.variant() === 'profile') {
      if (this.theme() === 'LIGHT') {
        const lightBase = `${base} bg-[#fff8e3]/90 text-[#3b1d12] placeholder:text-[#8a6248]/60 focus-visible:ring-[#B8792E]/50`;
        return this.codeError()
          ? `${lightBase} border-[#dc2626]`
          : `${lightBase} border-[#8a4b20]/60 hover:border-[#8a4b20]`;
      }
      const darkBase = `${base} bg-[#2a0805]/80 text-[#fff7d6] placeholder:text-[#d8b982]/60 focus-visible:ring-[#facc15]/70`;
      return this.codeError()
        ? `${darkBase} border-[#dc2626]`
        : `${darkBase} border-[#8a4b20] hover:border-[#facc15]/50`;
    }

    const authBase = `${base} bg-white/90 text-slate-950 placeholder:text-slate-400 focus-visible:ring-[#8b070c] focus-visible:ring-offset-1 focus-visible:ring-offset-white/70`;
    return this.codeError()
      ? `${authBase} border-red-500 bg-red-50/90`
      : `${authBase} border-slate-300 hover:border-slate-500`;
  }

  errorClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'text-xs font-semibold text-[#b91c1c]'
        : 'text-xs font-semibold text-[#fecaca]';
    }
    return 'text-xs font-medium text-red-700';
  }

  descriptionClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'text-sm leading-6 text-[#6b3a22]'
        : 'text-sm leading-6 text-[#d8b982]';
    }
    return this.theme() === 'DARK'
      ? 'text-sm font-medium leading-6 text-white'
      : 'text-sm font-medium leading-6 text-slate-700';
  }

  resendTextClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'mb-2 text-center text-sm font-medium text-[#5A2B12]'
        : 'mb-2 text-center text-sm font-medium text-[#d8b982]';
    }
    return this.theme() === 'DARK'
      ? 'mb-2 text-center text-sm font-medium text-white'
      : 'mb-2 text-center text-sm font-medium text-slate-700';
  }

  timerBoxClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'mt-4 rounded-lg border border-[#8a4b20]/35 bg-[#fff8e3]/70 px-4 py-3 text-center shadow-sm backdrop-blur-sm'
        : 'mt-4 rounded-lg border border-[#8a4b20]/50 bg-[#2a0805]/60 px-4 py-3 text-center shadow-sm backdrop-blur-sm';
    }
    return 'mt-4 rounded-lg border border-white/50 bg-white/55 px-4 py-3 text-center shadow-sm backdrop-blur-sm';
  }

  timerLabelClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'text-xs font-semibold uppercase text-[#6b3a22]'
        : 'text-xs font-semibold uppercase text-[#d8b982]';
    }
    return 'text-xs font-semibold uppercase text-slate-600';
  }

  timerValueClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'mt-1 text-3xl font-bold tabular-nums text-[#8b1b1f]'
        : 'mt-1 text-3xl font-bold tabular-nums text-[#fde68a]';
    }
    return 'mt-1 text-3xl font-bold tabular-nums text-[#8b070c]';
  }

  dividerClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'mt-4 border-t border-[#8a4b20]/30 pt-4'
        : 'mt-4 border-t border-[#8a4b20]/40 pt-4';
    }
    return 'mt-4 border-t border-white/40 pt-4';
  }

  resendButtonClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-md border border-[#8a4b20]/60 bg-[#fff8e3]/85 px-4 py-2 text-sm font-bold text-[#6b2812] shadow-sm transition-colors hover:bg-[#fff1cf]/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8a4b20] disabled:cursor-not-allowed disabled:opacity-60'
        : 'inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-md border border-[#8a4b20]/60 bg-[#2a0805]/60 px-4 py-2 text-sm font-bold text-[#d8b982] shadow-sm transition-colors hover:bg-[#3b0905]/70 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#d8b982] disabled:cursor-not-allowed disabled:opacity-60';
    }
    return 'inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-md border border-[#8b070c] bg-white/80 px-4 py-2 text-sm font-bold text-[#8b070c] shadow-sm transition-colors hover:bg-red-50/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c] focus-visible:ring-offset-2 focus-visible:ring-offset-white/70 disabled:cursor-not-allowed disabled:opacity-60';
  }

  cancelButtonClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'mt-3 inline-flex min-h-9 w-full items-center justify-center bg-transparent px-3 py-2 text-sm font-semibold text-[#6b2812] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8a4b20] disabled:cursor-not-allowed disabled:opacity-60'
        : 'mt-3 inline-flex min-h-9 w-full items-center justify-center bg-transparent px-3 py-2 text-sm font-semibold text-[#d8b982] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#d8b982] disabled:cursor-not-allowed disabled:opacity-60';
    }
    return 'mt-3 inline-flex min-h-9 w-full items-center justify-center bg-transparent px-3 py-2 text-sm font-semibold text-[#8b070c] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c] disabled:cursor-not-allowed disabled:opacity-60';
  }
}
