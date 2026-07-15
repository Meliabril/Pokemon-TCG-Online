import { Routes } from '@angular/router';
import { APP_ROUTES } from '../core/constants/routing/routes.constants';
import { authGuard } from '../core/guards/auth.guard';
import { publicAuthGuard } from '../core/guards/public-auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: APP_ROUTES.login
  },
  {
    path: APP_ROUTES.home,
    pathMatch: 'full',
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/home/pages/home-page/home-page.component').then((m) => m.HomePageComponent)
  },
  {
    path: APP_ROUTES.login,
    canActivate: [publicAuthGuard],
    loadComponent: () =>
      import('../features/auth/pages/login-page/login-page.component').then((m) => m.LoginPageComponent)
  },
  {
    path: APP_ROUTES.register,
    canActivate: [publicAuthGuard],
    loadComponent: () =>
      import('../features/auth/pages/register-page/register-page.component').then(
        (m) => m.RegisterPageComponent
      )
  },
  {
    path: APP_ROUTES.verifyAccount,
    loadComponent: () =>
      import('../features/auth/pages/verify-account-page/verify-account-page.component').then(
        (m) => m.VerifyAccountPageComponent
      )
  },
  {
    path: APP_ROUTES.pokedex,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/pokedex/pages/pokedex-page/pokedex-page').then((m) => m.PokedexPage)
  },
  {
    path: APP_ROUTES.deck,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/deck/pages/deck-page/deck-page.component').then((m) => m.DeckPageComponent)
  },

  {
    path: APP_ROUTES.playRoom,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/play/pages/play-room-page/play-room-page.component').then(
        (m) => m.PlayRoomPageComponent
      )
  },

  {
    path: APP_ROUTES.profile,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/profile/pages/profile-page/profile-page.component').then(
        (m) => m.ProfilePageComponent
      )
  },
  {
    path: APP_ROUTES.game,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/game/pages/game-page/game-page.component').then((m) => m.GamePageComponent)
  },
  {
    path: `${APP_ROUTES.game}/:gameId`,
    canActivate: [authGuard],
    loadComponent: () =>
      import('../features/game/pages/game-page/game-page.component').then((m) => m.GamePageComponent)
  },
  {
    path: 'dev/animations',
    loadComponent: () =>
      import('../features/game/pages/animation-playground-page/animation-playground-page.component').then(
        (m) => m.AnimationPlaygroundPageComponent
      )
  },
  {
    path: '**',
    redirectTo: APP_ROUTES.login
  }
];
