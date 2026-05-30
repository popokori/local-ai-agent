import { Injectable } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { Preferences } from '@capacitor/preferences';

/**
 * Stockage des tokens JWT, abstrait sur Capacitor Preferences (mobile)
 * et localStorage (web). API entièrement asynchrone pour cohérence.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private static readonly ACCESS_KEY = 'access_token';
  private static readonly REFRESH_KEY = 'refresh_token';

  private readonly nativeCapacitor = Capacitor.isNativePlatform();

  async getAccessToken(): Promise<string | null> {
    return this.get(TokenStorageService.ACCESS_KEY);
  }

  async setAccessToken(token: string): Promise<void> {
    await this.set(TokenStorageService.ACCESS_KEY, token);
  }

  async getRefreshToken(): Promise<string | null> {
    return this.get(TokenStorageService.REFRESH_KEY);
  }

  async setRefreshToken(token: string): Promise<void> {
    await this.set(TokenStorageService.REFRESH_KEY, token);
  }

  async clear(): Promise<void> {
    if (this.nativeCapacitor) {
      await Preferences.remove({ key: TokenStorageService.ACCESS_KEY });
      await Preferences.remove({ key: TokenStorageService.REFRESH_KEY });
    } else {
      localStorage.removeItem(TokenStorageService.ACCESS_KEY);
      localStorage.removeItem(TokenStorageService.REFRESH_KEY);
    }
  }

  private async get(key: string): Promise<string | null> {
    if (this.nativeCapacitor) {
      const { value } = await Preferences.get({ key });
      return value ?? null;
    }
    return localStorage.getItem(key);
  }

  private async set(key: string, value: string): Promise<void> {
    if (this.nativeCapacitor) {
      await Preferences.set({ key, value });
    } else {
      localStorage.setItem(key, value);
    }
  }
}
