# Plan Frontend — Ionic + Angular (web + mobile)

Cible : une seule base de code Ionic + Angular qui se déploie en **web** (PWA)
et en **mobile native** (Android + iOS via Capacitor).

> Backend prêt à consommer : http://localhost:8081, voir
> `PROJECT_STATE.md §7` pour la liste complète des endpoints REST + SSE.

---

## 1. Stack et choix techniques

| Composant | Choix | Pourquoi |
|---|---|---|
| Framework UI | **Ionic 8** | Composants natifs-look (Material + Cupertino auto), thèmes dark/light intégrés, écosystème Angular mature |
| Framework Angular | **Angular 18** (standalone components) | Plus simple que NgModules, signals stables, support officiel Ionic 8 |
| Mobile bridge | **Capacitor 6** | Bridge natif iOS/Android propre, plugins maintenus, fonctionne aussi en PWA pure |
| TypeScript | **strict mode** | Catch bugs tôt, IDE fluide |
| State management | **Angular Signals** + `signal()`/`computed()`/`effect()` + RxJS pour les flux | Pas besoin de NgRx (single-user, état raisonnable). Signals = perf + simplicité. |
| HTTP | `HttpClient` + interceptors (JWT auth + refresh) | Standard Angular |
| Streaming SSE | `fetch()` + `ReadableStream.getReader()` (parser manuel) | **PAS `EventSource`** : ne supporte pas les headers `Authorization`, fragile sur Capacitor WebView |
| Markdown rendering | **`marked` + DOMPurify** (sanitization stricte) | Réponses LLM contiennent du markdown ; sécurité XSS impérative |
| Form validation | Angular `ReactiveFormsModule` | Standard |
| Icons | `ionicons` (inclus) | Cohérence Ionic |
| HTTP timeouts | 5 min sur SSE, 30 s sur REST | Adapté aux LLM CPU lents |
| Storage tokens | **Capacitor Preferences** sur mobile, `localStorage` chiffré minimal sur web | Plus sûr que `localStorage` plain sur mobile |
| Internationalisation | **différée Phase F8** (FR par défaut) | Pas critique pour MVP |
| Tests | Karma + Jasmine (Angular défaut) + Cypress E2E plus tard | Standard |
| Build web | `ionic build --prod` → `www/` static | Servable par n'importe quel reverse proxy |
| Build Android | `ionic cap add android` + Android Studio | APK signé pour Play Store ou sideload |
| Build iOS | `ionic cap add ios` + Xcode (Mac requis) | TestFlight ou App Store |

### Pourquoi NE PAS utiliser ces alternatives

| Alternative | Raison du rejet |
|---|---|
| React Native | Une autre stack à apprendre, écosystème Ionic + Capacitor déjà cohérent |
| Flutter | Pas d'Angular, deux fois plus de code à maintenir |
| Pur Angular (sans Ionic) | Devrait réinventer le composant mobile-friendly (drawer, ion-content scroll, etc.) |
| Vercel AI SDK `useChat` | Génial mais React-only |
| `EventSource` | Pas de header auth → JWT impossible ; pas fiable sur WebView mobile |
| NgRx / NgRx Signals Store | Sur-engineering pour un mono-utilisateur ; Signals + services suffisent |

---

## 2. Architecture globale

```
┌───────────────────────────────────────────────────────────────┐
│  Pages (routed standalone components)                         │
│   /login  /register  /chats  /chats/:id  /kbs  /kbs/:id       │
│   /memory  /settings                                          │
└─────────────────────────┬─────────────────────────────────────┘
                          │ Signals binding
┌─────────────────────────▼─────────────────────────────────────┐
│  Stateful services (Signals exposés)                          │
│   AuthService      ChatService    MessageService              │
│   KbService        DocumentService MemoryService              │
│   ThemeService     SseStreamService                           │
└─────────────────────────┬─────────────────────────────────────┘
                          │ HttpClient + interceptors
┌─────────────────────────▼─────────────────────────────────────┐
│  Infrastructure                                               │
│   ApiClient (base URL)                                        │
│   AuthInterceptor (Bearer + 401 → refresh + retry)            │
│   TokenStorageService (Capacitor Preferences / localStorage)  │
│   ErrorHandlerService (toast utilisateur)                     │
└─────────────────────────┬─────────────────────────────────────┘
                          │
                          ▼
                Backend Spring Boot :8081
                (REST + SSE)
```

