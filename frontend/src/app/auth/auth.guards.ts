import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Pages réservées aux utilisateurs connectés. */
export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return (await auth.ensureLoaded()) ? true : router.createUrlTree(['/login']);
};

/** Pages de connexion et d'inscription : inutiles si l'on est déjà connecté. */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return (await auth.ensureLoaded()) ? router.createUrlTree(['/books']) : true;
};
