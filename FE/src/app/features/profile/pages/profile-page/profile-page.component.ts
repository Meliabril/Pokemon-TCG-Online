import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  OnDestroy,
  computed,
  inject,
  signal
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import {
  matchingPasswordsValidator,
  strongPasswordValidator
} from '../../../../core/validators/password.validators';
import { StorageService } from '../../../../core/storage/storage.service';
import { LanguageService } from '../../../../core/services/language.service';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { UserApiService } from '../../../../infrastructure/api/user/user-api.service';
import { DEFAULT_AVATAR_ID } from '../../../../shared/ui/profile/avatar-picker/avatar-picker.component';
import { NewPasswordFormComponent } from '../../../../shared/ui/password/new-password-form/new-password-form.component';
import { PasswordCodeFormComponent } from '../../../../shared/ui/password/password-code-form/password-code-form.component';
import {
  UpdateUserRequest,
  UserProfile
} from '../../../../core/models/interfaces/user/update-user.interface';
import { ProfileHistoryPlaceholderComponent } from '../../components/profile-history-placeholder/profile-history-placeholder.component';
import { ProfileSummaryCardComponent } from '../../components/profile-summary-card/profile-summary-card.component';

type ProfileSection = 'trainer' | 'history';
type PasswordChangeStep = 'code' | 'password';

const CODE_EXPIRATION_SECONDS = 300;
const RESEND_COOLDOWN_SECONDS = 60;
const MESSAGE_DURATION_MS = 3000;

type ProfileEditFormGroup = FormGroup<{
  username: FormControl<string>;
  avatar: FormControl<string>;
}>;

