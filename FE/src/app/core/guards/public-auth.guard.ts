import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { APP_ROUTES } from '../constants/routing/routes.constants';
import { StorageService } from '../storage/storage.service';

export const publicAuthGuard: CanActivateFn = () => {
  const storageService = inject(StorageService);
  const router = inject(Router);

  if (storageService.authStatus() === 'checking') {
    return storageService.awaitAuthCheck().then((status) => {
      if (status !== 'authenticated') {
        return true;
      }

      return router.createUrlTree([`/${APP_ROUTES.home}`]);
    });
  }

  if (!storageService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree([`/${APP_ROUTES.home}`]);
};
