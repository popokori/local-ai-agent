import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonMenuButton, IonItem, IonInput, IonSpinner, IonNote,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  moonOutline, sunnyOutline, contrastOutline, personOutline, lockClosedOutline,
  sparklesOutline, serverOutline, checkmarkOutline, refreshOutline,
  arrowForwardOutline, informationCircleOutline,
} from 'ionicons/icons';

import { AuthService } from '../../core/auth/auth.service';
import { UserService } from '../../core/auth/user.service';
import { ThemeService, ThemeMode } from '../../core/ui/theme.service';
import { SettingsService } from '../../core/ui/settings.service';
import { ToastService } from '../../core/ui/toast.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonMenuButton, IonItem, IonInput, IonSpinner, IonNote,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }
    .wrap { max-width: 720px; margin: 0 auto; padding: 24px 16px 96px; }

    h1.title { font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; margin: 0 0 4px; }
    p.sub { color: var(--ion-color-step-500); margin: 0 0 24px; }

    .section {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-lg);
      padding: 18px;
      margin-bottom: 16px;
    }
    .section-head {
      display: flex; align-items: center; gap: 10px;
      margin-bottom: 14px;
    }
    .section-icon {
      width: 32px; height: 32px;
      display: grid; place-items: center;
      border-radius: 10px;
      color: #fff;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-brand-soft) 100%);
      flex-shrink: 0;
    }
    .section-icon ion-icon { font-size: 16px; }
    .section-head h2 { margin: 0; font-size: 1rem; font-weight: 600; }
    .section-head p  { margin: 0; font-size: 0.78rem; color: var(--ion-color-step-500); }

    /* Theme toggle */
    .theme-cards {
      display: grid; gap: 8px;
      grid-template-columns: repeat(3, 1fr);
    }
    .theme-card {
      background: var(--ion-color-step-50);
      border: 2px solid transparent;
      border-radius: var(--laa-radius-md);
      padding: 12px 8px;
      cursor: pointer;
      text-align: center;
      transition: border-color 0.15s;
    }
    .theme-card:hover { background: var(--ion-color-step-100); }
    .theme-card.selected { border-color: var(--ion-color-primary); }
    .theme-card ion-icon { font-size: 20px; color: var(--ion-color-primary); }
    .theme-card .label { display: block; margin-top: 4px; font-size: 0.78rem; font-weight: 600; }

    /* Forms compactes */
    ion-item {
      --background: var(--ion-color-step-50);
      --border-radius: var(--laa-radius-md);
      --inner-padding-end: 12px; --padding-start: 14px;
      border-radius: var(--laa-radius-md);
      overflow: hidden;
      margin-bottom: 10px;
    }
    .form-actions { display: flex; gap: 8px; margin-top: 6px; }
    .form-actions ion-button { flex: 1; }

    .info-row {
      display: flex; align-items: center; justify-content: space-between;
      padding: 8px 0;
      font-size: 0.85rem;
      border-bottom: 1px solid var(--ion-color-step-50);
    }
    .info-row:last-child { border-bottom: none; }
    .info-row .k { color: var(--ion-color-step-500); }
    .info-row .v {
      font-family: var(--laa-font-mono);
      font-size: 0.8rem;
      color: var(--ion-text-color);
    }
    .api-badge {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600;
      background: rgba(16,185,129,0.16); color: #34d399;
      margin-left: 6px;
    }
    .api-badge.custom { background: rgba(245,158,11,0.16); color: #fbbf24; }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>Réglages</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <div class="wrap">
        <h1 class="title">Préférences</h1>
        <p class="sub">Adapte l'apparence, ton profil et la connexion au backend.</p>

        <!-- ─── Apparence ──────────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="contrast-outline"></ion-icon></div>
            <div>
              <h2>Apparence</h2>
              <p>Thème de l'interface (persisté)</p>
            </div>
          </div>
          <div class="theme-cards">
            <div
              class="theme-card"
              [class.selected]="theme.mode() === 'light'"
              (click)="theme.setMode('light')">
              <ion-icon name="sunny-outline"></ion-icon>
              <span class="label">Clair</span>
            </div>
            <div
              class="theme-card"
              [class.selected]="theme.mode() === 'dark'"
              (click)="theme.setMode('dark')">
              <ion-icon name="moon-outline"></ion-icon>
              <span class="label">Sombre</span>
            </div>
            <div
              class="theme-card"
              [class.selected]="theme.mode() === 'auto'"
              (click)="theme.setMode('auto')">
              <ion-icon name="contrast-outline"></ion-icon>
              <span class="label">Système</span>
            </div>
          </div>
        </div>

        <!-- ─── Profil ───────────────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="person-outline"></ion-icon></div>
            <div>
              <h2>Profil</h2>
              <p>Modifie tes informations personnelles</p>
            </div>
          </div>
          <form [formGroup]="profileForm" (ngSubmit)="saveProfile()">
            <ion-item lines="none">
              <ion-input
                label="Nom affiché"
                labelPlacement="floating"
                formControlName="displayName"
                [clearInput]="true"></ion-input>
            </ion-item>
            <ion-item lines="none">
              <ion-input
                label="Email"
                labelPlacement="floating"
                type="email"
                formControlName="email"
                [clearInput]="true"></ion-input>
            </ion-item>
            <div class="form-actions">
              <ion-button
                type="submit"
                fill="solid"
                [disabled]="savingProfile() || profileForm.invalid || !profileForm.dirty">
                @if (savingProfile()) {
                  <ion-spinner name="dots"></ion-spinner>
                } @else {
                  <ion-icon slot="start" name="checkmark-outline"></ion-icon>
                  Sauvegarder
                }
              </ion-button>
            </div>
          </form>
        </div>

        <!-- ─── Mot de passe ─────────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="lock-closed-outline"></ion-icon></div>
            <div>
              <h2>Mot de passe</h2>
              <p>Change ton mot de passe (8 caractères minimum)</p>
            </div>
          </div>
          <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
            <ion-item lines="none">
              <ion-input
                label="Mot de passe actuel"
                labelPlacement="floating"
                type="password"
                formControlName="currentPassword"
                autocomplete="current-password"></ion-input>
            </ion-item>
            <ion-item lines="none">
              <ion-input
                label="Nouveau mot de passe"
                labelPlacement="floating"
                type="password"
                formControlName="newPassword"
                autocomplete="new-password"></ion-input>
            </ion-item>
            <div class="form-actions">
              <ion-button
                type="submit"
                fill="solid"
                [disabled]="changingPwd() || passwordForm.invalid">
                @if (changingPwd()) {
                  <ion-spinner name="dots"></ion-spinner>
                } @else {
                  <ion-icon slot="start" name="checkmark-outline"></ion-icon>
                  Changer le mot de passe
                }
              </ion-button>
            </div>
          </form>
        </div>

        <!-- ─── LLM par défaut ──────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="sparkles-outline"></ion-icon></div>
            <div>
              <h2>Modèle LLM préféré</h2>
              <p>Utilisé pour les nouvelles conversations (override possible côté backend)</p>
            </div>
          </div>
          <ion-item lines="none">
            <ion-input
              label="Nom du modèle Ollama"
              labelPlacement="floating"
              placeholder="ex. dolphin-mistral:7b"
              [value]="settings.defaultModel()"
              (ionInput)="onModelChange($any($event).detail.value ?? '')"></ion-input>
          </ion-item>
          <ion-note color="medium">
            Le modèle réellement utilisé reste celui configuré côté backend
            (variable d'env LLM_DEFAULT_MODEL). Cette valeur sert juste de
            préférence affichée côté UI pour les futures sessions.
          </ion-note>
        </div>

        <!-- ─── Backend URL ─────────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="server-outline"></ion-icon></div>
            <div>
              <h2>
                URL Backend
                @if (apiUrlIsCustom()) {
                  <span class="api-badge custom">custom</span>
                } @else {
                  <span class="api-badge">défaut</span>
                }
              </h2>
              <p>Change le serveur appelé (utile sur mobile ou multi-machine)</p>
            </div>
          </div>
          <ion-item lines="none">
            <ion-input
              label="URL complète /api/v1"
              labelPlacement="floating"
              type="url"
              [value]="apiUrlDraft()"
              (ionInput)="apiUrlDraft.set($any($event).detail.value ?? '')"
              [clearInput]="true"></ion-input>
          </ion-item>
          <div class="form-actions">
            <ion-button
              fill="outline"
              (click)="resetApiUrl()"
              [disabled]="!apiUrlIsCustom()">
              <ion-icon slot="start" name="refresh-outline"></ion-icon>
              Réinitialiser
            </ion-button>
            <ion-button
              fill="solid"
              (click)="saveApiUrl()"
              [disabled]="!canApplyUrl()">
              <ion-icon slot="start" name="arrow-forward-outline"></ion-icon>
              Appliquer + recharger
            </ion-button>
          </div>
          <ion-note color="medium">
            URL par défaut (build) : <code>{{ settings.defaultApiUrl }}</code>
          </ion-note>
        </div>

        <!-- ─── Infos ───────────────────────────────────────────── -->
        <div class="section">
          <div class="section-head">
            <div class="section-icon"><ion-icon name="information-circle-outline"></ion-icon></div>
            <div>
              <h2>À propos</h2>
              <p>Informations applicatives</p>
            </div>
          </div>
          <div class="info-row">
            <span class="k">Build</span>
            <span class="v">{{ env.production ? 'production' : 'développement' }}</span>
          </div>
          <div class="info-row">
            <span class="k">API actuelle</span>
            <span class="v">{{ env.apiUrl }}</span>
          </div>
          <div class="info-row">
            <span class="k">Utilisateur</span>
            <span class="v">{{ auth.user()?.username }} (id {{ auth.user()?.id }})</span>
          </div>
          <div class="info-row">
            <span class="k">Rôles</span>
            <span class="v">{{ auth.user()?.roles?.join(', ') ?? '-' }}</span>
          </div>
        </div>
      </div>
    </ion-content>
  `,
})
export class SettingsPage implements OnInit {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly settings = inject(SettingsService);
  private readonly users = inject(UserService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  readonly env = environment;

  // Profil
  readonly profileForm = this.fb.nonNullable.group({
    displayName: [''],
    email: ['', [Validators.required, Validators.email]],
  });
  readonly savingProfile = signal(false);

  // Password
  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });
  readonly changingPwd = signal(false);

  // Backend URL
  readonly apiUrlDraft = signal('');
  readonly apiUrlIsCustom = computed(
    () => this.settings.apiUrl() !== this.settings.defaultApiUrl,
  );
  readonly canApplyUrl = computed(() => {
    const v = this.apiUrlDraft().trim();
    return v.length > 0 && v !== this.settings.apiUrl();
  });

  constructor() {
    addIcons({
      moonOutline, sunnyOutline, contrastOutline, personOutline, lockClosedOutline,
      sparklesOutline, serverOutline, checkmarkOutline, refreshOutline,
      arrowForwardOutline, informationCircleOutline,
    });
  }

  ngOnInit(): void {
    const u = this.auth.user();
    this.profileForm.patchValue({
      displayName: u?.displayName ?? '',
      email: u?.email ?? '',
    });
    this.apiUrlDraft.set(this.settings.apiUrl());
  }

  async saveProfile(): Promise<void> {
    if (this.profileForm.invalid || this.savingProfile()) return;
    this.savingProfile.set(true);
    try {
      const v = this.profileForm.getRawValue();
      await this.users.updateMe({
        displayName: v.displayName.trim() || undefined,
        email: v.email.trim(),
      });
      this.profileForm.markAsPristine();
      await this.toast.success('Profil mis à jour ✓');
    } catch (err) {
      await this.toast.error(err);
    } finally {
      this.savingProfile.set(false);
    }
  }

  async changePassword(): Promise<void> {
    if (this.passwordForm.invalid || this.changingPwd()) return;
    this.changingPwd.set(true);
    try {
      const v = this.passwordForm.getRawValue();
      await this.users.changePassword({
        currentPassword: v.currentPassword,
        newPassword: v.newPassword,
      });
      this.passwordForm.reset({ currentPassword: '', newPassword: '' });
      await this.toast.success('Mot de passe changé ✓');
    } catch (err) {
      await this.toast.error(err, 'Mot de passe actuel incorrect ?');
    } finally {
      this.changingPwd.set(false);
    }
  }

  onModelChange(value: string): void {
    this.settings.setDefaultModel(value);
  }

  saveApiUrl(): void {
    const url = this.apiUrlDraft().trim();
    if (!url || url === this.settings.apiUrl()) return;
    if (!confirm(`Appliquer la nouvelle URL backend ?\n\n${url}\n\nLa page sera rechargée.`)) return;
    this.settings.setApiUrl(url);
    setTimeout(() => location.reload(), 200);
  }

  resetApiUrl(): void {
    if (!confirm('Revenir à l\'URL par défaut ?\n\nLa page sera rechargée.')) return;
    this.settings.resetApiUrl();
    this.apiUrlDraft.set(this.settings.defaultApiUrl);
    setTimeout(() => location.reload(), 200);
  }
}
