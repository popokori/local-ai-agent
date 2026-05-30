import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import {
  LoginRequest,
  RegisterRequest,
  RefreshRequest,
  TokenResponse,
  UserDto,
} from './auth.types';

/**
 * Authentification : login/register/refresh/logout + signal `user`.
 * Au bootstrap, tente de charger /users/me si un access token est en storage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  private readonly _user = signal<UserDto | null>(null);
  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);

  private refreshInFlight: Promise<boolean> | null = null;

  /** Appelé au bootstrap pour récupérer la session si tokens en cache. */
  async restoreSession(): Promise<void> {
    const access = await this.tokenStorage.getAccessToken();
    if (!access) return;
    try {
      await this.loadProfile();
    } catch (err) {
      // Tente un refresh, sinon clear
      if (err instanceof HttpErrorResponse && err.status === 401) {
        const refreshed = await this.tryRefresh();
        if (refreshed) {
          try { await this.loadProfile(); } catch { await this.tokenStorage.clear(); }
        } else {
          await this.tokenStorage.clear();
        }
      } else {
        await this.tokenStorage.clear();
      }
    }
  }

  async login(payload: LoginRequest): Promise<UserDto> {
    const tokens = await firstValueFrom(
      this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, payload),
    );
    await this.persistTokens(tokens);
    return this.loadProfile();
  }

  async register(payload: RegisterRequest): Promise<UserDto> {
    const tokens = await firstValueFrom(
      this.http.post<TokenResponse>(`${environment.apiUrl}/auth/register`, payload),
    );
    await this.persistTokens(tokens);
    return this.loadProfile();
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}));
    } catch {
      // best-effort, le tokens locaux seront purgés quoi qu'il arrive
    }
    await this.tokenStorage.clear();
    this._user.set(null);
    await this.router.navigateByUrl('/login', { replaceUrl: true });
  }

  /**
   * Tente de rafraîchir l'access token. Mutex : si plusieurs requêtes 401
   * arrivent en même temps, on partage la promesse de refresh.
   */
  async tryRefresh(): Promise<boolean> {
    if (this.refreshInFlight) return this.refreshInFlight;
    this.refreshInFlight = (async () => {
      const refresh = await this.tokenStorage.getRefreshToken();
      if (!refresh) return false;
      try {
        const tokens = await firstValueFrom(
          this.http.post<TokenResponse>(
            `${environment.apiUrl}/auth/refresh`,
            { refreshToken: refresh } satisfies RefreshRequest,
          ),
        );
        await this.persistTokens(tokens);
        return true;
      } catch {
        await this.tokenStorage.clear();
        this._user.set(null);
        return false;
      }
    })();
    try {
      return await this.refreshInFlight;
    } finally {
      this.refreshInFlight = null;
    }
  }

  async loadProfile(): Promise<UserDto> {
    const me = await firstValueFrom(
      this.http.get<UserDto>(`${environment.apiUrl}/users/me`),
    );
    this._user.set(me);
    return me;
  }

  private async persistTokens(tokens: TokenResponse): Promise<void> {
    await this.tokenStorage.setAccessToken(tokens.accessToken);
    await this.tokenStorage.setRefreshToken(tokens.refreshToken);
  }
}
