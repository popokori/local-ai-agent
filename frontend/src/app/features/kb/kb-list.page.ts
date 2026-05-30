import { Component, OnInit, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonFab, IonFabButton, IonMenuButton, IonSkeletonText,
  IonRefresher, IonRefresherContent,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  addOutline, libraryOutline, trashOutline, documentTextOutline,
} from 'ionicons/icons';

import { KbService } from './kb.service';
import { KnowledgeBaseDto } from './kb.types';
import { RelativeDatePipe } from '../../shared/pipes/relative-date.pipe';
import { ToastService } from '../../core/ui/toast.service';

@Component({
  selector: 'app-kb-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, RelativeDatePipe,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonFab, IonFabButton, IonMenuButton, IonSkeletonText,
    IonRefresher, IonRefresherContent,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }
    .page-header {
      padding: 24px 16px 8px;
      max-width: 920px; margin: 0 auto;
    }
    .page-header h1 {
      font-size: 1.8rem; font-weight: 700; letter-spacing: -0.02em;
      margin: 0 0 4px;
    }
    .page-header p { color: var(--ion-color-step-500); margin: 0; }

    .grid {
      padding: 12px 16px 96px;
      max-width: 920px; margin: 0 auto;
      display: grid; gap: 12px;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    }

    .kb-card {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-lg);
      padding: 16px;
      cursor: pointer;
      transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
      display: flex; flex-direction: column; gap: 10px;
      min-height: 150px;
    }
    .kb-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 24px -12px rgba(0,0,0,0.3);
      border-color: rgba(var(--ion-color-primary-rgb), 0.25);
    }
    .kb-card .title-row {
      display: flex; align-items: flex-start; justify-content: space-between; gap: 8px;
    }
    .kb-card .icon-wrap {
      width: 38px; height: 38px;
      display: grid; place-items: center;
      border-radius: 10px;
      color: #fff;
      flex-shrink: 0;
    }
    .kb-card .icon-wrap ion-icon { font-size: 20px; }
    .kb-card h3 {
      font-size: 1rem; font-weight: 600; margin: 0 0 4px;
      letter-spacing: -0.01em; line-height: 1.3;
    }
    .kb-card .desc {
      color: var(--ion-color-step-500);
      font-size: 0.85rem;
      line-height: 1.4;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .kb-card .meta {
      display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
      font-size: 0.75rem; color: var(--ion-color-step-500);
      margin-top: auto;
    }
    .domain-chip {
      display: inline-flex; align-items: center;
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600; letter-spacing: 0.02em;
    }
    .delete-btn {
      --color: var(--ion-color-step-500);
      --background-hover: rgba(239,68,68,0.1);
      --color-hover: var(--ion-color-danger);
      margin: -4px -4px 0 0;
    }

    .empty {
      max-width: 480px; margin: 80px auto;
      text-align: center; padding: 16px;
    }
    .empty .empty-icon {
      width: 72px; height: 72px;
      display: grid; place-items: center;
      border-radius: 24px;
      margin: 0 auto 16px;
      background: linear-gradient(135deg, rgba(99,102,241,0.18), rgba(20,184,166,0.18));
      color: var(--ion-color-primary);
    }
    .empty .empty-icon ion-icon { font-size: 36px; }
    .empty h2 { font-size: 1.3rem; font-weight: 700; margin: 0 0 6px; letter-spacing: -0.01em; }
    .empty p { color: var(--ion-color-step-500); margin: 0 0 20px; }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>Bases documentaires</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <ion-refresher slot="fixed" (ionRefresh)="onRefresh($event)">
        <ion-refresher-content></ion-refresher-content>
      </ion-refresher>

      <div class="page-header">
        <h1>Tes bases documentaires</h1>
        <p>Indexe des PDF, DOCX ou TXT pour que l'assistant puisse les citer.</p>
      </div>

      @if (loading()) {
        <div class="grid">
          @for (i of [1,2,3,4]; track i) {
            <div class="kb-card">
              <ion-skeleton-text animated style="height: 16px; width: 60%;"></ion-skeleton-text>
              <ion-skeleton-text animated style="height: 12px; width: 90%;"></ion-skeleton-text>
              <ion-skeleton-text animated style="height: 12px; width: 50%; margin-top: auto;"></ion-skeleton-text>
            </div>
          }
        </div>
      } @else if (kbs().length === 0) {
        <div class="empty">
          <div class="empty-icon">
            <ion-icon name="library-outline"></ion-icon>
          </div>
          <h2>Aucune base documentaire</h2>
          <p>Crée une KB et uploade des PDF pour activer la recherche documentaire (RAG).</p>
          <ion-button routerLink="/kbs/new">
            <ion-icon slot="start" name="add-outline"></ion-icon>
            Créer ma première KB
          </ion-button>
        </div>
      } @else {
        <div class="grid">
          @for (k of kbs(); track k.id) {
            <div class="kb-card" (click)="open(k)">
              <div class="title-row">
                <div class="icon-wrap" [style.background]="domainGradient(k.domain)">
                  <ion-icon [name]="domainIcon(k.domain)"></ion-icon>
                </div>
                <ion-button
                  class="delete-btn"
                  fill="clear"
                  size="small"
                  (click)="$event.stopPropagation(); confirmDelete(k)"
                  aria-label="Supprimer">
                  <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
                </ion-button>
              </div>
              <div>
                <h3>{{ k.name }}</h3>
                @if (k.description) {
                  <div class="desc">{{ k.description }}</div>
                }
              </div>
              <div class="meta">
                <span class="domain-chip" [style.background]="domainTint(k.domain)" [style.color]="domainColor(k.domain)">
                  {{ domainLabel(k.domain) }}
                </span>
                <span>{{ k.embeddingModel }}</span>
                <span>· {{ k.createdAt | relativeDate }}</span>
              </div>
            </div>
          }
        </div>
      }

      <ion-fab slot="fixed" vertical="bottom" horizontal="end">
        <ion-fab-button routerLink="/kbs/new" aria-label="Nouvelle KB">
          <ion-icon name="add-outline"></ion-icon>
        </ion-fab-button>
      </ion-fab>
    </ion-content>
  `,
})
export class KbListPage implements OnInit {
  private readonly kbService = inject(KbService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly kbs = computed(() => this.kbService.kbs());
  readonly loading = computed(() => this.kbService.loading());

  constructor() {
    addIcons({ addOutline, libraryOutline, trashOutline, documentTextOutline });
  }

  async ngOnInit(): Promise<void> {
    try {
      await this.kbService.refresh();
    } catch (err) {
      await this.toast.error(err, 'Impossible de charger les KBs.');
    }
  }

  async onRefresh(ev: Event): Promise<void> {
    try {
      await this.kbService.refresh();
    } catch (err) {
      await this.toast.error(err);
    } finally {
      (ev.target as HTMLIonRefresherElement).complete();
    }
  }

  open(k: KnowledgeBaseDto): void {
    this.router.navigate(['/kbs', k.id]);
  }

  async confirmDelete(k: KnowledgeBaseDto): Promise<void> {
    if (!confirm(`Supprimer la KB "${k.name}" et tous ses documents ?`)) return;
    try {
      await this.kbService.delete(k.id);
      await this.toast.success('KB supprimée');
    } catch (err) {
      await this.toast.error(err);
    }
  }

  domainLabel(d: string): string {
    return {
      GENERIC: 'Général',
      SCIENCE: 'Science',
      BIOLOGY: 'Biologie',
      CHEMISTRY: 'Chimie',
      MATHEMATICS: 'Maths',
      MEDICAL: 'Médical',
      COMPUTER_SCIENCE: 'Informatique',
    }[d] ?? d;
  }
  domainIcon(d: string): string {
    return 'document-text-outline';
  }
  domainGradient(d: string): string {
    return ({
      GENERIC:           'linear-gradient(135deg, #6366f1, #818cf8)',
      SCIENCE:           'linear-gradient(135deg, #6366f1, #14b8a6)',
      BIOLOGY:           'linear-gradient(135deg, #10b981, #34d399)',
      CHEMISTRY:         'linear-gradient(135deg, #f59e0b, #fbbf24)',
      MATHEMATICS:       'linear-gradient(135deg, #8b5cf6, #a78bfa)',
      MEDICAL:           'linear-gradient(135deg, #ef4444, #f87171)',
      COMPUTER_SCIENCE:  'linear-gradient(135deg, #06b6d4, #67e8f9)',
    } as Record<string, string>)[d] ?? 'linear-gradient(135deg, #6366f1, #818cf8)';
  }
  domainTint(d: string): string {
    return ({
      GENERIC:           'rgba(99,102,241,0.16)',
      SCIENCE:           'rgba(99,102,241,0.16)',
      BIOLOGY:           'rgba(16,185,129,0.16)',
      CHEMISTRY:         'rgba(245,158,11,0.16)',
      MATHEMATICS:       'rgba(139,92,246,0.16)',
      MEDICAL:           'rgba(239,68,68,0.16)',
      COMPUTER_SCIENCE:  'rgba(6,182,212,0.16)',
    } as Record<string, string>)[d] ?? 'rgba(99,102,241,0.16)';
  }
  domainColor(d: string): string {
    return ({
      GENERIC:           '#818cf8',
      SCIENCE:           '#818cf8',
      BIOLOGY:           '#34d399',
      CHEMISTRY:         '#fbbf24',
      MATHEMATICS:       '#a78bfa',
      MEDICAL:           '#f87171',
      COMPUTER_SCIENCE:  '#67e8f9',
    } as Record<string, string>)[d] ?? '#818cf8';
  }
}
