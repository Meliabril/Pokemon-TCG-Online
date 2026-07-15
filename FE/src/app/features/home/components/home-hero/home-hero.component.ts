import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LanguageService, TranslationParams } from '../../../../core/services/language.service';

@Component({
  selector: 'app-home-hero',
  templateUrl: './home-hero.component.html',
  styleUrl: './home-hero.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeHeroComponent {
  private readonly languageService = inject(LanguageService);

  readonly headline = input.required<string>();
  readonly isLightTheme = input(false);
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);

  eyebrowClasses(): string {
    return this.isLightTheme() ? 'home-eyebrow home-eyebrow--light' : 'home-eyebrow home-eyebrow--dark';
  }

  headlineClasses(): string {
    return this.isLightTheme() ? 'home-title home-title--light' : 'home-title home-title--dark';
  }
}
