import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonItem, IonInput, IonBackButton, IonSpinner,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  closeOutline, sparklesOutline, flashOutline, shieldCheckmarkOutline,
  libraryOutline, addOutline,
} from 'ionicons/icons';

import { ChatService } from './chat.service';
import { ChatMode } from './chat.types';
import { ToastService } from '../../core/ui/toast.service';
import { KbService } from '../kb/kb.service';

interface ModeOption {
  value: ChatMode;
  label: string;
  description: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-chat-create',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterLink,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonItem, IonInput, IonBackButton, IonSpinner,
  ],
  styles: [`
    :host { display: contents; }
    ion-content { --background: var(--ion-background-color); }
    .wrap {
      max-width: 640px; margin: 0 auto;
      padding: 32px 20px 96px;
    }
    h1 {
      font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em;
      margin: 0 0 4px;
    }
    p.sub { color: var(--ion-color-step-500); margin: 0 0 24px; }

    .field-label {
      font-size: 0.8rem; font-weight: 600;
      color: var(--ion-color-step-700);
      text-transform: uppercase; letter-spacing: 0.06em;
      margin: 0 0 8px;
    }

    ion-item.title-input {
      --background: var(--ion-card-background);
      --border-radius: var(--laa-radius-md);
      --inner-padding-end: 12px;
      --padding-start: 14px;
      border-radius: var(--laa-radius-md);
      overflow: hidden;
      margin-bottom: 24px;
    }

    .modes {
      display: grid; gap: 10px;
      grid-template-columns: 1fr;
    }
    @media (min-width: 560px) {
      .modes { grid-template-columns: repeat(3, 1fr); }
    }
    .mode-card {
      background: var(--ion-card-background);
      border: 2px solid transparent;
      border-radius: var(--laa-radius-lg);
      padding: 16px;
      cursor: pointer;
      transition: border-color 0.15s, transform 0.15s;
      display: flex; flex-direction: column; gap: 8px;
    }
    .mode-card:hover { transform: translateY(-2px); }
    .mode-card.selected { border-color: var(--ion-color-primary); }
    .mode-card .mode-icon {
      width: 36px; height: 36px;
      display: grid; place-items: center;
      border-radius: 10px;
      color: #fff;
    }
    .mode-card .mode-icon ion-icon { font-size: 18px; }
    .mode-card h4 { margin: 0; font-size: 0.95rem; font-weight: 600; }
    .mode-card p  { margin: 0; font-size: 0.78rem; color: var(--ion-color-step-500); }

    .kb-section { margin-top: 24px; }
    .kb-empty {
      background: var(--ion-card-background);
      border: 1px dashed rgba(var(--ion-color-primary-rgb), 0.25);
      border-radius: var(--laa-radius-md);
      padding: 16px;
      display: flex; align-items: center; justify-content: space-between; gap: 12px;
    }
    .kb-empty p {
      margin: 0; font-size: 0.85rem;
      color: var(--ion-color-step-500);
    }
    .kb-list {
      display: flex; flex-direction: column; gap: 6px;
    }
    .kb-row {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 12px;
      background: var(--ion-card-background);
      border: 2px solid transparent;
      border-radius: var(--laa-radius-md);
      cursor: pointer;
    }
    .kb-row.selected { border-color: var(--ion-color-primary); }
    .kb-row.none { font-style: italic; color: var(--ion-color-step-500); }
    .kb-row .check {
      width: 18px; height: 18px;
      border: 2px solid var(--ion-color-step-200);
      border-radius: 50%;
      display: grid; place-items: center;
      flex-shrink: 0;
    }
    .kb-row.selected .check {
      border-color: var(--ion-color-primary);
      background: var(--ion-color-primary);
      color: #fff;
    }
    .kb-row .check ion-icon { font-size: 12px; }
    .kb-row .kb-info { flex: 1; }
    .kb-row .kb-name { font-size: 0.9rem; font-weight: 600; }
    .kb-row .kb-meta { font-size: 0.72rem; color: var(--ion-color-step-500); }

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
          <ion-back-button defaultHref="/chats"></ion-back-button>
        </ion-buttons>
        <ion-title>Nouvelle conversation</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <div class="wrap">
        <h1>Lancer une discussion</h1>
        <p class="sub">Choisis un titre et un mode pour démarrer.</p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <p class="field-label">Titre</p>
          <ion-item class="title-input" lines="none">
            <ion-input
              placeholder="Ex. Questions sur la pharmacologie"
              formControlName="title"
              [clearInput]="true"></ion-input>
          </ion-item>

          <p class="field-label">Mode de réponse</p>
          <div class="modes">
            @for (m of modes; track m.value) {
              <div
                class="mode-card"
                [class.selected]="form.controls.mode.value === m.value"
                (click)="form.controls.mode.setValue(m.value)">
                <div class="mode-icon" [style.background]="m.color">
                  <ion-icon [name]="m.icon"></ion-icon>
                </div>
                <h4>{{ m.label }}</h4>
                <p>{{ m.description }}</p>
              </div>
            }
          </div>

          <div class="kb-section">
            <p class="field-label">Base documentaire (optionnel)</p>
            @if (loadingKbs()) {
              <ion-spinner name="dots"></ion-spinner>
            } @else if (kbs().length === 0) {
              <div class="kb-empty">
                <p>Aucune KB. Crées-en une et uploade des docs pour activer le RAG.</p>
                <ion-button fill="outline" size="small" routerLink="/kbs/new">
                  <ion-icon slot="start" name="add-outline"></ion-icon>
                  Créer une KB
                </ion-button>
              </div>
            } @else {
              <div class="kb-list">
                <div
                  class="kb-row none"
                  [class.selected]="selectedKbId() === null"
                  (click)="selectedKbId.set(null)">
                  <div class="check">
                    @if (selectedKbId() === null) {
                      <ion-icon name="library-outline"></ion-icon>
                    }
                  </div>
                  <div class="kb-info">
                    <div class="kb-name">Sans base documentaire</div>
                    <div class="kb-meta">L'assistant répondra sans RAG</div>
                  </div>
                </div>
                @for (k of kbs(); track k.id) {
                  <div
                    class="kb-row"
                    [class.selected]="selectedKbId() === k.id"
                    (click)="selectedKbId.set(k.id)">
                    <div class="check">
                      @if (selectedKbId() === k.id) {
                        <ion-icon name="library-outline"></ion-icon>
                      }
                    </div>
                    <div class="kb-info">
                      <div class="kb-name">{{ k.name }}</div>
                      <div class="kb-meta">{{ k.embeddingModel }} · {{ k.domain }}</div>
                    </div>
                  </div>
                }
              </div>
            }
          </div>

          <div class="actions">
            <ion-button fill="outline" type="button" (click)="cancel()">
              Annuler
            </ion-button>
            <ion-button type="submit" [disabled]="loading() || form.invalid">
              @if (loading()) {
                <ion-spinner name="dots"></ion-spinner>
              } @else {
                Démarrer
              }
            </ion-button>
          </div>
        </form>
      </div>
    </ion-content>
  `,
})
export class ChatCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly chats = inject(ChatService);
  private readonly kbService = inject(KbService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly kbs = computed(() => this.kbService.kbs());
  readonly loadingKbs = signal(true);
  readonly selectedKbId = signal<number | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      await this.kbService.refresh();
    } catch {
      /* on tolère que la liste soit vide */
    } finally {
      this.loadingKbs.set(false);
    }
  }

  readonly modes: ModeOption[] = [
    {
      value: 'NORMAL', label: 'Normal',
      description: 'Réponses concises et naturelles.',
      icon: 'sparkles-outline',
      color: 'linear-gradient(135deg, #6366f1, #818cf8)',
    },
    {
      value: 'EXPERT', label: 'Expert',
      description: 'Raisonnement étape par étape, sources.',
      icon: 'flash-outline',
      color: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
    },
    {
      value: 'FACT_CHECK', label: 'Fact-check',
      description: 'Annote les faits vérifiés / incertains.',
      icon: 'shield-checkmark-outline',
      color: 'linear-gradient(135deg, #10b981, #34d399)',
    },
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.maxLength(255)]],
    mode: ['NORMAL' as ChatMode, [Validators.required]],
  });

  readonly loading = signal(false);

  constructor() {
    addIcons({
      closeOutline, sparklesOutline, flashOutline, shieldCheckmarkOutline,
      libraryOutline, addOutline,
    });
  }

  async submit(): Promise<void> {
    if (this.loading()) return;
    this.loading.set(true);
    try {
      const v = this.form.getRawValue();
      const kbId = this.selectedKbId();
      const created = await this.chats.create({
        title: v.title?.trim() || undefined,
        mode: v.mode,
        ...(kbId !== null ? { knowledgeBaseId: kbId } : {}),
      });
      await this.router.navigate(['/chats', created.id]);
    } catch (err) {
      await this.toast.error(err, 'Création impossible.');
    } finally {
      this.loading.set(false);
    }
  }

  cancel(): void {
    this.router.navigate(['/chats']);
  }
}
