import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonItem, IonInput, IonTextarea, IonBackButton, IonSpinner,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  documentTextOutline, flaskOutline, leafOutline, calculatorOutline,
  medkitOutline, codeSlashOutline, planetOutline, libraryOutline,
} from 'ionicons/icons';

import { KbService } from './kb.service';
import { Domain } from './kb.types';
import { ToastService } from '../../core/ui/toast.service';

interface DomainOption {
  value: Domain;
  label: string;
  icon: string;
  gradient: string;
}

@Component({
  selector: 'app-kb-create',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonItem, IonInput, IonTextarea, IonBackButton, IonSpinner,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }
    .wrap { max-width: 640px; margin: 0 auto; padding: 32px 20px 96px; }
    h1 { font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; margin: 0 0 4px; }
    p.sub { color: var(--ion-color-step-500); margin: 0 0 24px; }
    .field-label {
      font-size: 0.8rem; font-weight: 600;
      color: var(--ion-color-step-700);
      text-transform: uppercase; letter-spacing: 0.06em;
      margin: 16px 0 8px;
    }
    ion-item {
      --background: var(--ion-card-background);
      --border-radius: var(--laa-radius-md);
      --inner-padding-end: 12px; --padding-start: 14px;
      border-radius: var(--laa-radius-md);
      overflow: hidden;
      margin-bottom: 8px;
    }
    .domains {
      display: grid; gap: 10px;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    }
    .domain-card {
      background: var(--ion-card-background);
      border: 2px solid transparent;
      border-radius: var(--laa-radius-lg);
      padding: 14px;
      cursor: pointer;
      transition: border-color 0.15s, transform 0.15s;
      display: flex; align-items: center; gap: 10px;
    }
    .domain-card:hover { transform: translateY(-2px); }
    .domain-card.selected { border-color: var(--ion-color-primary); }
    .domain-icon {
      width: 32px; height: 32px;
      display: grid; place-items: center;
      border-radius: 8px;
      color: #fff; flex-shrink: 0;
    }
    .domain-icon ion-icon { font-size: 16px; }
    .domain-card span { font-size: 0.85rem; font-weight: 600; }
    .actions {
      margin-top: 32px;
      display: flex; gap: 8px; flex-wrap: wrap;
    }
    .actions ion-button { flex: 1; min-width: 160px; }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button defaultHref="/kbs"></ion-back-button>
        </ion-buttons>
        <ion-title>Nouvelle KB</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <div class="wrap">
        <h1>Créer une base documentaire</h1>
        <p class="sub">Donne-lui un nom, choisis un domaine, puis tu pourras uploader tes documents.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <p class="field-label">Nom</p>
          <ion-item lines="none">
            <ion-input placeholder="Ex. Documentation produit" formControlName="name" [clearInput]="true"></ion-input>
          </ion-item>

          <p class="field-label">Description (optionnelle)</p>
          <ion-item lines="none">
            <ion-textarea
              placeholder="Quelques lignes pour t'aider à t'y retrouver"
              formControlName="description"
              autoGrow="true"
              rows="2"></ion-textarea>
          </ion-item>

          <p class="field-label">Domaine</p>
          <div class="domains">
            @for (d of domains; track d.value) {
              <div
                class="domain-card"
                [class.selected]="form.controls.domain.value === d.value"
                (click)="form.controls.domain.setValue(d.value)">
                <div class="domain-icon" [style.background]="d.gradient">
                  <ion-icon [name]="d.icon"></ion-icon>
                </div>
                <span>{{ d.label }}</span>
              </div>
            }
          </div>

          <div class="actions">
            <ion-button fill="outline" type="button" (click)="cancel()">Annuler</ion-button>
            <ion-button type="submit" [disabled]="loading() || form.invalid">
              @if (loading()) {
                <ion-spinner name="dots"></ion-spinner>
              } @else {
                Créer la KB
              }
            </ion-button>
          </div>
        </form>
      </div>
    </ion-content>
  `,
})
export class KbCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly kb = inject(KbService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly domains: DomainOption[] = [
    { value: 'GENERIC',          label: 'Général',     icon: 'library-outline',      gradient: 'linear-gradient(135deg, #6366f1, #818cf8)' },
    { value: 'SCIENCE',          label: 'Science',     icon: 'planet-outline',       gradient: 'linear-gradient(135deg, #6366f1, #14b8a6)' },
    { value: 'BIOLOGY',          label: 'Biologie',    icon: 'leaf-outline',         gradient: 'linear-gradient(135deg, #10b981, #34d399)' },
    { value: 'CHEMISTRY',        label: 'Chimie',      icon: 'flask-outline',        gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)' },
    { value: 'MATHEMATICS',      label: 'Maths',       icon: 'calculator-outline',   gradient: 'linear-gradient(135deg, #8b5cf6, #a78bfa)' },
    { value: 'MEDICAL',          label: 'Médical',     icon: 'medkit-outline',       gradient: 'linear-gradient(135deg, #ef4444, #f87171)' },
    { value: 'COMPUTER_SCIENCE', label: 'Informatique',icon: 'code-slash-outline',   gradient: 'linear-gradient(135deg, #06b6d4, #67e8f9)' },
  ];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    domain: ['GENERIC' as Domain, [Validators.required]],
  });

  readonly loading = signal(false);

  constructor() {
    addIcons({
      documentTextOutline, flaskOutline, leafOutline, calculatorOutline,
      medkitOutline, codeSlashOutline, planetOutline, libraryOutline,
    });
  }

  async submit(): Promise<void> {
    if (this.loading() || this.form.invalid) return;
    this.loading.set(true);
    try {
      const v = this.form.getRawValue();
      const created = await this.kb.create({
        name: v.name.trim(),
        description: v.description?.trim() || undefined,
        domain: v.domain,
      });
      await this.toast.success('KB créée ✓');
      await this.router.navigate(['/kbs', created.id]);
    } catch (err) {
      await this.toast.error(err, 'Création impossible.');
    } finally {
      this.loading.set(false);
    }
  }

  cancel(): void {
    this.router.navigate(['/kbs']);
  }
}