**Principes :**
- Aucun appel HTTP direct depuis un composant — toujours via un service.
- Les pages lisent des signals exposés par les services (`auth.user()`, `chats.list()`).
- Pour le streaming SSE : `MessageService.sendStream(...)` retourne un Observable d'événements typés.
- Les services côté domain mappent les DTOs backend en types front clairs (pas de "fuite" du JSON brut dans les composants).

---

## 3. Structure du projet

```
frontend/
├── ionic.config.json
├── angular.json
├── capacitor.config.ts
├── package.json
├── tsconfig.json
├── tsconfig.strict.json
├── src/
│   ├── main.ts
│   ├── index.html
│   ├── global.scss
│   ├── environments/
│   │   ├── environment.ts            # local : http://localhost:8081
│   │   └── environment.prod.ts       # prod URL backend
│   ├── theme/
│   │   └── variables.scss            # palette Ionic
│   ├── assets/
│   │   ├── icon/                     # logos, favicons
│   │   └── splash/                   # écrans de démarrage Capacitor
│   ├── app/
│   │   ├── app.component.ts          # shell racine (router-outlet + side menu sur web)
│   │   ├── app.routes.ts             # définition routes lazy-loaded
│   │   ├── core/
│   │   │   ├── api/
│   │   │   │   ├── api-client.ts                  # wrapper HttpClient + base URL
│   │   │   │   └── sse-stream.service.ts          # parser fetch streaming → Observable
│   │   │   ├── auth/
│   │   │   │   ├── auth.service.ts                # login/register/logout, signal user
│   │   │   │   ├── token-storage.service.ts       # Capacitor Preferences
│   │   │   │   ├── auth.interceptor.ts            # Bearer + 401 refresh + retry
│   │   │   │   └── auth.guard.ts                  # canActivate
│   │   │   ├── theme/
│   │   │   │   └── theme.service.ts               # dark/light persisté
│   │   │   └── errors/
│   │   │       └── error-handler.service.ts       # toast Ionic
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── message-bubble/                # USER / ASSISTANT + markdown
│   │   │   │   ├── source-pill/                   # badge "[source N]" cliquable
│   │   │   │   ├── tool-call-accordion/           # iter, name, args, summary
│   │   │   │   ├── streaming-cursor/              # "..." pendant le stream
│   │   │   │   └── empty-state/                   # placeholder visuel
│   │   │   ├── directives/
│   │   │   └── pipes/
│   │   │       ├── markdown.pipe.ts               # marked + DOMPurify
│   │   │       └── relative-date.pipe.ts          # "il y a 2 min"
│   │   └── features/
│   │       ├── auth/
│   │       │   ├── login.page.ts
│   │       │   └── register.page.ts
│   │       ├── chat/
│   │       │   ├── chat-list.page.ts              # sidebar web / page mobile
│   │       │   ├── chat-detail.page.ts            # vue principale + stream
│   │       │   ├── chat-create.page.ts            # form mode/KB/modèle
│   │       │   ├── chat.service.ts                # sessions CRUD signal
│   │       │   ├── message.service.ts             # history + sendStream
│   │       │   └── chat.types.ts                  # ChatSession, Message, etc.
│   │       ├── kb/
│   │       │   ├── kb-list.page.ts
│   │       │   ├── kb-detail.page.ts              # liste docs + zone upload
│   │       │   ├── document-upload.component.ts   # drag-drop + progress
│   │       │   ├── kb.service.ts
│   │       │   ├── document.service.ts
│   │       │   └── kb.types.ts
│   │       ├── memory/
│   │       │   ├── memory.page.ts                 # tabs Facts / Entries
│   │       │   ├── fact-card.component.ts
│   │       │   ├── entry-card.component.ts
│   │       │   ├── memory.service.ts
│   │       │   └── memory.types.ts
│   │       └── settings/
│   │           ├── settings.page.ts
│   │           └── settings.service.ts
│   └── polyfills.ts
├── android/                          # généré par `ionic cap add android`
└── ios/                              # généré par `ionic cap add ios` (sur Mac)
```