@Component({
  selector: 'app-profile-page',
  imports: [
    ProfileSummaryCardComponent,
    ProfileHistoryPlaceholderComponent,
    PasswordCodeFormComponent,
    NewPasswordFormComponent
  ],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfilePageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly storageService = inject(StorageService);
  private readonly userApi = inject(UserApiService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);

  readonly currentUser = this.storageService.currentUser;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly profile = signal<UserProfile | null>(this.profileFromSession());
  readonly activeSection = signal<ProfileSection>('trainer');
  readonly isLightTheme = computed(() => this.appTheme.theme() === 'LIGHT');
  readonly isEditingUsername = signal(false);
  readonly isLoadingUser = signal(false);
  readonly isSaving = signal(false);
  readonly submitted = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly isPasswordModalOpen = signal(false);
  readonly passwordChangeStep = signal<PasswordChangeStep>('code');
  readonly passwordVerificationToken = signal('');
  readonly passwordFlowMessage = signal('');
  readonly passwordFlowError = signal('');
  readonly passwordCodeFieldError = signal('');
  readonly newPasswordFieldError = signal('');
  readonly passwordCodeSubmitted = signal(false);
  readonly passwordSubmitted = signal(false);
  readonly isRequestingPasswordCode = signal(false);
  readonly isVerifyingPasswordCode = signal(false);
  readonly isChangingPassword = signal(false);
  readonly isResendingPasswordCode = signal(false);
  readonly codeExpiresInSeconds = signal(CODE_EXPIRATION_SECONDS);
  readonly resendCooldownSeconds = signal(0);
  private passwordIntervalId: number | null = null;
  private successMessageTimeoutId: number | null = null;
  private errorMessageTimeoutId: number | null = null;
  private passwordMessageTimeoutId: number | null = null;
  private passwordErrorTimeoutId: number | null = null;
  private hasPasswordHistoryEntry = false;
  readonly profileForm: ProfileEditFormGroup = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    avatar: [DEFAULT_AVATAR_ID, [Validators.required]]
  });
  readonly passwordCodeForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
  });
  readonly passwordChangeForm = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), strongPasswordValidator]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: matchingPasswordsValidator }
  );
  readonly formValue = toSignal(this.profileForm.valueChanges, {
    initialValue: this.profileForm.getRawValue()
  });
  readonly selectedAvatarId = computed(() => this.formValue().avatar || DEFAULT_AVATAR_ID);
  readonly trainerTabClasses = computed(() => this.navTabClasses('trainer'));
  readonly historyTabClasses = computed(() => this.navTabClasses('history'));

  constructor() {
    const profile = this.profile();

    if (profile) {
      this.applyProfileToForm(profile);
    }

    this.loadProfile();
  }

  ngOnDestroy(): void {
    this.stopPasswordTimers();
    this.clearAllMessageTimeouts();
  }

  @HostListener('window:popstate')
  handleBrowserBack(): void {
    if (!this.isPasswordModalOpen() || this.passwordChangeStep() !== 'password') {
      return;
    }

    this.hasPasswordHistoryEntry = false;
    this.closePasswordChangeModal();
  }

  setActiveSection(section: ProfileSection): void {
    this.activeSection.set(section);
  }

  startUsernameEdit(): void {
    this.submitted.set(false);
    this.clearProfileMessages();
    this.isEditingUsername.set(true);
  }

  saveUsername(): void {
    this.submitted.set(true);
    this.clearProfileMessages();

    const profile = this.profile();

    if (!profile) {
      this.setProfileError(this.t('PROFILE.NO_SESSION'));
      return;
    }

    const usernameControl = this.profileForm.controls.username;

    if (usernameControl.invalid) {
      usernameControl.markAsTouched();
      return;
    }

    const nextUsername = usernameControl.value.trim();

    if (nextUsername.toLowerCase() === profile.username.toLowerCase()) {
      this.cancelUsernameEdit();
      return;
    }

    this.updateProfile({ username: nextUsername }, () => {
      this.isEditingUsername.set(false);
      this.submitted.set(false);
      this.setProfileSuccess(this.t('PROFILE.USERNAME_UPDATED'));
    });
  }

  cancelUsernameEdit(): void {
    const profile = this.profile();

    if (!profile) {
      return;
    }

    this.profileForm.controls.username.setValue(profile.username);
    this.profileForm.controls.username.markAsPristine();
    this.profileForm.controls.username.markAsUntouched();
    this.isEditingUsername.set(false);
    this.submitted.set(false);
    this.clearProfileError();
  }

  updateAvatar(avatarId: string): void {
    if (this.isSaving()) {
      return;
    }

    const profile = this.profile();

    if (!profile) {
      return;
    }

    const currentAvatarId = profile.avatar || DEFAULT_AVATAR_ID;

    if (avatarId === currentAvatarId) {
      return;
    }

    this.profileForm.controls.avatar.setValue(avatarId);
    this.profileForm.controls.avatar.markAsDirty();
    this.clearProfileMessages();

    this.updateProfile({ avatar: avatarId }, () => {
      this.setProfileSuccess(this.t('PROFILE.AVATAR_UPDATED'));
    });
  }

  usernameControl(): FormControl<string> {
    return this.profileForm.controls.username;
  }

  openPasswordChangeModal(): void {
    this.clearPasswordFlowState();
    this.isPasswordModalOpen.set(true);
    this.requestPasswordChangeCode(false);
  }

  closePasswordChangeModal(): void {
    this.isPasswordModalOpen.set(false);
    this.clearPasswordFlowState();
  }

  submitPasswordChangeCode(): void {
    this.passwordCodeSubmitted.set(true);
    this.clearPasswordMessages();

    if (this.codeExpiresInSeconds() <= 0) {
      this.setPasswordError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
      return;
    }

    if (this.passwordCodeForm.invalid) {
      this.passwordCodeForm.markAllAsTouched();
      return;
    }

    this.isVerifyingPasswordCode.set(true);

    this.userApi.verifyPasswordChangeCode(this.passwordCodeForm.getRawValue()).subscribe({
      next: (response) => {
        this.passwordCodeFieldError.set('');
        this.passwordVerificationToken.set(response.verificationToken);
        this.passwordChangeForm.reset();
        this.passwordSubmitted.set(false);
        this.passwordChangeStep.set('password');
        this.pushPasswordHistoryEntry();
        this.setPasswordMessage(this.t('AUTH.MESSAGES.CODE_VERIFIED'));
        this.stopPasswordTimers();
        this.isVerifyingPasswordCode.set(false);
      },
      error: (error: unknown) => {
        this.setPasswordCodeError(this.normalizePasswordFlowError(error));
        this.isVerifyingPasswordCode.set(false);
      }
    });
  }

  resendPasswordChangeCode(): void {
    if (this.isPasswordResendDisabled()) {
      return;
    }

    this.requestPasswordChangeCode(true);
  }

  submitPasswordChange(): void {
    this.passwordSubmitted.set(true);
    this.clearPasswordMessages();

    if (!this.passwordVerificationToken()) {
      this.passwordChangeStep.set('code');
      this.setPasswordError(this.t('AUTH.MESSAGES.RECOVERY_CODE_REQUIRED'));
      return;
    }

    if (this.passwordChangeForm.invalid) {
      this.passwordChangeForm.markAllAsTouched();
      return;
    }

    const request = this.passwordChangeForm.getRawValue();
    this.isChangingPassword.set(true);

    this.userApi
      .changeMyPassword({
        verificationToken: this.passwordVerificationToken(),
        newPassword: request.newPassword,
        confirmPassword: request.confirmPassword
      })
      .subscribe({
        next: (response) => {
          this.closePasswordChangeModal();
          this.setProfileSuccess(this.normalizePasswordSuccessMessage(response.message));
          this.isChangingPassword.set(false);
        },
        error: (error: unknown) => {
          this.handlePasswordChangeError(error);
          this.isChangingPassword.set(false);
        }
      });
  }

  formattedPasswordExpiration(): string {
    const totalSeconds = Math.max(this.codeExpiresInSeconds(), 0);
    const minutes = Math.floor(totalSeconds / 60)
      .toString()
      .padStart(2, '0');
    const seconds = (totalSeconds % 60).toString().padStart(2, '0');

    return `${minutes}:${seconds}`;
  }

  isPasswordCodeSubmitDisabled(): boolean {
    return (
      this.isRequestingPasswordCode() ||
      this.isVerifyingPasswordCode() ||
      this.isResendingPasswordCode() ||
      this.codeExpiresInSeconds() <= 0
    );
  }

  isPasswordResendDisabled(): boolean {
    return (
      this.isRequestingPasswordCode() ||
      this.isVerifyingPasswordCode() ||
      this.isChangingPassword() ||
      this.isResendingPasswordCode() ||
      this.resendCooldownSeconds() > 0
    );
  }

  isPasswordSubmitDisabled(): boolean {
    return this.isChangingPassword() || !this.passwordVerificationToken();
  }

  private loadProfile(): void {
    if (!this.storageService.isAuthenticated()) {
      return;
    }

    this.isLoadingUser.set(true);

    this.userApi.getMyProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.storageService.updateCurrentUserProfile(profile);
        this.applyProfileToForm(profile);
        this.isLoadingUser.set(false);
      },
      error: () => {
        this.isLoadingUser.set(false);
      }
    });
  }

  private updateProfile(request: UpdateUserRequest, onSuccess: () => void): void {
    this.isSaving.set(true);

    this.userApi.updateMyProfile(request).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.storageService.updateCurrentUserProfile(profile);
        this.applyProfileToForm(profile);
        onSuccess();
        this.isSaving.set(false);
      },
      error: (error: unknown) => {
        const currentProfile = this.profile();

        if (currentProfile) {
          this.applyProfileToForm(currentProfile);
        }

        this.setProfileError(this.normalizeProfileError(error));
        this.isSaving.set(false);
      }
    });
  }

  private applyProfileToForm(profile: UserProfile): void {
    this.profileForm.reset({
      username: profile.username,
      avatar: profile.avatar || DEFAULT_AVATAR_ID
    });
  }

  private requestPasswordChangeCode(isResend: boolean): void {
    this.clearPasswordMessages();
    this.passwordVerificationToken.set('');
    this.passwordCodeForm.reset();
    this.passwordChangeForm.reset();
    this.passwordCodeSubmitted.set(false);
    this.passwordSubmitted.set(false);
    this.passwordCodeFieldError.set('');
    this.newPasswordFieldError.set('');
    this.passwordChangeStep.set('code');

    if (isResend) {
      this.isResendingPasswordCode.set(true);
    } else {
      this.isRequestingPasswordCode.set(true);
    }

    this.userApi.requestPasswordChangeCode().subscribe({
      next: () => {
        this.startPasswordTimers();
        this.setPasswordMessage(this.t('PROFILE.PASSWORD_CODE_SENT'));
        this.isRequestingPasswordCode.set(false);
        this.isResendingPasswordCode.set(false);
      },
      error: (error: unknown) => {
        this.setPasswordError(this.normalizePasswordFlowError(error));
        this.isRequestingPasswordCode.set(false);
        this.isResendingPasswordCode.set(false);
      }
    });
  }

  private startPasswordTimers(): void {
    this.stopPasswordTimers();
    this.codeExpiresInSeconds.set(CODE_EXPIRATION_SECONDS);
    this.resendCooldownSeconds.set(RESEND_COOLDOWN_SECONDS);

    this.passwordIntervalId = window.setInterval(() => this.tickPasswordTimers(), 1000);
  }

  private tickPasswordTimers(): void {
    if (this.codeExpiresInSeconds() > 0) {
      this.codeExpiresInSeconds.update((seconds) => seconds - 1);
    }

    if (this.resendCooldownSeconds() > 0) {
      this.resendCooldownSeconds.update((seconds) => seconds - 1);
    }

    if (
      this.codeExpiresInSeconds() === 0 &&
      this.passwordChangeStep() === 'code' &&
      !this.passwordFlowError()
    ) {
      this.passwordFlowMessage.set('');
      this.setPasswordError(this.t('AUTH.MESSAGES.CODE_EXPIRED'));
    }
  }

  private stopPasswordTimers(): void {
    if (this.passwordIntervalId === null) {
      return;
    }

    window.clearInterval(this.passwordIntervalId);
    this.passwordIntervalId = null;
  }

  private clearPasswordFlowState(): void {
    this.stopPasswordTimers();
    this.passwordChangeStep.set('code');
    this.passwordVerificationToken.set('');
    this.clearPasswordMessages();
    this.passwordCodeSubmitted.set(false);
    this.passwordSubmitted.set(false);
    this.passwordCodeFieldError.set('');
    this.newPasswordFieldError.set('');
    this.isRequestingPasswordCode.set(false);
    this.isVerifyingPasswordCode.set(false);
    this.isChangingPassword.set(false);
    this.isResendingPasswordCode.set(false);
    this.codeExpiresInSeconds.set(CODE_EXPIRATION_SECONDS);
    this.resendCooldownSeconds.set(0);
    this.passwordCodeForm.reset();
    this.passwordChangeForm.reset();
    this.hasPasswordHistoryEntry = false;
  }

  private setProfileSuccess(message: string): void {
    this.clearSuccessMessageTimeout();
    this.clearProfileError();
    this.successMessage.set(message);

    this.successMessageTimeoutId = window.setTimeout(() => {
      this.successMessage.set('');
      this.successMessageTimeoutId = null;
    }, MESSAGE_DURATION_MS);
  }

  private setProfileError(message: string): void {
    this.clearErrorMessageTimeout();
    this.clearProfileSuccess();
    this.errorMessage.set(message);

    this.errorMessageTimeoutId = window.setTimeout(() => {
      this.errorMessage.set('');
      this.errorMessageTimeoutId = null;
    }, MESSAGE_DURATION_MS);
  }

  private setPasswordMessage(message: string): void {
    this.clearPasswordMessageTimeout();
    this.clearPasswordError();
    this.passwordFlowMessage.set(message);

    this.passwordMessageTimeoutId = window.setTimeout(() => {
      this.passwordFlowMessage.set('');
      this.passwordMessageTimeoutId = null;
    }, MESSAGE_DURATION_MS);
  }

  private setPasswordError(message: string): void {
    this.clearPasswordErrorTimeout();
    this.clearPasswordMessage();
    this.passwordFlowError.set(message);

    this.passwordErrorTimeoutId = window.setTimeout(() => {
      this.passwordFlowError.set('');
      this.passwordErrorTimeoutId = null;
    }, MESSAGE_DURATION_MS);
  }

  private clearProfileMessages(): void {
    this.clearProfileSuccess();
    this.clearProfileError();
  }

  private clearPasswordMessages(): void {
    this.clearPasswordMessage();
    this.clearPasswordError();
    this.passwordCodeFieldError.set('');
    this.newPasswordFieldError.set('');
  }

  private clearProfileSuccess(): void {
    this.successMessage.set('');
    this.clearSuccessMessageTimeout();
  }

  private clearProfileError(): void {
    this.errorMessage.set('');
    this.clearErrorMessageTimeout();
  }

  private clearPasswordMessage(): void {
    this.passwordFlowMessage.set('');
    this.clearPasswordMessageTimeout();
  }

  private clearPasswordError(): void {
    this.passwordFlowError.set('');
    this.clearPasswordErrorTimeout();
  }

  private clearAllMessageTimeouts(): void {
    this.clearSuccessMessageTimeout();
    this.clearErrorMessageTimeout();
    this.clearPasswordMessageTimeout();
    this.clearPasswordErrorTimeout();
  }

  private clearSuccessMessageTimeout(): void {
    if (this.successMessageTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.successMessageTimeoutId);
    this.successMessageTimeoutId = null;
  }

  private clearErrorMessageTimeout(): void {
    if (this.errorMessageTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.errorMessageTimeoutId);
    this.errorMessageTimeoutId = null;
  }

  private clearPasswordMessageTimeout(): void {
    if (this.passwordMessageTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.passwordMessageTimeoutId);
    this.passwordMessageTimeoutId = null;
  }

  private clearPasswordErrorTimeout(): void {
    if (this.passwordErrorTimeoutId === null) {
      return;
    }

    window.clearTimeout(this.passwordErrorTimeoutId);
    this.passwordErrorTimeoutId = null;
  }

  private pushPasswordHistoryEntry(): void {
    if (this.hasPasswordHistoryEntry) {
      return;
    }

    window.history.pushState({ profilePasswordStep: 'change-password' }, '', window.location.href);
    this.hasPasswordHistoryEntry = true;
  }

  private profileFromSession(): UserProfile | null {
    const user = this.currentUser();

    if (!user) {
      return null;
    }

    return {
      username: user.username,
      email: user.email,
      emailVerified: user.emailVerified,
      ...(user.avatar !== undefined ? { avatar: user.avatar } : {})
    };
  }

  private navTabClasses(section: ProfileSection): string {
    const base =
      'font-pixel relative px-2 pb-2 text-[0.7rem] tracking-[0.16em] transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#B8792E]/70 sm:text-xs';

    const palette = this.isLightTheme()
      ? ' text-[#FFE09A] hover:text-[#F4D27A]'
      : ' text-[#D9A441] hover:text-[#C08A3A]';

    return this.activeSection() === section
      ? `${base}${palette} after:absolute after:inset-x-2 after:bottom-0 after:h-0.5 after:rounded-full ${this.isLightTheme() ? 'after:bg-[#F4D27A]' : 'after:bg-[#B8792E]'}`
      : `${base}${palette}`;
  }

  private normalizeProfileError(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return this.t('PROFILE.ERRORS.SAVE_PROFILE');
    }

    const bodyText = JSON.stringify(error.error ?? '').toLowerCase();

    if (error.status === 0) {
      return this.t('PROFILE.ERRORS.POKECENTER_CONNECTION');
    }

    if (error.status === 409 || bodyText.includes('username')) {
      return this.t('PROFILE.ERRORS.USERNAME_TAKEN');
    }

    if (error.status === 400) {
      return this.t('PROFILE.ERRORS.CHECK_USERNAME');
    }

    if (error.status === 403) {
      return this.t('PROFILE.ERRORS.PROFILE_FORBIDDEN');
    }

    return this.t('PROFILE.ERRORS.SAVE_PROFILE');
  }

  private normalizePasswordFlowError(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return this.t('PROFILE.ERRORS.PASSWORD_DEFAULT');
    }

    const bodyText = JSON.stringify(error.error ?? '').toLowerCase();

    if (error.status === 0) {
      return this.t('PROFILE.ERRORS.POKECENTER_CONNECTION');
    }

    if (error.status === 400) {
      if (this.passwordChangeStep() === 'code') {
        return this.t('AUTH.ERRORS.CODE_INVALID');
      }

      if (this.isCurrentPasswordError(bodyText)) {
        return this.t('AUTH.ERRORS.PASSWORD_REUSE');
      }

      if (bodyText.includes('weak') || bodyText.includes('password')) {
        return this.t('AUTH.VALIDATION.PASSWORD_STRONG_FULL');
      }

      return this.t('AUTH.ERRORS.CHECK_CODE_PASSWORD');
    }

    if (error.status === 401) {
      return this.t('AUTH.ERRORS.CODE_INVALID');
    }

    if (error.status === 403) {
      return this.t('PROFILE.ERRORS.PASSWORD_FORBIDDEN');
    }

    if (
      error.status === 409 ||
      this.isCurrentPasswordError(bodyText)
    ) {
      return this.t('AUTH.ERRORS.PASSWORD_REUSE');
    }

    if (error.status === 429) {
      return this.t('AUTH.ERRORS.WAIT_CODE');
    }

    return this.t('PROFILE.ERRORS.PASSWORD_DEFAULT');
  }

  private handlePasswordChangeError(error: unknown): void {
    const message = this.normalizePasswordFlowError(error);

    if (
      message === this.t('AUTH.ERRORS.PASSWORD_REUSE') ||
      message === this.t('AUTH.VALIDATION.PASSWORD_STRONG_FULL')
    ) {
      this.setNewPasswordError(message);
      return;
    }

    this.setPasswordError(message);
  }

  private setPasswordCodeError(message: string): void {
    this.clearPasswordMessage();
    this.clearPasswordError();
    this.passwordCodeFieldError.set(message);
  }

  private setNewPasswordError(message: string): void {
    this.clearPasswordMessage();
    this.clearPasswordError();
    this.newPasswordFieldError.set(message);
  }

  private normalizePasswordSuccessMessage(_message: string | undefined): string {
    return this.t('PROFILE.PASSWORD_UPDATED');
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
