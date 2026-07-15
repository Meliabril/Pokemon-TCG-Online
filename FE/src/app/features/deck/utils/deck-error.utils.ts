import { HttpErrorResponse } from '@angular/common/http';
import { UiTranslateFn } from '../../../core/services/language.service';

export function normalizeDeckError(error: unknown, t: UiTranslateFn = (key) => key): string {
  if (!(error instanceof HttpErrorResponse)) {
    return t('DECK.ERRORS.DEFAULT');
  }

  if (error.status === 0) {
    return t('DECK.ERRORS.CONNECTION');
  }

  if (error.status === 401) {
    return t('DECK.ERRORS.UNAUTHORIZED');
  }

  if (error.error && typeof error.error === 'object') {
    const payload = error.error as Record<string, unknown>;
    const message = payload['message'];
    if (message === 'Authentication is required') {
      return t('DECK.ERRORS.UNAUTHORIZED');
    }
  }

  if (error.status === 400) {
    return t('DECK.ERRORS.BAD_REQUEST');
  }

  if (error.status === 404) {
    return t('DECK.ERRORS.NOT_FOUND');
  }

  if (error.status === 409) {
    return t('DECK.ERRORS.CONFLICT');
  }

  if (error.status >= 500) {
    return t('DECK.ERRORS.SERVER');
  }

  return t('DECK.ERRORS.DEFAULT');
}
