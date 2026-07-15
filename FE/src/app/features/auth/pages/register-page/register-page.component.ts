import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RegisterRequest } from '../../../../core/models/interfaces/auth/register-user.interface';
import { LanguageService } from '../../../../core/services/language.service';
import { AuthApiService } from '../../../../infrastructure/api/auth/auth-api.service';
import { AuthShellComponent } from '../../components/auth-shell/auth-shell.component';
import type { AuthTheme } from '../../components/auth-shell/auth-shell.component';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import {
  AvatarPickerComponent,
  DEFAULT_AVATAR_ID,
} from '../../../../shared/ui/profile/avatar-picker/avatar-picker.component';

type RegisterField = 'username' | 'email' | 'password' | 'confirmPassword';
type RegisterFieldErrors = Partial<Record<RegisterField, string>>;
type MobileAuthMode = 'menu' | 'register';

const STRONG_PASSWORD_PATTERN = /^(?=.*\p{Lu})(?=.*\d)(?=.*[^\p{L}\p{N}]).+$/u;
const ALERT_DURATION_MS = 3000;
const MOBILE_AUTH_MODE_STORAGE_KEY = 'pokemon-tcg-mobile-auth-mode';

function strongPasswordValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value;

  if (!value || STRONG_PASSWORD_PATTERN.test(value)) {
    return null;
  }

  return { strongPassword: true };
}

function matchingPasswordsValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword || password === confirmPassword) {
    return null;
  }

  return { passwordMismatch: true };
}

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, AvatarPickerComponent, AuthShellComponent],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private alertTimeoutId: number | null = null;

  readonly isSubmitting = signal(false);
  readonly isSuccess = signal(false);
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly submitted = signal(false);
  readonly generalError = signal('');
  readonly successMessage = signal('');
  readonly backendFieldErrors = signal<RegisterFieldErrors>({});
  readonly mobileAuthMode = signal<MobileAuthMode>(this.initialMobileAuthMode());
  readonly selectedAvatarId = signal(DEFAULT_AVATAR_ID);
  readonly registerTheme = this.appTheme.theme;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly registerForm = this.fb.nonNullable.group(
    {
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchingPasswordsValidator },
  );

  ngOnDestroy(): void {
    this.clearAlertTimeout();
  }

  submit(): void {
    this.submitted.set(true);
    this.clearAlerts();
    this.backendFieldErrors.set({});

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    this.isSubmitting.set(true);

    this.authApi.register(request).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.isSuccess.set(true);
        this.showSuccessMessage(response.message || this.t('AUTH.MESSAGES.VERIFY_EMAIL_SENT'));
        void this.router.navigate(['/auth/verify-account'], {
          queryParams: { email: response.email || request.email },
        });
      },
      error: (error: unknown) => {
        this.isSubmitting.set(false);
        this.applyHttpError(error);
      },
    });
  }

  inputClasses(field: RegisterField): string {
    const base =
      'h-9 w-full rounded-md border bg-white/90 px-3 text-sm text-slate-950 shadow-sm outline-none transition-colors placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-[#8b070c] focus-visible:ring-offset-1 focus-visible:ring-offset-white/70';

    return this.hasFieldError(field) || (field === 'confirmPassword' && this.hasPasswordMismatch())
      ? `${base} border-red-500 bg-red-50/90`
      : `${base} border-slate-300 hover:border-slate-500`;
  }

  hasFieldError(field: RegisterField): boolean {
    const control = this.registerForm.controls[field];
    return Boolean(
      this.backendFieldErrors()[field] ||
      (control.invalid && (control.touched || control.dirty || this.submitted())),
    );
  }

  fieldError(field: RegisterField): string | null {
    const backendError = this.backendFieldErrors()[field];

    if (backendError) {
      return backendError;
    }

    const control = this.registerForm.controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      return this.requiredMessage(field);
    }

    if (control.hasError('minlength')) {
      return field === 'password'
        ? this.t('AUTH.VALIDATION.PASSWORD_MIN')
        : this.t('AUTH.VALIDATION.IDENTIFIER_MIN');
    }

    if (control.hasError('email')) {
      return this.t('AUTH.VALIDATION.EMAIL_FORMAT');
    }

    if (control.hasError('strongPassword')) {
      return this.t('AUTH.VALIDATION.PASSWORD_STRONG');
    }

    return null;
  }

  hasPasswordMismatch(): boolean {
    const confirmPassword = this.registerForm.controls.confirmPassword;
    return Boolean(
      this.registerForm.hasError('passwordMismatch') &&
      (confirmPassword.touched || confirmPassword.dirty || this.submitted()),
    );
  }

  confirmPasswordError(): string | null {
    return (
      this.fieldError('confirmPassword') ??
      (this.hasPasswordMismatch() ? this.t('AUTH.VALIDATION.PASSWORD_MISMATCH') : null)
    );
  }

  isDarkTheme(): boolean {
    return this.registerTheme() === 'DARK';
  }

  setRegisterTheme(theme: AuthTheme): void {
    this.appTheme.setTheme(theme);
  }

  showMobileLogin(event?: Event): void {
    this.stopAuthViewEvent(event);
    this.saveMobileAuthMode('login');
    void this.router.navigate(['/auth/login'], {
      queryParams: { mobileAuth: 'login' },
    });
  }

  showMobileRegister(event?: Event): void {
    this.stopAuthViewEvent(event);
    this.mobileAuthMode.set('register');
    this.saveMobileAuthMode('register');
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mobileAuth: 'register' },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  backToMobileMenu(event?: Event): void {
    this.stopAuthViewEvent(event);
    this.mobileAuthMode.set('menu');
    this.clearMobileAuthMode();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mobileAuth: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  panelBodyClasses(): string {
    return this.isDarkTheme()
      ? 'auth-body auth-body--register border-t border-white/15 px-6 py-5'
      : 'auth-body auth-body--register border-t border-white/30 px-6 py-5';
  }

  labelClasses(): string {
    return this.isDarkTheme()
      ? 'flex items-center gap-1.5 text-sm font-semibold text-white'
      : 'flex items-center gap-1.5 text-sm font-semibold text-slate-900';
  }

  requiredMarkClasses(): string {
    return this.isDarkTheme() ? 'text-red-300' : 'text-[#8b070c]';
  }

  fieldErrorClasses(): string {
    return this.isDarkTheme()
      ? 'text-xs font-medium text-red-200'
      : 'text-xs font-medium text-red-700';
  }

  authPromptClasses(): string {
    return this.isDarkTheme()
      ? 'mt-3 text-center text-sm font-medium text-slate-100'
      : 'mt-3 text-center text-sm font-medium text-slate-700';
  }

  authLinkClasses(): string {
    return this.isDarkTheme()
      ? 'font-semibold text-red-200 hover:text-white hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white'
      : 'font-semibold text-[#8b070c] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c]';
  }

  private buildRequest(): RegisterRequest {
    const value = this.registerForm.getRawValue();

    return {
      email: value.email.trim(),
      username: value.username.trim(),
      password: value.password,
      avatar: this.selectedAvatarId(),
    };
  }

  private initialMobileAuthMode(): MobileAuthMode {
    const requestedMode = this.route.snapshot.queryParamMap.get('mobileAuth') ?? this.storedMobileAuthMode();

    return requestedMode === 'register' ? 'register' : 'menu';
  }

  private saveMobileAuthMode(mode: 'login' | 'register'): void {
    window.sessionStorage.setItem(MOBILE_AUTH_MODE_STORAGE_KEY, mode);
  }

  private storedMobileAuthMode(): string | null {
    return window.sessionStorage.getItem(MOBILE_AUTH_MODE_STORAGE_KEY);
  }

  private clearMobileAuthMode(): void {
    window.sessionStorage.removeItem(MOBILE_AUTH_MODE_STORAGE_KEY);
  }

  private stopAuthViewEvent(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
  }

  private requiredMessage(field: RegisterField): string {
    const messages: Record<RegisterField, string> = {
      username: this.t('AUTH.VALIDATION.USERNAME_REQUIRED'),
      email: this.t('AUTH.VALIDATION.EMAIL_REQUIRED'),
      password: this.t('AUTH.VALIDATION.PASSWORD_REQUIRED'),
      confirmPassword: this.t('AUTH.VALIDATION.CONFIRM_PASSWORD_REQUIRED'),
    };

    return messages[field];
  }

  private showGeneralError(message: string): void {
    this.generalError.set(message);
    this.successMessage.set('');
    this.scheduleAlertClear();
  }

  private showSuccessMessage(message: string): void {
    this.successMessage.set(message);
    this.generalError.set('');
    this.scheduleAlertClear();
  }

  private clearAlerts(): void {
    this.clearAlertTimeout();
    this.generalError.set('');
    this.successMessage.set('');
  }

  private scheduleAlertClear(): void {
    this.clearAlertTimeout();

    this.alertTimeoutId = window.setTimeout(() => {
      this.generalError.set('');
      this.successMessage.set('');
      this.alertTimeoutId = null;
    }, ALERT_DURATION_MS);
  }

  private clearAlertTimeout(): void {
    if (this.alertTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.alertTimeoutId);
    this.alertTimeoutId = null;
  }

  private applyHttpError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.showGeneralError(this.t('AUTH.ERRORS.REGISTER_DEFAULT'));
      return;
    }

    const bodyText = JSON.stringify(error.error ?? '').toLowerCase();

    if (error.status === 0) {
      this.showGeneralError(this.t('AUTH.ERRORS.REGISTER_CONNECTION'));
      return;
    }

    if (error.status === 409) {
      if (bodyText.includes('username') || bodyText.includes('nombre')) {
        this.backendFieldErrors.set({ username: this.t('AUTH.ERRORS.USERNAME_TAKEN') });
      } else {
        this.backendFieldErrors.set({ email: this.t('AUTH.ERRORS.EMAIL_TAKEN') });
      }
      this.showGeneralError(this.t('AUTH.ERRORS.CHECK_MARKED'));
      return;
    }

    if (error.status === 400) {
      this.showGeneralError(this.t('AUTH.ERRORS.CHECK_DATA'));
      return;
    }

    if (error.status >= 500) {
      this.showGeneralError(this.t('AUTH.ERRORS.SERVER_SHORT'));
      return;
    }

    this.showGeneralError(this.t('AUTH.ERRORS.REGISTER_DEFAULT'));
  }
}
