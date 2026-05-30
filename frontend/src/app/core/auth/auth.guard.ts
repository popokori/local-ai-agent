import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Guard fonctionnel : laisse passer si user signal est non null, sinon
 * tente une restauration de session ; sinon redirect /login.
 */
export const authGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) return true;

  // Tentative de restaurer la session (au cas où on arrive ici via deep link)
  await authService.restoreSession();
  if (authService.isLoggedIn()) return true;

  return router.createUrlTree(['/login'], {
    queryParams: { redirect: state.url },
  });
};
