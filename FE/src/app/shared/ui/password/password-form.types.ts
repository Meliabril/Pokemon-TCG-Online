import { FormControl, FormGroup } from '@angular/forms';

export type PasswordCodeFormGroup = FormGroup<{
  code: FormControl<string>;
}>;

export type NewPasswordFormGroup = FormGroup<{
  newPassword: FormControl<string>;
  confirmPassword: FormControl<string>;
}>;

export type PasswordFormVariant = 'auth' | 'profile';
