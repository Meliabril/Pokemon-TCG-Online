import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { LanguageService } from '../../../../core/services/language.service';
import { UserProfile } from '../../../../core/models/interfaces/user/update-user.interface';
import {
  AvatarPickerComponent,
  DEFAULT_AVATAR_ID
} from '../../../../shared/ui/profile/avatar-picker/avatar-picker.component';

@Component({
  selector: 'app-profile-summary-card',
  imports: [ReactiveFormsModule, AvatarPickerComponent],
  templateUrl: './profile-summary-card.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileSummaryCardComponent {
  private readonly languageService = inject(LanguageService);

  readonly profile = input.required<UserProfile>();
  readonly usernameControl = input.required<FormControl<string>>();
  readonly selectedAvatarId = input(DEFAULT_AVATAR_ID);
  readonly isEditingUsername = input(false);
  readonly isSaving = input(false);
  readonly submitted = input(false);
  readonly successMessage = input('');
  readonly errorMessage = input('');
  readonly isLightTheme = input(false);

  readonly avatarChanged = output<string>();
  readonly startUsernameEdit = output<void>();
  readonly saveUsername = output<void>();
  readonly cancelUsernameEdit = output<void>();
  readonly startPasswordChange = output<void>();
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  verificationLabel(): string {
    return this.profile().emailVerified ? this.t('PROFILE.SUMMARY.VERIFIED') : this.t('PROFILE.SUMMARY.PENDING');
  }

  usernameError(): string {
    const control = this.usernameControl();

    if (!(control.invalid && (control.touched || control.dirty || this.submitted()))) {
      return '';
    }

    if (control.hasError('required')) {
      return this.t('AUTH.VALIDATION.USERNAME_REQUIRED');
    }

    if (control.hasError('minlength')) {
      return this.t('AUTH.VALIDATION.IDENTIFIER_MIN');
    }

    return this.t('PROFILE.ERRORS.CHECK_USERNAME');
  }
}
