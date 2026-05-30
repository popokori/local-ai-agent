import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  IonContent, IonButton, IonInput, IonSpinner, IonIcon, IonItem,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { sparklesOutline, lockClosedOutline, personOutline } from 'ionicons/icons';

import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    IonContent, IonButton, IonInput, IonSpinner, IonIcon, IonItem,
  ],
  styles: [`
    :host { display: contents; }
    .auth-shell {
      min-height: 100%;
      display: flex; align-items: center; justify-content: center;
      padding: 32px 20px;
      background:
        radial-gradient(1200px 600px at 20% 0%, rgba(99,102,241,0.18), transparent 60%),
        radial-gradient(800px 500px at 100% 100%, rgba(20,184,166,0.12), transparent 60%);
    }
    .auth-card {
      width: 100%; max-width: 420px;
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.12);
      border-radius: var(--laa-radius-xl);
      padding: 36px 28px 28px;
      box-shadow: 0 20px 60px -20px rgba(0,0,0,0.4);
    }
    .brand {
      display: flex; align-items: center; gap: 10px;
      margin-bottom: 28px;
    }
    .brand-mark {
      width: 38px; height: 38px;
      display: grid; place-items: center;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-accent) 100%);
      color: #fff;
    }
    .brand-mark ion-icon { font-size: 22px; }
    .brand-name { font-weight: 700; font-size: 18px; letter-spacing: -0.01em; }
    .brand-sub  { font-size: 12px; color: var(--ion-color-step-500); }
    h1.title {
      font-size: 1.6rem; font-weight: 700;
      letter-spacing: -0.02em;
      margin: 6px 0 4px;
    }
    p.subtitle { color: var(--ion-color-step-500); margin: 0 0 24px; }
    ion-item {
      --background: var(--ion-color-step-50);
      --border-radius: var(--laa-radius-md);
      --inner-padding-end: 12px;
      --padding-start: 12px;
      margin-bottom: 12px;
      border-radius: var(--laa-radius-md);
      overflow: hidden;
    }
    .actions { margin-top: 20px; display: flex; flex-direction: column; gap: 8px; }
    .meta { text-align: center; color: var(--ion-color-step-500); font-size: 0.85rem; margin-top: 18px; }
    .meta a { color: var(--ion-color-primary); text-decoration: none; font-weight: 600; }
  `],
  template: `
    <ion-content [fullscreen]="true">
      <div class="auth-shell">
        <div class="auth-card">
          <div class="brand">
            <div class="brand-mark"><ion-icon name="sparkles-outline"></ion-icon></div>
            <div>
              <div class="brand-name">LocalAiAgent</div>
              <div class="brand-sub">Assistant IA local, multi-domaines</div>
            </div>
          </div>

          <h1 class="title">Bon retour 👋</h1>
          <p class="subtitle">Connecte-toi pour reprendre tes conversations.</p>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <ion-item lines="none">
              <ion-icon slot="start" name="person-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Nom d'utilisateur"
                labelPlacement="floating"
                formControlName="username"
                autocomplete="username"
                autocapitalize="off"
                [clearInput]="true"></ion-input>
            </ion-item>

            <ion-item lines="none">
              <ion-icon slot="start" name="lock-closed-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Mot de passe"
                labelPlacement="floating"
                type="password"
                formControlName="password"
                autocomplete="current-password"></ion-input>
            </ion-item>

            <div class="actions">
              <ion-button
                expand="block"
                type="submit"
                [disabled]="loading() || form.invalid"
                size="large">
                @if (loading()) {
                  <ion-spinner name="dots"></ion-spinner>
                } @else {
                  Se connecter
                }
              </ion-button>
            </div>

            <div class="meta">
              Pas encore de compte ?
              <a routerLink="/register">Créer un compte</a>
            </div>
          </form>
        </div>
      </div>
    </ion-content>
  `,
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly loading = signal(false);

  constructor() {
    addIcons({ sparklesOutline, lockClosedOutline, personOutline });
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    try {
      await this.auth.login(this.form.getRawValue());
      const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/chats';
      await this.router.navigateByUrl(redirect, { replaceUrl: true });
    } catch (err) {
      await this.toast.error(err, 'Identifiants invalides.');
    } finally {
      this.loading.set(false);
    }
  }
}
