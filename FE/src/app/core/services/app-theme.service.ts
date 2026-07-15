import { Injectable, signal } from '@angular/core';
import { STORAGE_KEYS } from '../constants/storage/storage.constants';

export type AppTheme = 'LIGHT' | 'DARK';

@Injectable({ providedIn: 'root' })
export class AppThemeService {
  private readonly themeState = signal<AppTheme>(this.readStoredTheme());

  readonly theme = this.themeState.asReadonly();

  setTheme(theme: AppTheme): void {
    this.themeState.set(theme);
    this.writeStoredTheme(theme);
  }

  toggleTheme(): void {
    this.setTheme(this.themeState() === 'DARK' ? 'LIGHT' : 'DARK');
  }

  private readStoredTheme(): AppTheme {
    if (typeof localStorage === 'undefined') {
      return 'LIGHT';
    }

    return localStorage.getItem(STORAGE_KEYS.appTheme) === 'DARK' ? 'DARK' : 'LIGHT';
  }

  private writeStoredTheme(theme: AppTheme): void {
    if (typeof localStorage === 'undefined') {
      return;
    }

    localStorage.setItem(STORAGE_KEYS.appTheme, theme);
  }
}
