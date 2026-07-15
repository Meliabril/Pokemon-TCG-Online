import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StorageService } from '../storage/storage.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const storageService = inject(StorageService);
  const router = inject(Router);

  if (storageService.authStatus() === 'checking') {
    return storageService.awaitAuthCheck().then((status) => {
      if (status === 'authenticated') {
        return true;
      }

      return router.createUrlTree(['/auth/login'], {
        queryParams: { returnUrl: state.url }
      });
    });
  }

  if (storageService.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url }
  });
};
