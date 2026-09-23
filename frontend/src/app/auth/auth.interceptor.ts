import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Session expirée pendant l'utilisation : retour à la page de connexion. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        req.url.startsWith('/api/') &&
        !req.url.startsWith('/api/auth/')
      ) {
        auth.clear();
        void router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
