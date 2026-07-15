import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { AppThemeService } from '../../../../core/services/app-theme.service';

@Component({
  selector: 'app-shell',
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppShellComponent {
  private readonly appTheme = inject(AppThemeService);

  readonly useHomeBackground = input(false);
  readonly useDarkThemeVideoBackground = input(false);
  readonly darkHomeBackgroundVideoSrc = 'assets/videos/pokemon-fondo-noche.mp4';
  readonly isDarkTheme = computed(() => this.appTheme.theme() === 'DARK');
  readonly showDarkHomeVideo = computed(
    () => (this.useHomeBackground() || this.useDarkThemeVideoBackground()) && this.isDarkTheme()
  );

  shellClasses(): string {
    const base = 'app-shell min-h-screen text-[#fff7d6]';

    if (this.useHomeBackground()) {
      return this.isDarkTheme() ? `${base} app-shell--home-dark` : `${base} app-shell--home-light`;
    }

    if (this.useDarkThemeVideoBackground() && this.isDarkTheme()) {
      return `${base} app-shell--default app-shell--video-dark`;
    }

    if (!this.isDarkTheme()) {
      return `${base} app-shell--default app-shell--default-light`;
    }

    return `${base} app-shell--default`;
  }
}