**Tout en standalone components** : pas de `NgModule` à part le bootstrap.

---

## 4. Routes et navigation

| Path | Composant | Auth | Description |
|---|---|---|---|
| `/login` | `LoginPage` | public | Form username + password |
| `/register` | `RegisterPage` | public | Form complet inscription |
| `/chats` | `ChatListPage` | guard | Liste paginée sessions (web : sidebar; mobile : page) |
| `/chats/new` | `ChatCreatePage` | guard | Form nouvelle session : titre, mode, KB, modèle |
| `/chats/:id` | `ChatDetailPage` | guard | **Page principale** : historique + input + stream |
| `/kbs` | `KbListPage` | guard | Liste KBs cards |
| `/kbs/new` | `KbCreatePage` | guard | Form (nom, description, domaine) |
| `/kbs/:id` | `KbDetailPage` | guard | Docs uploadés + zone upload + ingestion progress |
| `/memory` | `MemoryPage` | guard | Tabs : "Profil" + "Souvenirs" |
| `/settings` | `SettingsPage` | guard | Thème, modèle par défaut, langue, profil |
| `/**` | redirect `/chats` ou `/login` | — | Selon auth state |

Layout :
- **Web** (`>=768px`) : split-pane Ionic = sidebar fixe (liste sessions, nav) + content détail.
- **Mobile** : navigation par stack, side menu via burger button, tabs en bas pour les sections principales (Chats, KBs, Memory, Settings).

---

## 5. Services — responsabilités détaillées

### AuthService (`core/auth/auth.service.ts`)

```ts
class AuthService {
  user = signal<UserDto | null>(null);   // utilisateur courant
  isLoggedIn = computed(() => this.user() !== null);

  async login(username: string, password: string): Promise<void>;
  async register(payload: RegisterRequest): Promise<void>;
  async refresh(): Promise<void>;       // utilisé par l'interceptor
  async logout(): Promise<void>;        // appel backend + purge tokens locaux
  async loadProfile(): Promise<void>;   // GET /users/me, peuple user()
}
```

Au bootstrap (`APP_INITIALIZER`), si un access token est en storage, tenter
`loadProfile()` ; si 401 → `refresh()` ; si échec → redirect `/login`.

### TokenStorageService

Abstraction storage :
- **Mobile** : `@capacitor/preferences` (chiffré sur iOS Keychain / Android KeyStore via plugin Secure Storage si dispo)
- **Web** : `localStorage` (les tokens JWT sont par nature révocables côté serveur)

API : `getAccessToken / setAccessToken / getRefreshToken / setRefreshToken / clear`.

### AuthInterceptor

- Ajoute `Authorization: Bearer ${access}` sur toutes les requêtes vers le backend.
- Sur 401 : tente `auth.refresh()` une fois, replay la requête initiale, sinon redirect `/login`.
- Concurrency : si plusieurs requêtes 401 simultanées, mutex pour ne refresh qu'une seule fois (RxJS `BehaviorSubject<boolean>` "refreshing").

### SseStreamService (`core/api/sse-stream.service.ts`)

Cœur du chat. **N'utilise pas `EventSource`** (cf. choix techniques).

