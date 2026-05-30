import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  // Pages d'auth — pas dans le shell
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.page').then(m => m.LoginPage),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.page').then(m => m.RegisterPage),
  },

  // App shell (sidebar + outlet) pour tout le reste, derrière authGuard
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/components/app-shell.component').then(m => m.AppShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'chats' },
      {
        path: 'chats',
        loadComponent: () =>
          import('./features/chat/chat-list.page').then(m => m.ChatListPage),
      },
      {
        path: 'chats/new',
        loadComponent: () =>
          import('./features/chat/chat-create.page').then(m => m.ChatCreatePage),
      },
      {
        path: 'chats/:id',
        loadComponent: () =>
          import('./features/chat/chat-detail.page').then(m => m.ChatDetailPage),
      },
      {
        path: 'kbs',
        loadComponent: () =>
          import('./features/kb/kb-list.page').then(m => m.KbListPage),
      },
      {
        path: 'kbs/new',
        loadComponent: () =>
          import('./features/kb/kb-create.page').then(m => m.KbCreatePage),
      },
      {
        path: 'kbs/:id',
        loadComponent: () =>
          import('./features/kb/kb-detail.page').then(m => m.KbDetailPage),
      },
      {
        path: 'memory',
        loadComponent: () =>
          import('./features/memory/memory.page').then(m => m.MemoryPage),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.page').then(m => m.SettingsPage),
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
