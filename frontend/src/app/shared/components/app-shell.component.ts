import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import {
  IonSplitPane, IonMenu, IonContent,
  IonRouterOutlet, IonItem, IonLabel, IonIcon, IonButton,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  sparklesOutline, chatbubblesOutline, libraryOutline, bulbOutline,
  settingsOutline, logOutOutline, addOutline, personOutline,
} from 'ionicons/icons';

import { AuthService } from '../../core/auth/auth.service';

interface NavLink { label: string; icon: string; path: string; disabled?: boolean }

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule, RouterLink, RouterLinkActive,
    IonSplitPane, IonMenu, IonContent,
    IonRouterOutlet, IonItem, IonLabel, IonIcon, IonButton,
  ],
  styles: [`
    ion-menu {
      --width: 260px;
      --background: var(--ion-color-step-50);
    }
    .brand {
      display: flex; align-items: center; gap: 10px;
      padding: 18px 16px 10px;
    }
    .brand-mark {
      width: 34px; height: 34px;
      display: grid; place-items: center;
      border-radius: 10px;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-accent) 100%);
      color: #fff;
    }
    .brand-name { font-weight: 700; font-size: 16px; letter-spacing: -0.01em; }
    .brand-sub  { font-size: 11px; color: var(--ion-color-step-500); }

    .new-chat-wrap { padding: 8px 12px 12px; }
    .new-chat-wrap ion-button { --border-radius: var(--laa-radius-md); }

    .nav-list { padding: 4px 8px; }
    .nav-list ion-item {
      --background: transparent;
      --border-radius: var(--laa-radius-md);
      --padding-start: 12px;
      --inner-padding-end: 8px;
      --min-height: 44px;
      margin: 2px 0;
      border-radius: var(--laa-radius-md);
      cursor: pointer;
    }
    .nav-list ion-item.active {
      --background: rgba(var(--ion-color-primary-rgb), 0.12);
      --color: var(--ion-color-primary);
      font-weight: 600;
    }
    .nav-list ion-item ion-icon { font-size: 18px; }

    .footer {
      margin-top: auto;
      padding: 12px;
      border-top: 1px solid var(--ion-color-step-100);
    }
    .user-pill {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 10px;
      border-radius: var(--laa-radius-md);
      background: var(--ion-color-step-50);
    }
    .user-avatar {
      width: 32px; height: 32px;
      display: grid; place-items: center;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--laa-brand-soft), var(--laa-brand));
      color: #fff; font-weight: 700; font-size: 13px;
    }
    .user-meta { flex: 1; min-width: 0; }
    .user-name { font-weight: 600; font-size: 0.9rem; }
    .user-email {
      font-size: 0.75rem; color: var(--ion-color-step-500);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .menu-flex { display: flex; flex-direction: column; height: 100%; }
  `],
  template: `
    <ion-split-pane contentId="main-content" when="(min-width: 992px)">
      <ion-menu contentId="main-content" type="overlay" side="start">
        <ion-content [scrollY]="true">
          <div class="menu-flex">
            <div class="brand">
              <div class="brand-mark"><ion-icon name="sparkles-outline"></ion-icon></div>
              <div>
                <div class="brand-name">LocalAiAgent</div>
                <div class="brand-sub">Assistant local</div>
              </div>
            </div>

            <div class="new-chat-wrap">
              <ion-button expand="block" routerLink="/chats/new">
                <ion-icon slot="start" name="add-outline"></ion-icon>
                Nouvelle conversation
              </ion-button>
            </div>

            <div class="nav-list">
              @for (link of navLinks; track link.path) {
                <ion-item
                  button
                  detail="false"
                  [routerLink]="link.path"
                  routerLinkActive="active"
                  [routerLinkActiveOptions]="{exact: link.path === '/chats'}">
                  <ion-icon slot="start" [name]="link.icon" aria-hidden="true"></ion-icon>
                  <ion-label>{{ link.label }}</ion-label>
                </ion-item>
              }
            </div>

            <div class="footer">
              <div class="user-pill">
                <div class="user-avatar">{{ userInitials() }}</div>
                <div class="user-meta">
                  <div class="user-name">{{ auth.user()?.displayName ?? auth.user()?.username }}</div>
                  <div class="user-email">{{ auth.user()?.email }}</div>
                </div>
                <ion-button fill="clear" size="small" (click)="logout()" aria-label="Déconnexion">
                  <ion-icon name="log-out-outline" slot="icon-only"></ion-icon>
                </ion-button>
              </div>
            </div>
          </div>
        </ion-content>
      </ion-menu>

      <ion-router-outlet id="main-content"></ion-router-outlet>
    </ion-split-pane>
  `,
})
export class AppShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly navLinks: NavLink[] = [
    { label: 'Conversations', icon: 'chatbubbles-outline', path: '/chats' },
    { label: 'Bases documentaires', icon: 'library-outline', path: '/kbs' },
    { label: 'Mémoire',     icon: 'bulb-outline',    path: '/memory' },
    { label: 'Réglages',    icon: 'settings-outline', path: '/settings' },
  ];

  readonly userInitials = computed(() => {
    const u = this.auth.user();
    if (!u) return '?';
    const src = (u.displayName ?? u.username).trim();
    if (!src) return '?';
    const parts = src.split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return src.slice(0, 2).toUpperCase();
  });

  constructor() {
    addIcons({
      sparklesOutline, chatbubblesOutline, libraryOutline, bulbOutline,
      settingsOutline, logOutOutline, addOutline, personOutline,
    });
  }

  async logout(): Promise<void> {
    await this.auth.logout();
  }
}