```ts
interface SseEvent {
  event: 'source' | 'tool_start' | 'tool_end' | 'token' | 'final' | 'error';
  data: any;
}

class SseStreamService {
  stream(url: string, body: any, headers: HeadersInit): Observable<SseEvent>;
}
```

Implémentation :
```ts
stream(url, body, headers): Observable<SseEvent> {
  return new Observable(subscriber => {
    const ctrl = new AbortController();
    fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8', ...headers },
      body: JSON.stringify(body),
      signal: ctrl.signal,
    }).then(async res => {
      if (!res.ok) { subscriber.error(...); return; }
      const reader = res.body!.getReader();
      const dec = new TextDecoder();
      let buf = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buf += dec.decode(value, { stream: true });
        let idx;
        while ((idx = buf.indexOf('\n\n')) >= 0) {
          const block = buf.slice(0, idx);
          buf = buf.slice(idx + 2);
          const parsed = parseSseBlock(block);
          if (parsed) subscriber.next(parsed);
        }
      }
      subscriber.complete();
    }).catch(err => subscriber.error(err));
    return () => ctrl.abort();
  });
}

function parseSseBlock(block: string): SseEvent | null {
  let event = 'message'; let data = '';
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) data += line.slice(5).trim();
  }
  try { return { event: event as any, data: JSON.parse(data) }; }
  catch { return null; }
}
```

### MessageService

```ts
class MessageService {
  history(sessionId: number, page = 0): Promise<PageResponse<MessageDto>>;

  // Stream un message — retourne tous les événements typés
  sendStream(sessionId: number, content: string): Observable<SseEvent>;
}
```

Le `ChatDetailPage` consomme cet Observable et met à jour un signal
`messages = signal<UiMessage[]>([])` où `UiMessage` agrège tokens + sources +
tool calls reçus.

### ChatService

Liste, crée, met à jour, supprime des sessions. Expose `chats = signal<ChatSessionDto[]>([])`.

### KbService + DocumentService

CRUD KBs, upload (multipart FormData), poll status (`GET /documents/{id}` toutes les 2 s tant que status != INDEXED | FAILED).

### MemoryService

CRUD facts + entries. Signal `facts$` + `entries$` pour réactivité.

### ThemeService

Toggle light/dark, persiste dans `localStorage`, applique via `document.body.classList`.

---

## 6. Composants UI clés

### MessageBubble

Props : `message: UiMessage`.

Rendu :
- Bulle alignée gauche (assistant) ou droite (user)
- Markdown via pipe `markdown` (sanitized)
- Si `sources?.length > 0` : ligne de `<source-pill>` cliquables
- Si `toolCalls?.length > 0` : `<tool-call-accordion>` collapsable
- Si message en cours de stream : indicateur `<streaming-cursor>` (petit `▍` qui clignote)

### SourcePill

Tap → ouvre un modal Ionic avec :
- Nom du document
- Numéro de page si dispo
- Snippet (déjà reçu dans `event:source`)
- Score (debug only en dev)
- Lien "ouvrir le document" (Phase F4+)

### ToolCallAccordion

Liste les `event:tool_start`/`tool_end` reçus pendant la génération :
```
🔧 1. calculator          ✓ "17*23 + sqrt(144) = 403"
🔧 2. web_search          ✓ "5 result(s) for 'inventeur du WWW'"
🔧 3. web_fetch           ✓ "Fetched https://en.wikipedia.org/... (47431 bytes)"
```

Click sur une entrée → expand les `arguments` et `summary` complets.

### ChatInputArea

- Textarea auto-resize (max 6 lignes)
- Bouton "envoyer" (disabled si message vide ou stream en cours)
- Si KB attachée : badge "📚 KB: <nom>" cliquable pour détacher
- Mode picker (NORMAL / EXPERT / FACT_CHECK) en menu compact si on veut changer pour ce message

### DocumentUpload

