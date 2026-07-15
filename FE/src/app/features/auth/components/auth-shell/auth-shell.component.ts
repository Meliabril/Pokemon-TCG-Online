import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  ViewEncapsulation,
  computed,
  inject,
} from '@angular/core';
import {
  AUTH_DARK_BACKGROUND_VIDEO_SRC,
  AUTH_LIGHT_BACKGROUND_VIDEO_SRC,
  AuthMediaService,
} from '../../data-access/auth-media.service';
import { LanguageService } from '../../../../core/services/language.service';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import type { AppTheme } from '../../../../core/services/app-theme.service';

const SPANISH_LOGO_URL =
  'https://images.wikidexcdn.net/mwuploads/wikidex/thumb/7/75/latest/20190307170728/Logo_Pok%C3%A9mon_Trading_Card_Game.png/800px-Logo_Pok%C3%A9mon_Trading_Card_Game.png';
const ENGLISH_LOGO_URL = 'assets/icons/Pokémon_log_en.png';

export {
  AUTH_DARK_BACKGROUND_VIDEO_SRC,
  AUTH_LIGHT_BACKGROUND_VIDEO_SRC,
};
export type { AppTheme as AuthTheme };

@Component({
  selector: 'app-auth-shell',
  templateUrl: './auth-shell.component.html',
  styleUrl: './auth-shell.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class AuthShellComponent implements AfterViewInit, OnDestroy {
  @ViewChild('backgroundVideo') private backgroundVideo?: ElementRef<HTMLVideoElement>;

  private readonly authMedia = inject(AuthMediaService);
  private readonly languageService = inject(LanguageService);
  private readonly appTheme = inject(AppThemeService);

  @Input({ required: true }) theme: AppTheme = 'LIGHT';
  @Input({ required: true }) skipLinkTarget = '#auth-form';
  @Input({ required: true }) panelLabelledBy = 'auth-title';
  @Input() skipLinkLabel = '';
  @Input() animationName = 'auth-enter';
  @Input() fullscreen = false;
  @Output() themeChange = new EventEmitter<AppTheme>();

  readonly lightBackgroundVideoSrc = AUTH_LIGHT_BACKGROUND_VIDEO_SRC;
  readonly logoUrl = computed(() =>
    this.languageService.isSpanish() ? SPANISH_LOGO_URL : ENGLISH_LOGO_URL,
  );
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);

  languageLabel(): string {
    return this.languageService.language().toUpperCase();
  }

  toggleLanguage(): void {
    void this.languageService.toggleLanguage();
  }

  ngAfterViewInit(): void {
    this.setBackgroundVideoSpeed(0.75);
  }

  ngOnDestroy(): void {
    this.saveBackgroundVideoTime();
  }

  onBackgroundVideoLoaded(): void {
    this.restoreBackgroundVideoTime();
    this.setBackgroundVideoSpeed(0.75);
  }

  @HostListener('document:click')
  @HostListener('document:keydown')
  enableBackgroundVideoAudio(): void {
    const video = this.backgroundVideo?.nativeElement;

    if (!video) {
      return;
    }

    video.volume = 1;
    this.playBackgroundVideo(video);
    this.authMedia.enableAudio(video.currentTime);
  }

  isDarkTheme(): boolean {
    return this.theme === 'DARK';
  }

  backgroundVideoSrc(): string {
    return this.isDarkTheme() ? AUTH_DARK_BACKGROUND_VIDEO_SRC : AUTH_LIGHT_BACKGROUND_VIDEO_SRC;
  }

  backgroundOverlayClasses(): string {
    return this.isDarkTheme() ? 'absolute inset-0 bg-black/0' : 'absolute inset-0 bg-black/10';
  }

  panelClasses(): string {
    const cardVariant = this.panelLabelledBy.includes('register')
      ? 'auth-card--register'
      : 'auth-card--login';
    const base = `auth-card ${cardVariant} w-full max-w-lg overflow-hidden rounded-xl shadow-2xl backdrop-blur-md motion-safe:animate-[${this.animationName}_300ms_ease-out]`;

    return this.isDarkTheme()
      ? `${base} border border-white/20 bg-black/65 text-white`
      : `${base} border border-white/40 bg-white/75 text-slate-950`;
  }

  mainClasses(): string {
    return this.fullscreen
      ? 'flex min-h-0 flex-1 items-stretch justify-center overflow-hidden'
      : 'auth-main flex flex-1 items-center justify-center px-4 pb-6 pt-36 sm:justify-end sm:pl-8 sm:pr-24 sm:pt-24 lg:pl-16 lg:pr-32 xl:pr-40';
  }

  contentClasses(): string {
    return this.fullscreen ? 'auth-fullscreen flex min-h-0 w-full flex-1 overflow-hidden' : this.panelClasses();
  }

  toggleTheme(): void {
    const nextTheme = this.isDarkTheme() ? 'LIGHT' : 'DARK';

    this.saveBackgroundVideoTime();
    this.appTheme.setTheme(nextTheme);
    this.themeChange.emit(nextTheme);

    window.setTimeout(() => {
      const video = this.backgroundVideo?.nativeElement;

      if (!video) {
        return;
      }

      video.load();
      this.restoreBackgroundVideoTime();
      this.setBackgroundVideoSpeed(0.75);
    });
  }

  private setBackgroundVideoSpeed(speed: number): void {
    const video = this.backgroundVideo?.nativeElement;

    if (!video) {
      return;
    }

    video.playbackRate = speed;
    video.muted = true;
    video.volume = 1;
    this.playBackgroundVideo(video);
    this.authMedia.playIfEnabled(video.currentTime);
  }

  private playBackgroundVideo(video: HTMLVideoElement): void {
    void video.play().catch(() => undefined);
  }

  private saveBackgroundVideoTime(): void {
    this.authMedia.saveBackgroundVideoTime(this.backgroundVideo?.nativeElement);
  }

  private restoreBackgroundVideoTime(): void {
    this.authMedia.restoreBackgroundVideoTime(this.backgroundVideo?.nativeElement);
  }
}
