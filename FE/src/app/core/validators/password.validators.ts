import { AbstractControl, ValidationErrors } from '@angular/forms';

const STRONG_PASSWORD_PATTERN = /^(?=.*\p{Lu})(?=.*\d)(?=.*[^\p{L}\p{N}]).+$/u;

export function strongPasswordValidator(
  control: AbstractControl<string>
): ValidationErrors | null {
  const password = control.value;

  if (!password || STRONG_PASSWORD_PATTERN.test(password)) {
    return null;
  }

  return { strongPassword: true };
}

export function matchingPasswordsValidator(control: AbstractControl): ValidationErrors | null {
  const newPassword = control.get('newPassword')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!newPassword || !confirmPassword || newPassword === confirmPassword) {
    return null;
  }

  return { passwordMismatch: true };
}
