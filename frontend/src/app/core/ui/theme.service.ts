import { Injectable, signal, effect } from '@angular/core';

export type ThemeMode = 'dark' | 'light' | 'auto';

const STORAGE_KEY = 'laa.theme';

/**
 * Gestion du thème dark/light/auto, persisté en localStorage et appliqué via
 * la classe `theme-dark` sur le body (cohérent avec theme/variables.scss).
 *
 * - `auto`  → suit `prefers-color-scheme` du système
 * - `dark`  → force dark
 * - `light` → force light
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _mode = signal<ThemeMode>(this.readStored());
  readonly mode = this._mode.asReadonly();

  private mediaQuery?: MediaQueryList;

  constructor() {
    // Effet : applique le thème dès qu'il change
    effect(() => {
      const mode = this._mode();
      this.applyEffective(mode);
      try { localStorage.setItem(STORAGE_KEY, mode); } catch { /* ignore */ }
    });

    // Suit le système quand mode=auto
    if (typeof window !== 'undefined' && window.matchMedia) {
      this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      this.mediaQuery.addEventListener('change', () => {
        if (this._mode() === 'auto') this.applyEffective('auto');
      });
    }
  }

  setMode(mode: ThemeMode): void {
    this._mode.set(mode);
  }

  /** Renvoie le thème effectif (résolu) pour l'affichage du toggle. */
  effective(): 'dark' | 'light' {
    const mode = this._mode();
    if (mode !== 'auto') return mode;
    return this.systemPrefersDark() ? 'dark' : 'light';
  }

  private applyEffective(mode: ThemeMode): void {
    const dark = mode === 'dark' || (mode === 'auto' && this.systemPrefersDark());
    document.body.classList.toggle('theme-dark', dark);
    // meta color-scheme pour la barre de couleur browser sur mobile
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
  }

  private systemPrefersDark(): boolean {
    return !!this.mediaQuery?.matches;
  }

  private readStored(): ThemeMode {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      if (v === 'dark' || v === 'light' || v === 'auto') return v;
    } catch { /* ignore */ }
    return 'dark'; // par défaut on force dark (UI plus moderne LLM-like)
  }
}
