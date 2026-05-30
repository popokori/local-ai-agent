import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonSearchbar, IonRefresher, IonRefresherContent, IonFab, IonFabButton,
  IonMenuButton, IonSkeletonText,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  addOutline, chatbubblesOutline, trashOutline, sparklesOutline, searchOutline,
} from 'ionicons/icons';

import { ChatService } from './chat.service';
import { ChatSessionDto } from './chat.types';
import { RelativeDatePipe } from '../../shared/pipes/relative-date.pipe';
import { ToastService } from '../../core/ui/toast.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, RelativeDatePipe,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonSearchbar, IonRefresher, IonRefresherContent, IonFab, IonFabButton,
    IonMenuButton, IonSkeletonText,
  ],
  styles: [`
    :host { display: contents; }
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

    .search-wrap { padding: 0 12px; max-width: 920px; margin: 0 auto 8px; }
    ion-searchbar {
      --background: var(--ion-card-background);
      --border-radius: var(--laa-radius-md);
      --box-shadow: none;
      padding: 0; min-height: 48px;
    }

    .grid {
      padding: 12px 16px 96px;
      max-width: 920px; margin: 0 auto;
      display: grid; gap: 12px;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    }

    .session-card {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-lg);
      padding: 16px;
      cursor: pointer;
      transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s;
      display: flex; flex-direction: column; gap: 10px;
      min-height: 130px;
    }
    .session-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 24px -12px rgba(0,0,0,0.3);
      border-color: rgba(var(--ion-color-primary-rgb), 0.25);
    }
    .session-card .title-row {
      display: flex; align-items: flex-start; justify-content: space-between; gap: 8px;
    }
    .session-card h3 {
      font-size: 1rem; font-weight: 600; margin: 0;
      letter-spacing: -0.01em;
      line-height: 1.3;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .session-card .meta {
      display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
      font-size: 0.75rem; color: var(--ion-color-step-500);
      margin-top: auto;
    }
    .mode-chip {
      display: inline-flex; align-items: center;
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600; letter-spacing: 0.02em;
    }
    .mode-NORMAL     { background: rgba(99,102,241,0.16);  color: var(--laa-brand-soft); }
    .mode-EXPERT     { background: rgba(245,158,11,0.16);  color: #fbbf24; }
    .mode-FACT_CHECK { background: rgba(16,185,129,0.16);  color: #34d399; }

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
        <ion-title>Conversations</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <ion-refresher slot="fixed" (ionRefresh)="onRefresh($event)">
        <ion-refresher-content></ion-refresher-content>
      </ion-refresher>

      <div class="page-header">
        <h1>Bonjour {{ greetingName() }} 👋</h1>
        <p>Reprends une conversation ou démarre-en une nouvelle.</p>
      </div>

      <div class="search-wrap">
        <ion-searchbar
          [(ngModel)]="searchText"
          placeholder="Rechercher dans tes conversations…"></ion-searchbar>
      </div>

      @if (loading()) {
        <div class="grid">
          @for (i of [1,2,3,4,5,6]; track i) {
            <div class="session-card">
              <ion-skeleton-text animated style="height: 16px; width: 70%;"></ion-skeleton-text>
              <ion-skeleton-text animated style="height: 12px; width: 50%;"></ion-skeleton-text>
              <ion-skeleton-text animated style="height: 12px; width: 90%; margin-top: auto;"></ion-skeleton-text>
            </div>
          }
        </div>
      } @else if (filtered().length === 0 && !searchText) {
        <div class="empty">
          <div class="empty-icon">
            <ion-icon name="sparkles-outline"></ion-icon>
          </div>
          <h2>Aucune conversation</h2>
          <p>Crée ta première conversation pour discuter avec ton assistant local.</p>
          <ion-button routerLink="/chats/new">
            <ion-icon slot="start" name="add-outline"></ion-icon>
            Nouvelle conversation
          </ion-button>
        </div>
      } @else if (filtered().length === 0) {
        <div class="empty">
          <div class="empty-icon">
            <ion-icon name="search-outline"></ion-icon>
          </div>
          <h2>Aucun résultat</h2>
          <p>Aucune conversation ne correspond à « {{ searchText }} ».</p>
        </div>
      } @else {
        <div class="grid">
          @for (s of filtered(); track s.id) {
            <div class="session-card" (click)="open(s)">
              <div class="title-row">
                <h3>{{ s.title || 'Sans titre' }}</h3>
                <ion-button
                  class="delete-btn"
                  fill="clear"
                  size="small"
                  (click)="$event.stopPropagation(); confirmDelete(s)"
                  aria-label="Supprimer">
                  <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
                </ion-button>
              </div>
              <div class="meta">
                <span class="mode-chip" [class]="'mode-' + s.mode">{{ s.mode }}</span>
                @if (s.knowledgeBaseId !== null) {
                  <span>📚 KB #{{ s.knowledgeBaseId }}</span>
                }
                <span>{{ (s.lastMessageAt ?? s.createdAt) | relativeDate }}</span>
              </div>
            </div>
          }
        </div>
      }

      <ion-fab slot="fixed" vertical="bottom" horizontal="end">
        <ion-fab-button routerLink="/chats/new" aria-label="Nouvelle conversation">
          <ion-icon name="add-outline"></ion-icon>
        </ion-fab-button>
      </ion-fab>
    </ion-content>
  `,
})
export class ChatListPage implements OnInit {
  private readonly chats = inject(ChatService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  searchText = '';
  readonly loading = computed(() => this.chats.loading());

  readonly filtered = computed(() => {
    const q = this.searchText.trim().toLowerCase();
    if (!q) return this.chats.sessions();
    return this.chats.sessions().filter(s =>
      (s.title ?? '').toLowerCase().includes(q),
    );
  });

  readonly greetingName = computed(() => {
    const u = this.auth.user();
    if (!u) return '';
    return (u.displayName ?? u.username ?? '').split(' ')[0];
  });

  constructor() {
    addIcons({ addOutline, chatbubblesOutline, trashOutline, sparklesOutline, searchOutline });
  }

  async ngOnInit(): Promise<void> {
    try {
      await this.chats.refresh();
    } catch (err) {
      await this.toast.error(err, 'Impossible de charger les conversations.');
    }
  }

  async onRefresh(ev: Event): Promise<void> {
    try {
      await this.chats.refresh();
    } catch (err) {
      await this.toast.error(err);
    } finally {
      (ev.target as HTMLIonRefresherElement).complete();
    }
  }

  open(s: ChatSessionDto): void {
    this.router.navigate(['/chats', s.id]);
  }

  async confirmDelete(s: ChatSessionDto): Promise<void> {
    if (!confirm(`Supprimer la conversation "${s.title || 'Sans titre'}" ?`)) return;
    try {
      await this.chats.delete(s.id);
      await this.toast.success('Conversation supprimée');
    } catch (err) {
      await this.toast.error(err);
    }
  }
}
