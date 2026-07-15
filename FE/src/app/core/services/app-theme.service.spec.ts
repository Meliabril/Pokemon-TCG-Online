import { TestBed } from '@angular/core/testing';
import { STORAGE_KEYS } from '../constants/storage/storage.constants';
import { AppThemeService } from './app-theme.service';

describe('AppThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('defaults to light theme when nothing is stored', () => {
    const service = TestBed.inject(AppThemeService);

    expect(service.theme()).toBe('LIGHT');
  });

  it('reads the stored dark theme', () => {
    localStorage.setItem(STORAGE_KEYS.appTheme, 'DARK');

    const service = TestBed.inject(AppThemeService);

    expect(service.theme()).toBe('DARK');
  });

  it('stores theme changes', () => {
    const service = TestBed.inject(AppThemeService);

    service.setTheme('DARK');

    expect(service.theme()).toBe('DARK');
    expect(localStorage.getItem(STORAGE_KEYS.appTheme)).toBe('DARK');
  });

  it('toggles between light and dark themes', () => {
    const service = TestBed.inject(AppThemeService);

    service.toggleTheme();
    expect(service.theme()).toBe('DARK');

    service.toggleTheme();
    expect(service.theme()).toBe('LIGHT');
  });
});
