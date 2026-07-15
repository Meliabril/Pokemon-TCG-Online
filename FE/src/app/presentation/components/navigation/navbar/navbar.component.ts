import { NgClass } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { APP_ROUTES } from '../../../../core/constants/routing/routes.constants';
import { NavbarItem } from '../../../../core/models/interfaces/navigation/navbar-item.interface';
import { AppThemeService } from '../../../../core/services/app-theme.service';
import { LanguageService } from '../../../../core/services/language.service';
import { StorageService } from '../../../../core/storage/storage.service';
import { AuthApiService } from '../../../../infrastructure/api/auth/auth-api.service';
import { OakTutorialService } from '../../../../features/home/services/oak-tutorial.service';
import { avatarImageUrlFor } from '../../../../shared/ui/profile/avatar-picker/avatar-picker.component';

@Component({
  selector: 'app-navbar',
  imports: [NgClass, RouterLink, RouterLinkActive],
  templateUrl: './navbar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {
  readonly userMenuTriggerClosedClasses = '';
  readonly userMenuTriggerOpenClasses =
    'rounded-b-none rounded-t-[1.25rem] border-[#facc15]/35 bg-[#4a0b07]/96 shadow-[0_18px_40px_rgba(0,0,0,0.32)]';

  private readonly storageService = inject(StorageService);
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly appTheme = inject(AppThemeService);
  private readonly oakTutorial = inject(OakTutorialService);
  readonly languageService = inject(LanguageService);

  readonly routes = APP_ROUTES;
  readonly language = this.languageService.language;
  readonly languageLabel = this.languageService.label;
  readonly theme = this.appTheme.theme;
  readonly t = (key: string, params?: Record<string, string | number | boolean | null | undefined>) =>
    this.languageService.t(key, params);
  readonly currentUser = this.storageService.currentUser;
  readonly isUserMenuOpen = signal(false);
  readonly isLoggingOut = signal(false);

  getAvatarUrl(avatarId: string | null | undefined): string {
    return avatarImageUrlFor(avatarId);
  }
  readonly navItems = computed<NavbarItem[]>(() => {
    this.languageService.translations();

    if (this.storageService.isAuthenticated()) {
      return [
        { label: this.t('NAV.HOME'), route: APP_ROUTES.home, exact: true },
        { label: this.t('NAV.DECK'), route: APP_ROUTES.deck },
        { label: this.t('NAV.POKEDEX'), route: APP_ROUTES.pokedex }
      ];
    }

    return [
      { label: this.t('NAV.HOME'), route: APP_ROUTES.home, exact: true },
      { label: this.t('NAV.LOGIN'), route: APP_ROUTES.login },
      { label: this.t('NAV.REGISTER'), route: APP_ROUTES.register }
    ];
  });

  toLink(route: string): string {
    return route ? `/${route}` : '/';
  }

  tourTargetFor(route: string): string | null {
    const tourTargets: Partial<Record<string, string>> = {
      [APP_ROUTES.home]: 'nav-home',
      [APP_ROUTES.deck]: 'nav-deck',
      [APP_ROUTES.pokedex]: 'nav-pokedex'
    };

    return tourTargets[route] ?? null;
  }

  toggleUserMenu(): void {
    this.isUserMenuOpen.update((isOpen) => !isOpen);
  }

  toggleLanguage(event?: MouseEvent): void {
    event?.stopPropagation();
    this.languageService.toggleLanguage();
  }

  isDarkTheme(): boolean {
    return this.theme() === 'DARK';
  }

  toggleTheme(event?: MouseEvent): void {
    event?.stopPropagation();
    this.appTheme.toggleTheme();
  }

  closeUserMenu(): void {
    this.isUserMenuOpen.set(false);
  }

  startOakTutorial(): void {
    this.oakTutorial.start();
  }

  logout(): void {
    this.isLoggingOut.set(true);

    this.authApi.logout().subscribe({
      next: () => this.finishLogout(),
      error: () => {
        this.storageService.clearSession();
        this.finishLogout();
      }
    });
  }

  @HostListener('document:click', ['$event'])
  closeUserMenuOnOutsideClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeUserMenu();
    }
  }

  private finishLogout(): void {
    this.isLoggingOut.set(false);
    this.closeUserMenu();
    void this.router.navigate([this.toLink(this.routes.login)]);
  }
}
