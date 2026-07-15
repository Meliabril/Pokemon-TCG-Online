import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';
import { AuthApiService } from '../../../../infrastructure/api/auth/auth-api.service';
import { AuthShellComponent } from '../../components/auth-shell/auth-shell.component';
import type { AuthTheme } from '../../components/auth-shell/auth-shell.component';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { AuthMediaService } from '../../data-access/auth-media.service';

const CODE_EXPIRATION_SECONDS = 300;
const RESEND_COOLDOWN_SECONDS = 60;
const ALERT_DURATION_MS = 3000;
const SUCCESS_REDIRECT_DELAY_MS = 5000;

@Component({
  selector: 'app-verify-account-page',
  imports: [ReactiveFormsModule, RouterLink, AuthShellComponent],
  templateUrl: './verify-account-page.component.html',
  styleUrl: './verify-account-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyAccountPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authApi = inject(AuthApiService);
  private readonly authMedia = inject(AuthMediaService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private readonly intervalId = window.setInterval(() => this.tick(), 1000);
  private redirectTimeoutId: number | null = null;
  private loadingIntervalId: number | null = null;
  private alertTimeoutId: number | null = null;

  readonly email = signal(this.route.snapshot.queryParamMap.get('email')?.trim() ?? '');
  readonly expirationSeconds = signal(CODE_EXPIRATION_SECONDS);
  readonly resendCooldown = signal(RESEND_COOLDOWN_SECONDS);
  readonly isVerifying = signal(false);
  readonly isResending = signal(false);
  readonly isSuccess = signal(false);
  readonly submitted = signal(false);
  readonly generalError = signal('');
  readonly successMessage = signal('');
  readonly loadingText = signal('');
  readonly verifyTheme = this.appTheme.theme;
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);

  readonly verifyForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
  });

  constructor() {
    if (!this.email()) {
      this.showGeneralError(
        this.t('AUTH.ERRORS.VERIFY_EMAIL_MISSING'),
      );
    }
  }

  ngOnDestroy(): void {
    window.clearInterval(this.intervalId);

    if (this.redirectTimeoutId !== null) {
      window.clearTimeout(this.redirectTimeoutId);
    }

    this.clearAlertTimeout();
    this.stopLoadingTextAnimation();
  }

  submit(): void {
    this.submitted.set(true);
    this.clearAlerts();

    if (!this.email()) {
      this.showGeneralError(
        this.t('AUTH.ERRORS.VERIFY_EMAIL_MISSING'),
      );
      return;
    }

    if (this.expirationSeconds() <= 0) {
      this.showGeneralError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
      return;
    }

    if (this.verifyForm.invalid) {
      this.verifyForm.markAllAsTouched();
      return;
    }

    this.isVerifying.set(true);

    this.authApi
      .verifyAccount({
        email: this.email(),
        code: this.verifyForm.controls.code.value,
      })
      .subscribe({
        next: () => {
          this.isVerifying.set(false);
          this.isSuccess.set(true);
          this.successMessage.set('');
          this.startSuccessLoadingScreen();
        },
        error: (error: unknown) => {
          this.isVerifying.set(false);
          this.applyHttpError(error);
        },
      });
  }

  resendCode(): void {
    this.clearAlerts();

    if (!this.email()) {
      this.showGeneralError(this.t('AUTH.ERRORS.VERIFY_EMAIL_MISSING'));
      return;
    }

    this.isResending.set(true);

    this.authApi.resendVerificationCode({ email: this.email() }).subscribe({
      next: () => {
        this.isResending.set(false);
        this.expirationSeconds.set(CODE_EXPIRATION_SECONDS);
        this.resendCooldown.set(RESEND_COOLDOWN_SECONDS);
        this.submitted.set(false);
        this.verifyForm.reset();
        this.showSuccessMessage(this.t('AUTH.MESSAGES.VERIFICATION_CODE_RESENT'));
      },
      error: (error: unknown) => {
        this.isResending.set(false);
        this.applyHttpError(error);
      },
    });
  }

  formattedExpiration(): string {
    const totalSeconds = Math.max(this.expirationSeconds(), 0);
    const minutes = Math.floor(totalSeconds / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (totalSeconds % 60).toString().padStart(2, '0');

    return `${minutes}:${seconds}`;
  }

  isVerifyDisabled(): boolean {
    return (
      this.isVerifying() ||
      this.isResending() ||
      this.isSuccess() ||
      this.expirationSeconds() <= 0 ||
      !this.email()
    );
  }

  isResendDisabled(): boolean {
    return (
      this.isResending() ||
      this.isVerifying() ||
      this.isSuccess() ||
      this.resendCooldown() > 0 ||
      !this.email()
    );
  }

  inputClasses(): string {
    const base =
      'h-10 w-full rounded-md border bg-white/90 px-3 text-center text-lg font-bold text-slate-950 shadow-sm outline-none transition-colors placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-[#8b070c] focus-visible:ring-offset-1 focus-visible:ring-offset-white/70 disabled:cursor-not-allowed disabled:bg-slate-50/90 disabled:text-slate-500';

    return this.hasCodeError()
      ? `${base} border-red-500 bg-red-50/90`
      : `${base} border-slate-300 hover:border-slate-500`;
  }

  hasCodeError(): boolean {
    const control = this.verifyForm.controls.code;
    return Boolean(control.invalid && (control.touched || control.dirty || this.submitted()));
  }

  codeError(): string | null {
    const control = this.verifyForm.controls.code;

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      return this.t('AUTH.VALIDATION.CODE_REQUIRED');
    }

    const code = control.value;

    if (/[^0-9]/.test(code)) {
      return this.t('AUTH.VALIDATION.CODE_NUMERIC');
    }

    if (code.length !== 6) {
      return this.t('AUTH.VALIDATION.CODE_PATTERN');
    }

    return this.t('AUTH.VALIDATION.CODE_VALID');
  }

  isDarkTheme(): boolean {
    return this.verifyTheme() === 'DARK';
  }

  setVerifyTheme(theme: AuthTheme): void {
    this.appTheme.setTheme(theme);
  }

  panelBodyClasses(): string {
    return this.isDarkTheme()
      ? 'auth-body auth-body--verify border-t border-white/15 px-6 py-5'
      : 'auth-body auth-body--verify border-t border-white/30 px-6 py-5';
  }

  descriptionClasses(): string {
    return this.isDarkTheme()
      ? 'text-sm font-medium leading-6 text-slate-100'
      : 'text-sm font-medium leading-6 text-slate-700';
  }

  emailTextClasses(): string {
    return this.isDarkTheme() ? 'font-semibold text-white' : 'font-semibold text-slate-950';
  }

  hintClasses(): string {
    return this.isDarkTheme()
      ? 'mt-2 rounded-md border border-white/15 bg-black/35 px-3 py-2 text-xs font-semibold text-slate-100 shadow-sm'
      : 'mt-2 rounded-md border border-white/50 bg-white/45 px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm';
  }

  timerCardClasses(): string {
    return this.isDarkTheme()
      ? 'mt-4 rounded-lg border border-white/15 bg-black/35 px-4 py-3 text-center shadow-sm backdrop-blur-sm'
      : 'mt-4 rounded-lg border border-white/50 bg-white/55 px-4 py-3 text-center shadow-sm backdrop-blur-sm';
  }

  timerLabelClasses(): string {
    return this.isDarkTheme()
      ? 'text-xs font-semibold uppercase text-slate-200'
      : 'text-xs font-semibold uppercase text-slate-600';
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

  resendSectionClasses(): string {
    return this.isDarkTheme()
      ? 'mt-4 border-t border-white/15 pt-4'
      : 'mt-4 border-t border-white/40 pt-4';
  }

  resendTextClasses(): string {
    return this.isDarkTheme()
      ? 'mb-2 text-center text-sm font-medium text-slate-100'
      : 'mb-2 text-center text-sm font-medium text-slate-700';
  }

  private tick(): void {
    const previousExpirationSeconds = this.expirationSeconds();

    if (previousExpirationSeconds > 0) {
      this.expirationSeconds.update((value) => value - 1);
    }

    if (this.resendCooldown() > 0) {
      this.resendCooldown.update((value) => value - 1);
    }

    if (previousExpirationSeconds > 0 && this.expirationSeconds() === 0 && !this.isSuccess()) {
      this.showGeneralError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
    }
  }

  private startSuccessLoadingScreen(): void {
    this.authMedia.playIfEnabled();
    this.startLoadingTextAnimation();

    this.redirectTimeoutId = window.setTimeout(() => {
      this.stopLoadingTextAnimation();
      void this.router.navigateByUrl(`/${APP_ROUTES.home}`);
    }, SUCCESS_REDIRECT_DELAY_MS);
  }

  private startLoadingTextAnimation(): void {
    this.stopLoadingTextAnimation();
    const loadingText = this.t('COMMON.LOADING_UPPER');

    let index = 1;
    this.loadingText.set(loadingText.slice(0, index));

    this.loadingIntervalId = window.setInterval(() => {
      index++;

      if (index > loadingText.length) {
        index = 1;
      }

      this.loadingText.set(loadingText.slice(0, index));
    }, 180);
  }

  private stopLoadingTextAnimation(): void {
    if (this.loadingIntervalId === null) {
      return;
    }

    window.clearInterval(this.loadingIntervalId);
    this.loadingIntervalId = null;
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
      this.showGeneralError(this.t('AUTH.ERRORS.VERIFY_ACCOUNT_DEFAULT'));
      return;
    }

    if (error.status === 0) {
      this.showGeneralError(
        this.t('AUTH.ERRORS.CONNECTION'),
      );
      return;
    }

    if (error.status === 400) {
      this.showGeneralError(this.t('AUTH.VALIDATION.CODE_VALID'));
      return;
    }

    if (error.status === 401) {
      this.showGeneralError(this.t('AUTH.ERRORS.CODE_INCORRECT'));
      return;
    }

    if (error.status === 403) {
      this.showGeneralError(this.t('AUTH.ERRORS.VERIFY_ACCOUNT_FORBIDDEN'));
      return;
    }

    if (error.status === 404) {
      this.showGeneralError(this.t('AUTH.ERRORS.ACCOUNT_NOT_FOUND'));
      return;
    }

    if (error.status === 409) {
      this.showGeneralError(this.t('AUTH.ERRORS.ACCOUNT_ALREADY_VERIFIED'));
      return;
    }

    if (error.status === 410) {
      this.expirationSeconds.set(0);
      this.showGeneralError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
      return;
    }

    if (error.status === 429) {
      this.showGeneralError(this.t('AUTH.ERRORS.TOO_MANY_ATTEMPTS'));
      return;
    }

    if (error.status >= 500) {
      this.showGeneralError(
        this.t('AUTH.ERRORS.SERVER'),
      );
      return;
    }

    this.showGeneralError(this.t('AUTH.ERRORS.VERIFY_ACCOUNT_DEFAULT'));
  }
}
