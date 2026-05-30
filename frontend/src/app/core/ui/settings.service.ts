import { Injectable, signal } from '@angular/core';

import { environment } from '../../../environments/environment';

const KEY_API_URL = 'laa.apiUrl';
const KEY_MODEL = 'laa.defaultModel';

/**
 * Préférences applicatives persistées en localStorage :
 *   - URL backend (override de environment.apiUrl, utile en mobile/multi-machine)
 *   - Modèle LLM par défaut (juste affiché côté UI ; le backend reste maître
 *     sauf si on l'envoie dans CreateChatRequest.modelName)
 *
 * Important : la mutation d'environment.apiUrl est appelée AVANT le bootstrap
 * Angular via {@link applyBootEnvironment} pour que tous les services voient
 * la bonne URL dès leur instanciation.
 */
@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly _apiUrl = signal(SettingsService.readApiUrl());
  readonly apiUrl = this._apiUrl.asReadonly();

  private readonly _defaultModel = signal(SettingsService.readModel());
  readonly defaultModel = this._defaultModel.asReadonly();

  readonly defaultApiUrl = environment.apiUrl;

  setApiUrl(url: string): void {
    const v = url.trim().replace(/\/+$/, '');
    if (!v) return;
    try { localStorage.setItem(KEY_API_URL, v); } catch { /* ignore */ }
    this._apiUrl.set(v);
  }

  resetApiUrl(): void {
    try { localStorage.removeItem(KEY_API_URL); } catch { /* ignore */ }
    this._apiUrl.set(this.defaultApiUrl);
  }

  setDefaultModel(model: string): void {
    const v = model.trim();
    try { localStorage.setItem(KEY_MODEL, v); } catch { /* ignore */ }
    this._defaultModel.set(v);
  }

  // ─── Statics utilisés au bootstrap ─────────────────────────────────

  private static readApiUrl(): string {
    try {
      const v = localStorage.getItem(KEY_API_URL);
      if (v && v.trim()) return v.trim();
    } catch { /* ignore */ }
    return environment.apiUrl;
  }

  private static readModel(): string {
    try {
      return localStorage.getItem(KEY_MODEL) ?? '';
    } catch { return ''; }
  }
}

/**
 * À appeler depuis main.ts AVANT bootstrapApplication : mute environment.apiUrl
 * avec la valeur custom si l'utilisateur en avait défini une.
 */
export function applyBootEnvironment(): void {
  try {
    const stored = localStorage.getItem(KEY_API_URL);
    if (stored && stored.trim()) {
      (environment as { apiUrl: string }).apiUrl = stored.trim().replace(/\/+$/, '');
    }
  } catch { /* ignore */ }
}
