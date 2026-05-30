import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  IonContent, IonButton, IonInput, IonSpinner, IonIcon, IonItem,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  sparklesOutline, personOutline, mailOutline, lockClosedOutline, idCardOutline,
  arrowBackOutline,
} from 'ionicons/icons';

import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/ui/toast.service';

@Component({
  selector: 'app-register',
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
        radial-gradient(1200px 600px at 80% 0%, rgba(20,184,166,0.18), transparent 60%),
        radial-gradient(800px 500px at 0% 100%, rgba(99,102,241,0.12), transparent 60%);
    }
    .auth-card {
      width: 100%; max-width: 460px;
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.12);
      border-radius: var(--laa-radius-xl);
      padding: 36px 28px 28px;
      box-shadow: 0 20px 60px -20px rgba(0,0,0,0.4);
    }
    .brand {
      display: flex; align-items: center; gap: 10px;
      margin-bottom: 22px;
    }
    .brand-mark {
      width: 38px; height: 38px;
      display: grid; place-items: center;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--laa-accent) 0%, var(--laa-brand) 100%);
      color: #fff;
    }
    .brand-mark ion-icon { font-size: 22px; }
    .brand-name { font-weight: 700; font-size: 18px; letter-spacing: -0.01em; }
    .brand-sub  { font-size: 12px; color: var(--ion-color-step-500); }
    h1.title { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.02em; margin: 6px 0 4px; }
    p.subtitle { color: var(--ion-color-step-500); margin: 0 0 20px; }
    ion-item {
      --background: var(--ion-color-step-50);
      --border-radius: var(--laa-radius-md);
      --inner-padding-end: 12px;
      --padding-start: 12px;
      margin-bottom: 10px;
      border-radius: var(--laa-radius-md);
      overflow: hidden;
    }
    .actions { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
    .meta { text-align: center; color: var(--ion-color-step-500); font-size: 0.85rem; margin-top: 16px; }
    .meta a { color: var(--ion-color-primary); text-decoration: none; font-weight: 600; }
    .back-link {
      display: inline-flex; align-items: center; gap: 6px;
      color: var(--ion-color-step-500); text-decoration: none;
      font-size: 0.85rem; margin-bottom: 12px;
    }
    .back-link ion-icon { font-size: 16px; }
  `],
  template: `
    <ion-content [fullscreen]="true">
      <div class="auth-shell">
        <div class="auth-card">
          <a class="back-link" routerLink="/login">
            <ion-icon name="arrow-back-outline"></ion-icon> Retour
          </a>

          <div class="brand">
            <div class="brand-mark"><ion-icon name="sparkles-outline"></ion-icon></div>
            <div>
              <div class="brand-name">LocalAiAgent</div>
              <div class="brand-sub">Bienvenue à bord 🚀</div>
            </div>
          </div>

          <h1 class="title">Créer un compte</h1>
          <p class="subtitle">Quelques infos et tu peux discuter avec ton assistant.</p>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <ion-item lines="none">
              <ion-icon slot="start" name="person-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Nom d'utilisateur"
                labelPlacement="floating"
                formControlName="username"
                autocomplete="username"
                autocapitalize="off"></ion-input>
            </ion-item>

            <ion-item lines="none">
              <ion-icon slot="start" name="mail-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Email"
                labelPlacement="floating"
                type="email"
                formControlName="email"
                autocomplete="email"
                autocapitalize="off"></ion-input>
            </ion-item>

            <ion-item lines="none">
              <ion-icon slot="start" name="id-card-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Nom complet (optionnel)"
                labelPlacement="floating"
                formControlName="displayName"></ion-input>
            </ion-item>

            <ion-item lines="none">
              <ion-icon slot="start" name="lock-closed-outline" aria-hidden="true"></ion-icon>
              <ion-input
                label="Mot de passe (8+ caractères)"
                labelPlacement="floating"
                type="password"
                formControlName="password"
                autocomplete="new-password"></ion-input>
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
                  Créer mon compte
                }
              </ion-button>
            </div>

            <div class="meta">
              Déjà un compte ?
              <a routerLink="/login">Se connecter</a>
            </div>
          </form>
        </div>
      </div>
    </ion-content>
  `,
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(64)]],
    email: ['', [Validators.required, Validators.email]],
    displayName: [''],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });

  readonly loading = signal(false);

  constructor() {
    addIcons({ sparklesOutline, personOutline, mailOutline, lockClosedOutline, idCardOutline, arrowBackOutline });
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    try {
      const v = this.form.getRawValue();
      await this.auth.register({
        username: v.username,
        email: v.email,
        password: v.password,
        displayName: v.displayName || undefined,
      });
      await this.toast.success('Compte créé ✓');
      await this.router.navigateByUrl('/chats', { replaceUrl: true });
    } catch (err) {
      await this.toast.error(err, 'Inscription impossible.');
    } finally {
      this.loading.set(false);
    }
  }
}