- Zone drag-drop (web) + bouton "choisir fichier" (mobile via Capacitor Filesystem ou input file)
- Liste les fichiers en upload avec progress bar par fichier
- Une fois INDEXED → check vert + nombre de chunks

### StreamingCursor

```scss
.cursor {
  display: inline-block;
  width: 6px; height: 1em;
  background: var(--ion-color-medium);
  animation: blink 1s steps(2) infinite;
}
@keyframes blink { 50% { opacity: 0; } }
```

---

## 7. Mapping événements SSE → UI

Le `ChatDetailPage` maintient un signal `currentAssistant: UiMessage | null` :

| Événement SSE | Action UI |
|---|---|
| `event:source` | `currentAssistant.sources.push(parsed)` |
| `event:tool_start` | `currentAssistant.toolCalls.push({ iter, name, args, status: 'running' })` |
| `event:tool_end` | Met à jour le tool_call correspondant (`status`, `success`, `summary`) |
| `event:token` | `currentAssistant.content += data.text` ; auto-scroll bottom |
| `event:final` | `currentAssistant.id = data.messageId` ; persiste dans `messages` ; reset cursor |
| `event:error` | Toast erreur + suppression du `currentAssistant` ou état "failed" |

Auto-scroll : `IntersectionObserver` sur le footer pour ne pas forcer le scroll si l'user est en train de lire en haut.

---

## 8. Capacitor — configuration mobile

### `capacitor.config.ts`

```ts
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'mr.popo.localaiagent',
  appName: 'LocalAiAgent',
  webDir: 'www',
  server: {
    // En dev sur device physique, pointer vers IP locale du PC qui sert le backend
    // url: 'http://192.168.1.42:8081',
    // En prod : embed les assets et appeler l'IP/URL configurée à l'init
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: { launchAutoHide: false, backgroundColor: '#0b1220' },
    StatusBar: { style: 'DARK' },
    Keyboard: { resize: 'native' },
  },
};

export default config;
```

### Plugins Capacitor utilisés

| Plugin | Usage |
|---|---|
| `@capacitor/preferences` | Stockage tokens JWT |
| `@capacitor/network` | Détection offline → toast |
| `@capacitor/status-bar` | Style cohérent avec dark mode |
| `@capacitor/keyboard` | Resize content quand clavier ouvre |
| `@capacitor/filesystem` | Lire un fichier local pour upload (mobile) |
| `@capacitor/toast` | Notifications courtes |
| `@capacitor/splash-screen` | Splash custom |
| `@capacitor/preferences-secure` (optionnel) | Tokens en Keychain/KeyStore (Phase F7 polish) |

### Mobile vs Web — divergences à gérer

| Aspect | Web | Mobile |
|---|---|---|
| Storage | `localStorage` | Capacitor Preferences (chiffrement OS-level si Secure variant) |
| File upload | `<input type="file">` | Capacitor Filesystem + Picker |
| Backend URL | `environment.apiUrl` | Idem mais résolu à l'IP locale du LAN si dev |
| HTTPS | Recommandé en prod | **Obligatoire** en prod (sinon mixed content + permissions plugin) |
| Notifications | Browser Notification API | Capacitor Push Notifications (Phase 8+) |
| App icon / splash | favicon | `npx @capacitor/assets generate` |

### URL backend en mobile

Sur un device physique en dev, `localhost` désigne le téléphone, pas le PC.
Solutions :
1. Variable env `API_URL=http://<IP_DU_PC>:8081` au build mobile
2. Page Settings : laisser l'user configurer l'URL backend (utile pour multi-device)

Recommandation MVP : la 2e, exposer un champ "Backend URL" dans Settings.

---

## 9. Sécurité frontend

