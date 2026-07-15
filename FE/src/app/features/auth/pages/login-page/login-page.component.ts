import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnDestroy,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { LoginRequest } from '../../../../core/models/interfaces/auth/auth-session.interface';
import { LanguageService } from '../../../../core/services/language.service';
import {
  matchingPasswordsValidator,
  strongPasswordValidator,
} from '../../../../core/validators/password.validators';
import { AuthApiService } from '../../../../infrastructure/api/auth/auth-api.service';
import { NewPasswordFormComponent } from '../../../../shared/ui/password/new-password-form/new-password-form.component';
import { PasswordCodeFormComponent } from '../../../../shared/ui/password/password-code-form/password-code-form.component';
import { AuthShellComponent } from '../../components/auth-shell/auth-shell.component';
import type { AuthTheme } from '../../components/auth-shell/auth-shell.component';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { AuthMediaService } from '../../data-access/auth-media.service';

type AuthModalMode = 'login' | 'forgot-password' | 'verify-code' | 'change-password';
type MobileAuthMode = 'menu' | 'login';
type LoginField = 'identifier' | 'password';
type ForgotPasswordField = 'email';
type VerifyCodeField = 'code';
type ResetPasswordField = 'newPassword' | 'confirmPassword';

const CODE_EXPIRATION_SECONDS = 300;
const RESEND_COOLDOWN_SECONDS = 60;
const SUCCESS_MESSAGE_DURATION_MS = 3000;
const SUCCESS_REDIRECT_DELAY_MS = 5000;
const MOBILE_AUTH_MODE_STORAGE_KEY = 'pokemon-tcg-mobile-auth-mode';
@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    AuthShellComponent,
    PasswordCodeFormComponent,
    NewPasswordFormComponent,
  ],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authMedia = inject(AuthMediaService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);
  private redirectTimeoutId: number | null = null;
  private loadingIntervalId: number | null = null;
  private recoveryIntervalId: number | null = null;
  private successMessageTimeoutId: number | null = null;
  private errorMessageTimeoutId: number | null = null;
  private hasRecoveryHistoryEntry = false;

  readonly mode = signal<AuthModalMode>('login');
  readonly mobileAuthMode = signal<MobileAuthMode>(this.initialMobileAuthMode());
  readonly recoveryEmail = signal('');
  readonly recoveryVerificationToken = signal('');
  readonly codeExpiresInSeconds = signal(CODE_EXPIRATION_SECONDS);
  readonly resendCooldownSeconds = signal(0);
  readonly isSubmitting = signal(false);
  readonly isRequestingCode = signal(false);
  readonly isVerifyingCode = signal(false);
  readonly isResettingPassword = signal(false);
  readonly isResendingCode = signal(false);
  readonly isSuccess = signal(false);
  readonly showPassword = signal(false);
  readonly submitted = signal(false);
  readonly forgotSubmitted = signal(false);
  readonly codeSubmitted = signal(false);
  readonly resetSubmitted = signal(false);
  readonly generalError = signal('');
  readonly successMessage = signal('');
  readonly recoveryCodeFieldError = signal('');
  readonly resetPasswordFieldError = signal('');
  readonly loginTheme = this.appTheme.theme;
  readonly loadingText = signal('');
  readonly successText = () => this.t('AUTH.SUCCESS_TEXT');
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  readonly loginForm = this.fb.nonNullable.group({
    identifier: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [true],
  });

  readonly forgotPasswordForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  readonly verifyCodeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  readonly resetPasswordForm = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchingPasswordsValidator },
  );

  ngOnDestroy(): void {
    if (this.redirectTimeoutId !== null) {
      window.clearTimeout(this.redirectTimeoutId);
    }

    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
    this.stopRecoveryTimers();
    this.stopLoadingTextAnimation();
  }

  @HostListener('window:popstate')
  handleBrowserBack(): void {
    if (this.mode() !== 'change-password') {
      return;
    }

    this.hasRecoveryHistoryEntry = false;
    this.backToLogin();
  }

  submit(): void {
    this.submitted.set(true);
    this.clearFeedback();

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    this.authApi.login(this.buildRequest(), this.loginForm.controls.rememberMe.value).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.isSuccess.set(true);
        this.successMessage.set('');
        this.startSuccessLoadingScreen();
      },
      error: (error: unknown) => {
        this.isSubmitting.set(false);
        this.applyLoginHttpError(error);
      },
    });
  }

  submitForgotPassword(): void {
    this.forgotSubmitted.set(true);
    this.clearFeedback();

    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    const email = this.forgotPasswordForm.controls.email.value.trim();
    this.isRequestingCode.set(true);

    this.authApi.forgotPassword({ email }).subscribe({
      next: (response) => {
        this.isRequestingCode.set(false);
        this.recoveryEmail.set(email);
        this.recoveryVerificationToken.set('');
        this.verifyCodeForm.reset();
        this.resetPasswordForm.reset();
        this.codeSubmitted.set(false);
        this.resetSubmitted.set(false);
        this.startRecoveryTimers();
        this.mode.set('verify-code');
        this.setSuccessMessage(this.t('AUTH.MESSAGES.RECOVERY_SENT'));
      },
      error: (error: unknown) => {
        this.isRequestingCode.set(false);
        this.applyForgotPasswordHttpError(error);
      },
    });
  }

  submitRecoveryCode(): void {
    this.codeSubmitted.set(true);
    this.clearFeedback();

    if (this.codeExpiresInSeconds() <= 0) {
      this.setGeneralError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
      return;
    }

    if (!this.recoveryEmail()) {
      this.mode.set('forgot-password');
      this.setGeneralError(this.t('AUTH.MESSAGES.EMAIL_REQUIRED_FOR_CODE'));
      return;
    }

    if (this.verifyCodeForm.invalid) {
      this.verifyCodeForm.markAllAsTouched();
      return;
    }

    this.isVerifyingCode.set(true);

    this.authApi
      .verifyPasswordResetCode({
        email: this.recoveryEmail(),
        code: this.verifyCodeForm.controls.code.value,
      })
      .subscribe({
        next: (response) => {
          this.isVerifyingCode.set(false);
          this.recoveryCodeFieldError.set('');
          this.recoveryVerificationToken.set(response.verificationToken);
          this.resetPasswordForm.reset();
          this.resetSubmitted.set(false);
          this.stopRecoveryTimers();
          this.mode.set('change-password');
          this.pushRecoveryHistoryEntry();
          this.setSuccessMessage(
            this.t('AUTH.MESSAGES.CODE_VERIFIED'),
          );
        },
        error: (error: unknown) => {
          this.isVerifyingCode.set(false);
          this.applyVerifyCodeHttpError(error);
        },
      });
  }

  submitResetPassword(): void {
    this.resetSubmitted.set(true);
    this.clearFeedback();

    if (!this.recoveryEmail()) {
      this.mode.set('forgot-password');
      this.setGeneralError(this.t('AUTH.MESSAGES.EMAIL_REQUIRED_FOR_CODE'));
      return;
    }

    if (!this.recoveryVerificationToken()) {
      this.mode.set('verify-code');
      this.setGeneralError(this.t('AUTH.MESSAGES.RECOVERY_CODE_REQUIRED'));
      return;
    }

    if (this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    const value = this.resetPasswordForm.getRawValue();
    this.isResettingPassword.set(true);

    this.authApi
      .resetVerifiedPassword({
        verificationToken: this.recoveryVerificationToken(),
        newPassword: value.newPassword,
        confirmPassword: value.confirmPassword,
      })
      .subscribe({
        next: () => {
          this.isResettingPassword.set(false);
          this.returnToLoginAfterReset();
        },
        error: (error: unknown) => {
          this.isResettingPassword.set(false);
          this.applyResetPasswordHttpError(error);
        },
      });
  }

  resendRecoveryCode(): void {
    this.clearFeedback();

    if (!this.recoveryEmail()) {
      this.mode.set('forgot-password');
      this.setGeneralError(this.t('AUTH.MESSAGES.EMAIL_REQUIRED_FOR_CODE'));
      return;
    }

    this.isResendingCode.set(true);

    this.authApi.forgotPassword({ email: this.recoveryEmail() }).subscribe({
      next: (response) => {
        this.isResendingCode.set(false);
        this.recoveryVerificationToken.set('');
        this.verifyCodeForm.reset();
        this.resetPasswordForm.reset();
        this.codeSubmitted.set(false);
        this.resetSubmitted.set(false);
        this.startRecoveryTimers();
        this.mode.set('verify-code');
        this.setSuccessMessage(this.t('AUTH.MESSAGES.RECOVERY_SENT'));
      },
      error: (error: unknown) => {
        this.isResendingCode.set(false);
        this.applyForgotPasswordHttpError(error);
      },
    });
  }

  showForgotPassword(): void {
    this.clearFeedback();
    this.stopRecoveryTimers();
    this.mode.set('forgot-password');
    const currentIdentifier = this.loginForm.controls.identifier.value.trim();

    if (currentIdentifier.includes('@')) {
      this.forgotPasswordForm.controls.email.setValue(currentIdentifier);
    }
  }

  backToLogin(): void {
    this.mode.set('login');
    this.clearRecoveryState();
  }

  inputClasses(field: LoginField): string {
    return this.authInputClasses(this.hasFieldError(field));
  }

  recoveryInputClasses(hasError: boolean): string {
    return this.authInputClasses(hasError);
  }

  hasFieldError(field: LoginField): boolean {
    const control = this.loginForm.controls[field];
    return Boolean(control.invalid && (control.touched || control.dirty || this.submitted()));
  }

  hasForgotFieldError(field: ForgotPasswordField): boolean {
    const control = this.forgotPasswordForm.controls[field];
    return Boolean(
      control.invalid && (control.touched || control.dirty || this.forgotSubmitted()),
    );
  }

  hasVerifyCodeFieldError(field: VerifyCodeField): boolean {
    const control = this.verifyCodeForm.controls[field];
    return Boolean(control.invalid && (control.touched || control.dirty || this.codeSubmitted()));
  }

  hasResetFieldError(field: ResetPasswordField): boolean {
    const control = this.resetPasswordForm.controls[field];
    return Boolean(control.invalid && (control.touched || control.dirty || this.resetSubmitted()));
  }

  fieldError(field: LoginField): string | null {
    const control = this.loginForm.controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      return field === 'identifier'
        ? this.t('AUTH.VALIDATION.IDENTIFIER_REQUIRED')
        : this.t('AUTH.VALIDATION.PASSWORD_REQUIRED');
    }

    if (control.hasError('minlength')) {
      return field === 'identifier'
        ? this.t('AUTH.VALIDATION.IDENTIFIER_MIN')
        : this.t('AUTH.VALIDATION.PASSWORD_MIN');
    }

    return null;
  }

  forgotFieldError(field: ForgotPasswordField): string | null {
    const control = this.forgotPasswordForm.controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.forgotSubmitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      return this.t('AUTH.VALIDATION.EMAIL_REQUIRED');
    }

    if (control.hasError('email')) {
      return this.t('AUTH.VALIDATION.EMAIL_FORMAT');
    }

    return null;
  }

  verifyCodeFieldError(field: VerifyCodeField): string | null {
    const control = this.verifyCodeForm.controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.codeSubmitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      return this.t('AUTH.VALIDATION.RECOVERY_CODE_REQUIRED');
    }

    const code = control.value;

    if (/[^0-9]/.test(code)) {
      return this.t('AUTH.VALIDATION.CODE_NUMERIC');
    }

    if (control.hasError('pattern')) {
      return this.t('AUTH.VALIDATION.CODE_PATTERN');
    }

    return null;
  }

  resetFieldError(field: ResetPasswordField): string | null {
    const control = this.resetPasswordForm.controls[field];

    if (!(control.invalid && (control.touched || control.dirty || this.resetSubmitted()))) {
      return null;
    }

    if (control.hasError('required')) {
      const messages: Record<ResetPasswordField, string> = {
        newPassword: this.t('AUTH.VALIDATION.NEW_PASSWORD_REQUIRED'),
        confirmPassword: this.t('AUTH.VALIDATION.CONFIRM_PASSWORD_REQUIRED'),
      };

      return messages[field];
    }

    if (field === 'newPassword') {
      if (control.hasError('minlength')) {
        return this.t('AUTH.VALIDATION.PASSWORD_MIN');
      }

      if (control.hasError('strongPassword')) {
        return this.t('AUTH.VALIDATION.PASSWORD_STRONG');
      }
    }

    return null;
  }

  hasResetPasswordMismatch(): boolean {
    const confirmPassword = this.resetPasswordForm.controls.confirmPassword;
    return Boolean(
      this.resetPasswordForm.hasError('passwordMismatch') &&
        (confirmPassword.touched || confirmPassword.dirty || this.resetSubmitted()),
    );
  }

  confirmPasswordError(): string | null {
    return (
      this.resetFieldError('confirmPassword') ??
      (this.hasResetPasswordMismatch() ? this.t('AUTH.VALIDATION.PASSWORD_MISMATCH') : null)
    );
  }

  isDarkTheme(): boolean {
    return this.loginTheme() === 'DARK';
  }

  setLoginTheme(theme: AuthTheme): void {
    this.appTheme.setTheme(theme);
  }

  authTitle(): string {
    const titles: Record<AuthModalMode, string> = {
      login: this.t('AUTH.LOGIN_TITLE'),
      'forgot-password': this.t('AUTH.FORGOT_PASSWORD_TITLE'),
      'verify-code': this.t('AUTH.VERIFY_CODE_TITLE'),
      'change-password': this.t('AUTH.CHANGE_PASSWORD_TITLE'),
    };

    return titles[this.mode()];
  }

  authTitleId(): string {
    return `${this.mode()}-title`;
  }

  authFormId(): string {
    return `${this.mode()}-form`;
  }

  formattedExpiration(): string {
    const totalSeconds = Math.max(this.codeExpiresInSeconds(), 0);
    const minutes = Math.floor(totalSeconds / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (totalSeconds % 60).toString().padStart(2, '0');

    return `${minutes}:${seconds}`;
  }

  isResetSubmitDisabled(): boolean {
    return (
      this.isResettingPassword() ||
      !this.recoveryEmail() ||
      !this.recoveryVerificationToken()
    );
  }

  isResendDisabled(): boolean {
    return (
      this.isResendingCode() ||
      this.isVerifyingCode() ||
      this.isResettingPassword() ||
      this.resendCooldownSeconds() > 0 ||
      !this.recoveryEmail()
    );
  }

  panelBodyClasses(): string {
    return this.isDarkTheme()
      ? 'auth-body auth-body--login border-t border-white/15 px-6 py-5'
      : 'auth-body auth-body--login border-t border-white/30 px-6 py-5';
  }

  showMobileLogin(event?: Event): void {
    this.stopAuthViewEvent(event);
    this.mode.set('login');
    this.mobileAuthMode.set('login');
    this.saveMobileAuthMode('login');
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mobileAuth: 'login' },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  showMobileRegister(event?: Event): void {
    this.stopAuthViewEvent(event);
    this.saveMobileAuthMode('register');
    void this.router.navigate(['/auth/register'], {
      queryParams: { mobileAuth: 'register' },
    });
  }

  backToMobileMenu(event?: Event): void {
    this.stopAuthViewEvent(event);
    if (this.mode() !== 'login') {
      this.backToLogin();
    }

    this.mobileAuthMode.set('menu');
    this.clearMobileAuthMode();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { mobileAuth: null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
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

  rememberMeLabelClasses(): string {
    return this.isDarkTheme()
      ? 'inline-flex items-center gap-2 text-sm font-medium text-slate-100'
      : 'inline-flex items-center gap-2 text-sm font-medium text-slate-700';
  }

  secondaryLinkClasses(): string {
    return this.isDarkTheme()
      ? 'text-right text-sm font-semibold text-red-200 hover:text-white hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white'
      : 'text-right text-sm font-medium text-[#8b070c] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c]';
  }

  registerPromptClasses(): string {
    return this.isDarkTheme()
      ? 'mt-3 text-center text-sm font-medium text-slate-100'
      : 'mt-3 text-center text-sm font-medium text-slate-700';
  }

  registerLinkClasses(): string {
    return this.isDarkTheme()
      ? 'font-semibold text-red-200 hover:text-white hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white'
      : 'font-semibold text-[#8b070c] hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8b070c]';
  }

  descriptionClasses(): string {
    return this.isDarkTheme()
      ? 'text-sm text-white/90'
      : 'text-sm text-slate-700';
  }

  hintClasses(): string {
    return this.isDarkTheme()
      ? 'mt-3 rounded-md border border-white/15 bg-black/35 px-3 py-2 text-xs font-semibold text-slate-100 shadow-sm'
      : 'mt-3 rounded-md border border-white/50 bg-white/45 px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm';
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

  private authInputClasses(hasError: boolean): string {
    const base =
      'h-9 w-full rounded-md border bg-white/90 px-3 text-sm text-slate-950 shadow-sm outline-none transition-colors placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-[#8b070c] focus-visible:ring-offset-1 focus-visible:ring-offset-white/70 disabled:cursor-not-allowed disabled:bg-slate-50/90 disabled:text-slate-500';

    return hasError
      ? `${base} border-red-500 bg-red-50/90`
      : `${base} border-slate-300 hover:border-slate-500`;
  }

  private initialMobileAuthMode(): MobileAuthMode {
    const requestedMode = this.route.snapshot.queryParamMap.get('mobileAuth') ?? this.storedMobileAuthMode();

    return requestedMode === 'login' ? 'login' : 'menu';
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

  private buildRequest(): LoginRequest {
    const value = this.loginForm.getRawValue();

    return {
      identifier: value.identifier.trim(),
      password: value.password,
    };
  }

  private startRecoveryTimers(): void {
    this.stopRecoveryTimers();
    this.codeExpiresInSeconds.set(CODE_EXPIRATION_SECONDS);
    this.resendCooldownSeconds.set(RESEND_COOLDOWN_SECONDS);

    this.recoveryIntervalId = window.setInterval(() => this.tickRecoveryTimers(), 1000);
  }

  private tickRecoveryTimers(): void {
    if (this.codeExpiresInSeconds() > 0) {
      this.codeExpiresInSeconds.update((value) => value - 1);
    }

    if (this.resendCooldownSeconds() > 0) {
      this.resendCooldownSeconds.update((value) => value - 1);
    }

    if (
      this.codeExpiresInSeconds() === 0 &&
      this.mode() === 'verify-code' &&
      !this.generalError()
    ) {
      this.successMessage.set('');
      this.setGeneralError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
    }
  }

  private stopRecoveryTimers(): void {
    if (this.recoveryIntervalId === null) {
      return;
    }

    window.clearInterval(this.recoveryIntervalId);
    this.recoveryIntervalId = null;
  }

  private clearFeedback(): void {
    this.generalError.set('');
    this.successMessage.set('');
    this.recoveryCodeFieldError.set('');
    this.resetPasswordFieldError.set('');
    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
  }

  private clearRecoveryState(): void {
    this.clearFeedback();
    this.stopRecoveryTimers();
    this.recoveryEmail.set('');
    this.recoveryVerificationToken.set('');
    this.codeExpiresInSeconds.set(CODE_EXPIRATION_SECONDS);
    this.resendCooldownSeconds.set(0);
    this.forgotSubmitted.set(false);
    this.codeSubmitted.set(false);
    this.resetSubmitted.set(false);
    this.forgotPasswordForm.reset();
    this.verifyCodeForm.reset();
    this.resetPasswordForm.reset();
    this.isRequestingCode.set(false);
    this.isVerifyingCode.set(false);
    this.isResettingPassword.set(false);
    this.isResendingCode.set(false);
  }

  private returnToLoginAfterReset(): void {
    this.mode.set('login');
    this.hasRecoveryHistoryEntry = false;
    this.clearRecoveryState();
    this.setSuccessMessage(this.t('AUTH.MESSAGES.PASSWORD_UPDATED_LOGIN'));
  }

  private setSuccessMessage(message: string): void {
    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
    this.generalError.set('');
    this.successMessage.set(message);

    this.successMessageTimeoutId = window.setTimeout(() => {
      this.successMessage.set('');
      this.successMessageTimeoutId = null;
    }, SUCCESS_MESSAGE_DURATION_MS);
  }

  private clearSuccessMessageTimeout(): void {
    if (this.successMessageTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.successMessageTimeoutId);
    this.successMessageTimeoutId = null;
  }

  private setGeneralError(message: string): void {
    this.clearErrorMessageTimeout();
    this.clearSuccessMessageTimeout();
    this.successMessage.set('');
    this.generalError.set(message);

    this.errorMessageTimeoutId = window.setTimeout(() => {
      this.generalError.set('');
      this.errorMessageTimeoutId = null;
    }, SUCCESS_MESSAGE_DURATION_MS);
  }

  private clearErrorMessageTimeout(): void {
    if (this.errorMessageTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.errorMessageTimeoutId);
    this.errorMessageTimeoutId = null;
  }

  private pushRecoveryHistoryEntry(): void {
    if (this.hasRecoveryHistoryEntry) {
      return;
    }

    window.history.pushState({ passwordRecoveryStep: 'change-password' }, '', window.location.href);
    this.hasRecoveryHistoryEntry = true;
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

    let index = 1;
    const loadingText = this.t('COMMON.LOADING_UPPER');
    this.loadingText.set(loadingText.slice(0, index));

    this.loadingIntervalId = window.setInterval(() => {
      const nextLoadingText = this.t('COMMON.LOADING_UPPER');
      index++;

      if (index > nextLoadingText.length) {
        index = 1;
      }

      this.loadingText.set(nextLoadingText.slice(0, index));
    }, 180);
  }

  private stopLoadingTextAnimation(): void {
    if (this.loadingIntervalId === null) {
      return;
    }

    window.clearInterval(this.loadingIntervalId);
    this.loadingIntervalId = null;
  }

  private applyLoginHttpError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.setGeneralError(this.t('AUTH.ERRORS.LOGIN_DEFAULT'));
      return;
    }

    const apiError = error.error as { error?: string; message?: string } | null;
    const bodyText = JSON.stringify(error.error ?? '').toLowerCase();

    if (error.status === 0) {
      this.setGeneralError(this.t('AUTH.ERRORS.CONNECTION'));
      return;
    }

    if (error.status === 400) {
      this.setGeneralError(this.t('AUTH.ERRORS.CHECK_DATA'));
      return;
    }

    if (error.status === 401) {
      this.setGeneralError(this.t('AUTH.ERRORS.BAD_CREDENTIALS'));
      return;
    }

    if (error.status === 403) {
      if (apiError?.error === 'EMAIL_NOT_VERIFIED') {
        const identifier = this.loginForm.controls.identifier.value.trim();
        this.setGeneralError(apiError.message || this.t('AUTH.ERRORS.INACTIVE'));

        if (identifier.includes('@')) {
          void this.router.navigate(['/auth/verify-account'], {
            queryParams: { email: identifier },
          });
        }
        return;
      }

      if (bodyText.includes('blocked') || bodyText.includes('bloque')) {
        this.setGeneralError(this.t('AUTH.ERRORS.BLOCKED'));
      } else {
        this.setGeneralError(this.t('AUTH.ERRORS.INACTIVE'));
      }
      return;
    }

    if (error.status >= 500) {
      this.setGeneralError(this.t('AUTH.ERRORS.SERVER'));
      return;
    }

    this.setGeneralError(this.t('AUTH.ERRORS.LOGIN_DEFAULT'));
  }

  private applyForgotPasswordHttpError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.setGeneralError(this.t('AUTH.ERRORS.REQUEST_DEFAULT'));
      return;
    }

    if (error.status === 0) {
      this.setGeneralError(this.t('AUTH.ERRORS.CONNECTION'));
      return;
    }

    if (error.status === 400) {
      this.setGeneralError(this.t('AUTH.ERRORS.EMAIL_FORMAT'));
      return;
    }

    if (error.status === 409 || error.status === 429) {
      this.setGeneralError(this.t('AUTH.ERRORS.WAIT_CODE'));
      return;
    }

    if (error.status >= 500) {
      this.setGeneralError(this.t('AUTH.ERRORS.SERVER'));
      return;
    }

    this.setGeneralError(this.t('AUTH.ERRORS.REQUEST_DEFAULT'));
  }

  private applyVerifyCodeHttpError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.setGeneralError(this.t('AUTH.ERRORS.VERIFY_DEFAULT'));
      return;
    }

    if (error.status === 0) {
      this.setGeneralError(this.t('AUTH.ERRORS.REGISTER_CONNECTION'));
      return;
    }

    if (error.status === 400) {
      this.setRecoveryCodeFieldError(this.t('AUTH.ERRORS.CODE_INVALID'));
      return;
    }

    if (error.status === 401) {
      this.setRecoveryCodeFieldError(this.t('AUTH.ERRORS.CODE_INVALID'));
      return;
    }

    if (error.status === 403) {
      this.setGeneralError(this.t('AUTH.ERRORS.ACCOUNT_INACTIVE'));
      return;
    }

    if (error.status >= 500) {
      this.setGeneralError(this.t('AUTH.ERRORS.SERVER'));
      return;
    }

    this.setGeneralError(this.t('AUTH.ERRORS.VERIFY_DEFAULT'));
  }

  private applyResetPasswordHttpError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.setGeneralError(this.t('AUTH.ERRORS.RESET_DEFAULT'));
      return;
    }

    const bodyText = JSON.stringify(error.error ?? '').toLowerCase();

    if (error.status === 0) {
      this.setGeneralError(this.t('AUTH.ERRORS.REGISTER_CONNECTION'));
      return;
    }

    if (error.status === 400) {
      if (this.isCurrentPasswordError(bodyText)) {
        this.setResetPasswordFieldError(this.t('AUTH.ERRORS.PASSWORD_REUSE'));
        return;
      }

      if (bodyText.includes('weak') || bodyText.includes('password')) {
        this.setResetPasswordFieldError(this.t('AUTH.VALIDATION.PASSWORD_STRONG_FULL'));
        return;
      }

      this.setGeneralError(this.t('AUTH.ERRORS.CHECK_CODE_PASSWORD'));
      return;
    }

    if (error.status === 401) {
      this.setGeneralError(this.t('AUTH.ERRORS.CODE_INVALID'));
      return;
    }

    if (error.status === 403) {
      this.setGeneralError(this.t('AUTH.ERRORS.ACCOUNT_INACTIVE'));
      return;
    }

    if (error.status === 409 || this.isCurrentPasswordError(bodyText)) {
      this.setResetPasswordFieldError(this.t('AUTH.ERRORS.PASSWORD_REUSE'));
      return;
    }

    if (error.status >= 500) {
      this.setGeneralError(this.t('AUTH.ERRORS.SERVER'));
      return;
    }

    this.setGeneralError(this.t('AUTH.ERRORS.RESET_DEFAULT'));
  }

  private setRecoveryCodeFieldError(message: string): void {
    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
    this.generalError.set('');
    this.successMessage.set('');
    this.recoveryCodeFieldError.set(message);
  }

  private setResetPasswordFieldError(message: string): void {
    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
    this.generalError.set('');
    this.successMessage.set('');
    this.resetPasswordFieldError.set(message);
  }

  private isCurrentPasswordError(bodyText: string): boolean {
    return (
      bodyText.includes('reuse') ||
      bodyText.includes('current') ||
      bodyText.includes('actual') ||
      bodyText.includes('same')
    );
  }
}
