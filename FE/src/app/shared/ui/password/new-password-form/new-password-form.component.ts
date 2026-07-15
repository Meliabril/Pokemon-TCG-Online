import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { LanguageService } from '../../../../core/services/language.service';
import {
  NewPasswordFormGroup,
  PasswordFormVariant
} from '../password-form.types';

type NewPasswordField = 'newPassword' | 'confirmPassword';

@Component({
  selector: 'app-new-password-form',
  imports: [ReactiveFormsModule],
  templateUrl: './new-password-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewPasswordFormComponent {
  private readonly languageService = inject(LanguageService);

  readonly form = input.required<NewPasswordFormGroup>();
  readonly variant = input<PasswordFormVariant>('auth');
  readonly submitted = input(false);
  readonly isSubmitting = input(false);
  readonly submitDisabled = input(false);
  readonly description = input('');
  readonly submitLabel = input('');
  readonly submittingLabel = input('');
  readonly newPasswordExternalError = input('');
  readonly confirmPasswordExternalError = input('');

  readonly formSubmitted = output<void>();

  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  fieldError(field: NewPasswordField): string {
    const externalError =
      field === 'newPassword'
        ? this.newPasswordExternalError()
        : this.confirmPasswordExternalError();

    if (externalError) {
      return externalError;
    }

    const control = this.form().controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return '';
    }

    if (control.hasError('required')) {
      return field === 'newPassword'
        ? this.t('AUTH.VALIDATION.NEW_PASSWORD_REQUIRED')
        : this.t('AUTH.VALIDATION.CONFIRM_PASSWORD_REQUIRED_ALT');
    }

    if (field === 'newPassword') {
      if (control.hasError('minlength')) {
        return this.t('AUTH.VALIDATION.PASSWORD_STRONG_FULL');
      }

      if (control.hasError('strongPassword')) {
        return this.t('AUTH.VALIDATION.PASSWORD_STRONG_FULL');
      }
    }

    return '';
  }

  confirmPasswordError(): string {
    const confirmPassword = this.form().controls.confirmPassword;
    const mismatch = Boolean(
      this.form().hasError('passwordMismatch') &&
        (confirmPassword.touched || confirmPassword.dirty || this.submitted())
    );

    return this.fieldError('confirmPassword') || (mismatch ? this.t('AUTH.VALIDATION.PASSWORD_MISMATCH') : '');
  }

  hasError(field: NewPasswordField): boolean {
    return field === 'confirmPassword'
      ? !!this.confirmPasswordError()
      : !!this.fieldError(field);
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

  inputClasses(hasError: boolean): string {
    const base =
      'h-9 w-full rounded-md border px-3 text-sm shadow-sm outline-none transition-colors focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-70';

    if (this.variant() === 'profile') {
      if (this.theme() === 'LIGHT') {
        const lightBase = `${base} bg-[#fff8e3]/90 text-[#3b1d12] placeholder:text-[#8a6248]/60 focus-visible:ring-[#B8792E]/50`;
        return hasError
          ? `${lightBase} border-[#dc2626]`
          : `${lightBase} border-[#8a4b20]/60 hover:border-[#8a4b20]`;
      }
      const darkBase = `${base} bg-[#2a0805]/80 text-[#fff7d6] placeholder:text-[#d8b982]/60 focus-visible:ring-[#facc15]/70`;
      return hasError
        ? `${darkBase} border-[#dc2626]`
        : `${darkBase} border-[#8a4b20] hover:border-[#facc15]/50`;
    }

    const authBase = `${base} bg-white/90 text-slate-950 placeholder:text-slate-400 focus-visible:ring-[#8b070c] focus-visible:ring-offset-1 focus-visible:ring-offset-white/70`;
    return hasError
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

  toggleButtonClasses(): string {
    if (this.variant() === 'profile') {
      return this.theme() === 'LIGHT'
        ? 'absolute right-1 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-md text-[#6b3a22] transition-colors hover:bg-[#8a4b20]/10 hover:text-[#4A2416] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8a4b20]'
        : 'absolute right-1 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-md text-[#d8b982] transition-colors hover:bg-[#8a4b20]/20 hover:text-[#fff7d6] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#facc15]';
    }
    return 'absolute right-1 top-1/2 grid h-8 w-8 -translate-y-1/2 place-items-center rounded-md text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c]';
  }
}
