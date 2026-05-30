import { HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { from, Observable, switchMap, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import { AuthService } from './auth.service';

/**
 * Ajoute le Bearer access token sur les requêtes vers le backend.
 * Sur 401 : tente UN refresh puis replay. Si refresh échoue → /login.
 *
 * Les endpoints d'auth eux-mêmes sont skippés (sinon boucle infinie sur 401 du login).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiRequest(req.url)) return next(req);
  if (isAuthEndpoint(req.url)) return next(req);

  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  return from(tokenStorage.getAccessToken()).pipe(
    switchMap(token => {
      const authed = token ? withBearer(req, token) : req;
      return next(authed).pipe(
        catchError((err: unknown) => {
          if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
            return throwError(() => err);
          }
          return handle401(req, next, authService, tokenStorage, router);
        }),
      );
    }),
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  tokenStorage: TokenStorageService,
  router: Router,
): Observable<any> {
  return from(authService.tryRefresh()).pipe(
    switchMap(async refreshed => {
      if (!refreshed) {
        await router.navigateByUrl('/login', { replaceUrl: true });
        throw new Error('Session expired');
      }
      const newToken = await tokenStorage.getAccessToken();
      return newToken;
    }),
    switchMap(newToken => next(newToken ? withBearer(req, newToken) : req)),
  );
}

function withBearer(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function isApiRequest(url: string): boolean {
  return url.startsWith(environment.apiUrl) || url.startsWith('/api/');
}

function isAuthEndpoint(url: string): boolean {
  return /\/auth\/(login|register|refresh)$/.test(url);
}
