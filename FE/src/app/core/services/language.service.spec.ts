import { TestBed } from '@angular/core/testing';
import { STORAGE_KEYS } from '../constants/storage/storage.constants';
import { LanguageService } from './language.service';

describe('LanguageService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('defaults to Spanish when there is no stored preference', () => {
    const service = TestBed.inject(LanguageService);

    expect(service.language()).toBe('es');
  });

  it('persists the selected language', async () => {
    const service = TestBed.inject(LanguageService);

    await service.useLanguage('en');

    expect(service.language()).toBe('en');
    expect(localStorage.getItem(STORAGE_KEYS.language)).toBe('en');
  });

  it('uses translated card fields only in Spanish', async () => {
    const service = TestBed.inject(LanguageService);
    const card = {
      name: 'Venusaur-EX',
      displayName: 'Venusaur-EX ES'
    };

    expect(service.cardName(card)).toBe('Venusaur-EX ES');

    await service.useLanguage('en');

    expect(service.cardName(card)).toBe('Venusaur-EX');
  });
});