| Préoccupation | Mitigation |
|---|---|
| XSS dans le markdown LLM | `DOMPurify` sur la sortie de `marked` (whitelist tags : `p, br, strong, em, code, pre, ul, ol, li, h1-h4, a[href]`) |
| Tokens dans `localStorage` | Acceptable pour single-user. Pour multi-tenant : `HttpOnly` cookie + CSRF — refonte backend |
| Bearer dans logs réseau | Désactiver `console.log` des requêtes en prod via interceptor conditional |
| Replay refresh token | Backend déjà fait la rotation + révocation. Front : ne stocker QUE la dernière valeur. |
| CORS | Backend `app.cors.allowed-origins` doit lister explicitement le domaine de la PWA en prod |
| URL backend exposée | Pas un secret (besoin de l'appeler), mais paramétrable par utilisateur en mobile |
| Permissions Capacitor | Demander uniquement ce qui est utilisé (Filesystem pour upload) ; pas de Camera/Mic Phase F1-F6 |

---

## 10. Roadmap par sprints

Chaque sprint = livraison testable avec critère de sortie. Estimations à temps plein solo.

| Sprint | Périmètre | Critère de sortie | Durée |
|---|---|---|---|
| **F1 — Bootstrap + Auth** | `ionic start` Angular standalone tabs, theme dark/light, AuthService + interceptor + guard, LoginPage + RegisterPage | `npm start` ouvre login → register → login → redirect /chats placeholder. Token refresh en arrière-plan fonctionne. | 3 j |
| **F2 — Chat list + détail (sans stream)** | ChatService + MessageService.history(), ChatListPage, ChatDetailPage avec historique paginé, ChatCreatePage avec mode/KB picker | Création session → envoi message via simple POST sans stream (réponse complète à la fin) → historique persisté. | 3 j |
| **F3 — Streaming SSE + UI** | SseStreamService, MessageService.sendStream(), MessageBubble + StreamingCursor + auto-scroll, mapping événements | Token-par-token visible en live. `event:source` rendu en SourcePill. ToolCallAccordion fonctionnel pour `tool_start/tool_end`. | 4 j |
| **F4 — Knowledge Bases + upload** | KbService, KbListPage, KbDetailPage, DocumentService, DocumentUpload (drag-drop + progress + poll status) | Upload PDF → barre de progression → ingestion poll → INDEXED → on attache la KB à une session → RAG marche. | 4 j |
| **F5 — Memory UI** | MemoryService + MemoryPage avec tabs Facts/Entries, édition inline, suppression | Voir/éditer/supprimer ses faits ; voir ses souvenirs. | 2 j |
| **F6 — Settings + polish** | Dark mode toggle persisté, modèle picker, URL backend configurable, profil édition | UX cohérente, dark/light propre, scroll smooth, gestion erreur réseau. | 3 j |
| **F7 — Mobile Capacitor (Android)** | `cap add android`, icons + splash, build APK debug, fix layouts, plugins Preferences/StatusBar/Keyboard/Filesystem | APK sideload sur device → flow complet (login → chat stream → upload). | 4 j |
| **F8 — Mobile iOS + PWA prod + signing** | `cap add ios` (Mac requis), service worker PWA, manifest, signing release Android, TestFlight iOS | App installable depuis navigateur (PWA), APK signé pour Play Store, build iOS dans Xcode | 5 j |

**Total ≈ 4 semaines** à temps plein solo. F1-F6 = web complet ; F7-F8 = mobile + release.

---

## 11. Critères de succès

À la fin de F6 (web) :
- Login → register → chat avec stream visible token-par-token
- Upload d'un PDF → ingestion en arrière-plan visible → question → réponse avec sources cliquables
- Mode EXPERT testable, mémoire visible/éditable, profil utilisateur affiché
- Dark mode propre, responsive (testé Chrome desktop 1920×1080 + iPhone DevTools 390×844)

À la fin de F8 (mobile) :
- APK debug installable sur Android → flow complet
- Build iOS lance dans simulateur Xcode → flow complet
- PWA installable depuis Chrome desktop (icon + splash)

---

## 12. Risques et mitigations

| Risque | Mitigation |
|---|---|
| `fetch` streaming + CORS preflight bloque | Backend `CorsConfig.allowedHeaders` inclut `Authorization, Content-Type, Accept` (déjà ok) ; `allowCredentials=true` si cookies |
| Capacitor WebView ne supporte pas `ReadableStream` proprement sur vieux Android | Test cible Android 7+ (API 24). Fallback : polling court (toutes les 500 ms `GET` partiel) si stream impossible |
| Latence LLM CPU rend l'UX "morte" longtemps avant le 1er token | Spinner pendant `event:tool_start` ou pendant pré-RAG ; afficher "Réflexion en cours..." si rien pendant > 2 s |
| iOS exige HTTPS strict en prod | Mettre un reverse proxy (Caddy ou Cloudflare Tunnel) devant le backend |
| Markdown LLM contient des liens malveillants | `DOMPurify` + ouverture liens externes via `Browser.open()` Capacitor (pas dans la WebView) |
| URL backend changeable casse les sessions | Sur changement → purge tokens + redirect /login |
| Plugin Capacitor incompatible avec Angular 18 | Vérifier au bootstrap F7 ; si bloquant, downgrader Angular d'une mineure |

---

## 13. Commandes utiles

### Création initiale (F1, à faire 1 fois)

```bash
npm install -g @ionic/cli@latest
cd C:\Users\Administrateur\Pictures\llmproject
ionic start frontend tabs --type=angular --standalone --capacitor
cd frontend
npm install marked dompurify
npm install --save-dev @types/dompurify
ionic capacitor add android
# ionic capacitor add ios     # sur Mac uniquement
```

### Dev quotidien

```bash
cd frontend

# Web dev server (hot reload)
ionic serve

# Build prod web
ionic build --prod

# Build + sync sur Android
ionic capacitor build android

# Ouvrir Android Studio
ionic capacitor open android

# Live reload sur device Android (PC + device sur même Wi-Fi)
ionic capacitor run android --livereload --external
```

### Tests

```bash
npm test                # unit Karma
npx playwright test     # E2E (Phase F6)
```

### Variables d'environnement

`src/environments/environment.ts` :
```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api/v1',
};
```

`src/environments/environment.prod.ts` :
```ts
export const environment = {
  production: true,
  apiUrl: 'https://api.tondomaine.com/api/v1',
};
```

---

## 14. Liens utiles pour la suite

- Ionic docs : https://ionicframework.com/docs
- Angular Signals : https://angular.dev/guide/signals
- Capacitor : https://capacitorjs.com/docs
- DOMPurify : https://github.com/cure53/DOMPurify
- Backend API : http://localhost:8081/swagger-ui.html

---

## 15. Notes pour le futur

- **PWA installable** : ajouter `@angular/pwa` schematic en F8, configurer manifest + service worker (cache shell, pas le contenu API)
- **Push notifications** : Capacitor `@capacitor/push-notifications` + backend qui poll un endpoint webhook → trigger push (Phase futur, demande inscription FCM / APNs)
- **Multi-utilisateur du même device** : profils dans Capacitor Preferences, picker au démarrage
- **Internationalisation** : `@angular/localize` + JSON par langue (`fr`, `en`)
- **Accessibilité** : audit Lighthouse en F6, contrastes WCAG AA, navigation clavier, screen-reader labels sur les SourcePill et ToolCallAccordion
- **Voice input** : Web Speech API + Capacitor Speech Recognition pour dicter les messages
- **Export conversation** : bouton "exporter en Markdown" sur ChatDetailPage
- **Recherche dans l'historique** : barre de recherche dans ChatListPage qui interroge `/chats/{id}/messages` ou un futur endpoint search global

---

*Plan rédigé fin Phase 4 backend. À mettre à jour au fil de l'exécution des sprints F1-F8.*
