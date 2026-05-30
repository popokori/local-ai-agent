import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { applyBootEnvironment } from './app/core/ui/settings.service';

// Important : mute environment.apiUrl AVANT le bootstrap pour que tous les
// services HTTP voient la bonne valeur dès leur instanciation.
applyBootEnvironment();

// Applique le thème stocké AVANT le premier paint (évite le flash blanc en dark)
try {
  const stored = localStorage.getItem('laa.theme');
  const dark =
    stored === 'dark' ||
    (stored !== 'light' &&
      window.matchMedia?.('(prefers-color-scheme: dark)').matches) ||
    stored === null; // défaut = dark
  if (dark) document.body.classList.add('theme-dark');
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
} catch { /* ignore */ }

bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
