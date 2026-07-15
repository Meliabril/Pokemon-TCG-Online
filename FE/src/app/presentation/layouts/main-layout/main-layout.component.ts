import { ChangeDetectionStrategy, Component, HostListener, inject, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { NavbarComponent } from '../../components/navigation/navbar/navbar.component';
import { AppShellComponent } from '../../../shared/ui/layout/app-shell/app-shell.component';
import { AuthMediaService } from '../../../features/auth/data-access/auth-media.service';
import { LanguageService, TranslationParams } from '../../../core/services/language.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, NavbarComponent, AppShellComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MainLayoutComponent {
  private readonly router = inject(Router);
  private readonly authMedia = inject(AuthMediaService);
  private readonly languageService = inject(LanguageService);

  readonly isAuthRoute = signal(this.router.url.startsWith('/auth'));
  readonly isGameRoute = signal(this.router.url.startsWith('/games/'));
  readonly isHomeRoute = signal(this.router.url.startsWith('/home'));
  readonly useDarkThemeVideoBackground = signal(
    this.router.url.startsWith('/pokedex') || this.router.url.startsWith('/perfil')
  );
  readonly volumePercent = signal(Math.round(this.authMedia.volume() * 100));
  readonly t = (key: string, params?: TranslationParams) => this.languageService.t(key, params);

  private readonly document = inject(DOCUMENT);

  constructor() {
    this.updateHtmlClass(this.router.url);
    this.syncBackgroundAudio();

    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe((event) => {
      this.isAuthRoute.set(event.urlAfterRedirects.startsWith('/auth'));
      this.isGameRoute.set(event.urlAfterRedirects.startsWith('/games/'));
      this.isHomeRoute.set(event.urlAfterRedirects.startsWith('/home'));
      this.useDarkThemeVideoBackground.set(
        event.urlAfterRedirects.startsWith('/pokedex') || event.urlAfterRedirects.startsWith('/perfil')
      );
      this.updateHtmlClass(event.urlAfterRedirects);
      this.syncBackgroundAudio();
      this.authMedia.playIfEnabled();
    });
  }

  private syncBackgroundAudio(): void {
    if (this.isGameRoute()) {
      this.authMedia.playBattleTrack();
    } else {
      this.authMedia.stopBattleTrack();
    }
  }

  private updateHtmlClass(url: string): void {
    if (url.startsWith('/games/')) {
      this.document.documentElement.classList.add('game-route-active');
    } else {
      this.document.documentElement.classList.remove('game-route-active');
    }
  }

  onVolumeChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const nextVolume = Number(input.value);

    if (!Number.isFinite(nextVolume)) {
      return;
    }

    this.volumePercent.set(nextVolume);
    this.authMedia.setVolume(nextVolume / 100);
    this.authMedia.markAudioEnabled();
    this.syncBackgroundAudio();
  }

  @HostListener('document:click')
  @HostListener('document:keydown')
  resumeBackgroundAudio(): void {
    if (this.isGameRoute()) {
      this.authMedia.playBattleTrack();
    } else {
      this.authMedia.playIfEnabled();
    }
  }
}
