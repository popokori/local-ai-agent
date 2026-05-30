# Frontend Ionic + Angular — état détaillé

Documentation détaillée du frontend pour reprendre ou faire évoluer.
Voir aussi : [`PROJECT_STATE.md`](PROJECT_STATE.md) pour la vue d'ensemble + backend.

---

## 1. Stack et versions

| Composant | Version | Pourquoi |
|---|---|---|
| Ionic | 8 | Composants natifs-look, dark mode, écosystème mature |
| Angular | 20 (standalone) | Pas de NgModule, signals, control flow `@if/@for` |
| Capacitor | 7.6.5 | v8 demandait Node ≥ 22 ; v7 marche en Node 20 |
| TypeScript | strict | Catch bugs tôt |
| Node.js | 20.19 | LTS actuelle |
| State management | Angular Signals | Pas de NgRx pour ce projet single-user |
| HTTP | `HttpClient` + interceptors fonctionnels | Standard |
| SSE | `fetch()` + `ReadableStream` | **Pas `EventSource`** : ne supporte pas le header `Authorization` |
| Forms | `ReactiveFormsModule` | Standard, type-safe |
| Storage tokens | Capacitor Preferences (mobile) / localStorage (web) | Abstrait dans `TokenStorageService` |
| Icons | `ionicons` | Inclus |

### Pourquoi PAS ces alternatives

| Rejet | Raison |
|---|---|
| React Native | Une autre stack, écosystème Ionic + Capacitor déjà cohérent |
| Flutter | Pas d'Angular |
| Pur Angular sans Ionic | Devrait réinventer les composants mobile-friendly |
| NgRx | Sur-engineering pour un mono-utilisateur |
| EventSource | Pas de header auth → JWT impossible ; fragile en WebView |
| Capacitor 8 | Demande Node 22, on a 20 |

---

## 2. Structure des dossiers

```
frontend/
├── ionic.config.json
├── angular.json                          (budgets CSS portés à 8/16 kB)
├── capacitor.config.ts                   (appId=mr.popo.localaiagent)
├── package.json
├── tsconfig.json, tsconfig.app.json, tsconfig.strict.json
├── src/
│   ├── main.ts                           applyBootEnvironment + theme pré-paint + bootstrapApplication
│   ├── index.html
│   ├── global.scss                       Imports Ionic core + dark.system + Inter font + utilities
│   ├── environments/
│   │   ├── environment.ts                production:false, apiUrl='http://localhost:8081/api/v1'
│   │   └── environment.prod.ts           production:true, apiUrl='/api/v1' (reverse proxy)
│   ├── theme/
│   │   └── variables.scss                Palette indigo+teal + dark theme tokens
│   ├── assets/                           (icons, logos)
│   └── app/
│       ├── app.config.ts                 providers : Ionic + Router + HttpClient(authInterceptor) + RouteReuseStrategy
│       ├── app.routes.ts                 routes (auth + shell wrap)
│       ├── app.component.ts              <ion-app><ion-router-outlet/></ion-app>
│       │
│       ├── core/                         services transversaux
│       │   ├── api/
│       │   │   ├── sse-event.types.ts    sealed union SseEvent + parseSseBlock()
│       │   │   └── sse-stream.service.ts fetch() + ReadableStream + retry 401 + AbortController
│       │   ├── auth/
│       │   │   ├── auth.types.ts         LoginRequest, RegisterRequest, TokenResponse, UserDto, ApiError
│       │   │   ├── token-storage.service.ts   Capacitor Preferences (mobile) / localStorage (web)
│       │   │   ├── auth.service.ts       login/register/refresh/logout, signal user, mutex anti-double-refresh
│       │   │   ├── auth.interceptor.ts   Bearer + 401 → tryRefresh + replay
│       │   │   ├── auth.guard.ts         canActivate avec restoreSession fallback
│       │   │   └── user.service.ts       updateMe + changePassword
│       │   └── ui/
│       │       ├── toast.service.ts      info/success/error avec mapping HttpErrorResponse
│       │       ├── theme.service.ts      dark/light/auto persisté + effect()
│       │       └── settings.service.ts   apiUrl + defaultModel persistés + applyBootEnvironment()
│       │
│       ├── shared/                       réutilisables UI
│       │   ├── components/
│       │   │   ├── app-shell.component.ts        ion-split-pane + ion-menu + ion-router-outlet, sidebar nav, avatar user, logout
│       │   │   ├── source-pill.component.ts      Badge cliquable doc+page → modal Ionic avec snippet
│       │   │   └── tool-call-accordion.component.ts  Liste tool calls collapsibles avec status
│       │   └── pipes/
│       │       └── relative-date.pipe.ts         "à l'instant", "il y a 2 min", "il y a 3 j"
│       │
│       └── features/                     pages applicatives
│           ├── auth/
│           │   ├── login.page.ts         card centrée gradient indigo/teal, brand mark, form 2 champs, toast erreur
│           │   └── register.page.ts      card 4 champs avec icônes, gradient inversé teal/indigo, back-link
│           ├── chat/
│           │   ├── chat.types.ts         ChatSession, Message, ChatMode, MessageRole, PageResponse<T>
│           │   ├── chat.service.ts       signal sessions, refresh/get/create/update/delete/touchLocal
│           │   ├── message.service.ts    history paginé + sendStream Observable<SseEvent>
│           │   ├── chat-list.page.ts     grid cards, mode-chip coloré, search, FAB, skeleton, empty state, pull-to-refresh
│           │   ├── chat-create.page.ts   form titre + 3 cards mode + KB picker liste
│           │   └── chat-detail.page.ts   header avec back/menu/mode-badge, ion-content scroll, ion-footer composer, bulles user/assistant, source-pills, tool-call-accordion, curseur clignotant, bouton copier, bouton stop, send-btn animé
│           ├── kb/
│           │   ├── kb.types.ts           KnowledgeBaseDto, CreateKbRequest, DocumentDto, DocumentStatus, Domain
│           │   ├── kb.service.ts         signal kbs, refresh/get/create/delete
│           │   ├── document.service.ts   upload Observable<UploadEvent> avec progress HttpClient + list/get/delete
│           │   ├── kb-list.page.ts       cards par domaine, icône colorée, FAB, empty state
│           │   ├── kb-create.page.ts     form titre + textarea + 7 cards domaine
│           │   └── kb-detail.page.ts     header KB, zone upload drag-drop multi-fichiers, barre progression %, liste docs avec status pill animé, polling auto 2s
│           ├── memory/
│           │   ├── memory.types.ts       UserFactDto, MemoryEntryDto, MemoryKind, UpsertFactRequest
│           │   ├── memory.service.ts     signal facts + entries, refresh/upsertFact/deleteFact/deleteEntry/purgeAll
│           │   └── memory.page.ts        tabs Profil/Souvenirs, ajout manuel inline, édition inline, suppression confirmée, bouton 🗑 Tout effacer dans header
│           └── settings/
│               └── settings.page.ts      5 sections : Apparence (3 cards theme) / Profil (form save) / Mot de passe (form) / LLM préféré / URL backend (apply + reload) / À propos
└── android/                              (non encore créé, F7)
```

**Tout est standalone components** : pas de `NgModule`.

---

## 3. Routes

| Path | Page | Auth | Description |
|---|---|---|---|
| `/login` | `LoginPage` | public | Form connexion |
| `/register` | `RegisterPage` | public | Form inscription |
| `/` | redirect → `/chats` | guard | |
| `/chats` | `ChatListPage` | guard | Liste sessions |
| `/chats/new` | `ChatCreatePage` | guard | Form création + KB picker |
| `/chats/:id` | `ChatDetailPage` | guard | **Page principale** chat + SSE stream |
| `/kbs` | `KbListPage` | guard | Liste KBs |
| `/kbs/new` | `KbCreatePage` | guard | Form création KB |
| `/kbs/:id` | `KbDetailPage` | guard | Détail KB + upload + docs |
| `/memory` | `MemoryPage` | guard | Tabs Profil / Souvenirs |
| `/settings` | `SettingsPage` | guard | 5 sections de préférences |

Pages **auth** sont en dehors du shell (pas de side menu).
Toutes les autres pages sont enfants d'`AppShellComponent` qui pose le `<ion-split-pane>` + `<ion-menu>` + `<ion-router-outlet>` interne.

---

## 4. Theme system

### Palette

```scss
--laa-brand: #6366f1          /* indigo-500 */
--laa-brand-soft: #818cf8     /* indigo-400 */
--laa-accent: #14b8a6         /* teal-500 */
```

Plus de tokens dans `src/theme/variables.scss` :
- Rayons : `--laa-radius-{sm,md,lg,xl}` = 8 / 12 / 18 / 24 px
- Font : `--laa-font-sans` (Inter), `--laa-font-mono` (JetBrains Mono)
- Step grays `--ion-color-step-{50,100,150,200,500,700,900}` redéfinis pour dark

### Modes (dark / light / auto)

Géré par `ThemeService` (`core/ui/theme.service.ts`) :
- Signal `mode: 'dark' | 'light' | 'auto'`
- `effect()` : applique `body.theme-dark` + `documentElement.style.colorScheme`
- Listener sur `prefers-color-scheme: dark` en mode auto
- Persisté dans `localStorage.laa.theme`
- **Init pré-paint** dans `main.ts` : lit localStorage et applique la classe AVANT le bootstrap Angular → zéro flash blanc en mode sombre

### Pour changer la palette

Édite `src/theme/variables.scss` :
```scss
:root {
  --laa-brand: #ff6b35;  /* nouveau brand */
  --laa-brand-rgb: 255, 107, 53;
  --laa-accent: #f7b801;
  ...
}
```

Les ion-color-* sont dérivés automatiquement. Pour aller plus loin, override aussi `--ion-color-primary`, `--ion-color-primary-shade`, etc.

---

## 5. Authentication flow

```
                  ┌────────────────────────────────────────────┐
                  │ login/register                             │
                  │   ↓                                        │
                  │ AuthService.login() → POST /auth/login     │
                  │   ↓                                        │
                  │ TokenResponse {accessToken, refreshToken}  │
                  │   ↓                                        │
                  │ TokenStorageService.set{Access,Refresh}    │
                  │   ↓                                        │
                  │ AuthService.loadProfile() → GET /users/me  │
                  │   ↓                                        │
                  │ _user.set(UserDto) → user signal updated   │
                  │   ↓                                        │
                  │ Router.navigate('/chats')                  │
                  └────────────────────────────────────────────┘

Sur chaque requête HTTP :
                  ┌────────────────────────────────────────────┐
                  │ authInterceptor                            │
                  │   ↓                                        │
                  │ getAccessToken() → header Authorization    │
                  │   ↓                                        │
                  │ next(req)                                  │
                  │   ↓                                        │
                  │ Si 401 → AuthService.tryRefresh()          │
                  │   ↓ (mutex anti-double-refresh)            │
                  │ POST /auth/refresh                         │
                  │   ↓                                        │
                  │ Replay requête originale avec nouveau JWT  │
                  │ Si refresh échoue → Router /login          │
                  └────────────────────────────────────────────┘

Sur ngOnInit d'une page protégée :
                  ┌────────────────────────────────────────────┐
                  │ authGuard (canActivate)                    │
                  │   ↓                                        │
                  │ Si isLoggedIn() → true                     │
                  │   ↓                                        │
                  │ Sinon authService.restoreSession()         │
                  │   ↓                                        │
                  │ Si access token en storage → loadProfile() │
                  │   ↓                                        │
                  │ Sinon redirect /login?redirect=<originalUrl>│
                  └────────────────────────────────────────────┘
```

### Cas particuliers

- **SSE `sendStream()`** ne passe pas par l'interceptor (utilise `fetch()` direct). Le Bearer est ajouté manuellement dans `SseStreamService.doFetch()`. Le 401 est aussi géré (1 retry avec refresh).
- **Mutex** sur `tryRefresh()` : si plusieurs requêtes 401 arrivent en même temps, on partage la promesse → un seul appel `/auth/refresh`.

---

## 6. Streaming SSE — `core/api/sse-stream.service.ts`

### Pourquoi PAS `EventSource`

| Limite EventSource | Notre besoin |
|---|---|
| Pas de header `Authorization` | JWT impossible |
| Pas de body POST (uniquement GET) | On poste le message |
| Fragile en WebView Capacitor | Cible mobile |

### Implémentation `fetch()` + `ReadableStream`

```ts
stream(url: string, body: unknown): Observable<SseEvent> {
  return new Observable<SseEvent>(subscriber => {
    const controller = new AbortController();
    (async () => {
      const token = await this.tokens.getAccessToken();
      let res = await this.doFetch(url, body, token, controller.signal);
      if (res.status === 401) { /* refresh + retry */ }
      const reader = res.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        let idx;
        while ((idx = buffer.indexOf('\n\n')) >= 0) {
          const block = buffer.slice(0, idx);
          buffer = buffer.slice(idx + 2);
          const evt = parseSseBlock(block);
          if (evt) subscriber.next(evt);
        }
      }
      subscriber.complete();
    })();
    return () => controller.abort();  // Unsubscribe → abort fetch
  });
}
```

### Événements typés (sealed union)

```ts
type SseEvent =
  | { event: 'source';     data: { index, documentId, documentName, page, score, snippet } }
  | { event: 'tool_start'; data: { iteration, name, arguments } }
  | { event: 'tool_end';   data: { iteration, name, success, summary } }
  | { event: 'token';      data: { text } }
  | { event: 'final';      data: { messageId, userMessageId?, replayed? } }
  | { event: 'error';      data: { message } };
```

### Consommation dans `ChatDetailPage`

```ts
this.streamSub = this.msgs.sendStream(sessionId, content).subscribe({
  next: evt => {
    switch (evt.event) {
      case 'source':     this.sources.update(s => [...s, evt]); break;
      case 'tool_start': this.toolCalls.update(t => [...t, {...}]); break;
      case 'tool_end':   this.toolCalls.update(...); break;
      case 'token':      this.streamingMessage.update(m => ({text: m.text + evt.data.text})); break;
      case 'final':      /* handled in complete */ break;
      case 'error':      this.toast.error(...); break;
    }
  },
  error: err => this.handleError(err, optimistic.id),
  complete: () => this.handleComplete(sessionId),
});
```

### Bouton **⏹ stop**

Click → `streamSub.unsubscribe()` → AbortController coupe le fetch → backend reçoit un `ClientAbortException`. Pas d'erreur côté UI grâce à `handleError` résilient (cf. §11).

---

## 7. State management — Angular Signals

### Pattern utilisé partout

```ts
@Injectable({ providedIn: 'root' })
export class SomeService {
  private readonly _items = signal<Item[]>([]);
  readonly items = this._items.asReadonly();
  readonly count = computed(() => this._items().length);

  async refresh(): Promise<void> {
    const list = await firstValueFrom(this.http.get<Item[]>(url));
    this._items.set(list);
  }

  async create(req: CreateReq): Promise<Item> {
    const created = await firstValueFrom(this.http.post<Item>(url, req));
    this._items.update(list => [created, ...list]);
    return created;
  }
}
```

### Côté pages

```ts
@Component({ ... })
export class SomePage {
  readonly svc = inject(SomeService);
  readonly searchText = signal('');
  readonly filtered = computed(() =>
    this.svc.items().filter(i => i.name.includes(this.searchText())),
  );
}
```

Template :
```html
@if (svc.loading()) { <skeleton/> }
@else if (filtered().length === 0) { <empty/> }
@else { @for (i of filtered(); track i.id) { <card/> } }
```

### Signals utilisés

| Page | Signals |
|---|---|
| `LoginPage` | `loading` |
| `ChatListPage` | `chats.sessions`, `chats.loading`, local `searchText`, `filtered` (computed) |
| `ChatDetailPage` | `session`, `messages`, `loading`, `sending`, `draft`, `canSend` (computed), `streamingMessage`, `sources`, `toolCalls`, `copiedId` |
| `KbDetailPage` | `kb`, `documents`, `uploads`, `loading`, `isDragOver`, polling local |
| `MemoryPage` | `mem.facts`, `mem.entries`, `tab`, `kindFilter`, `filteredEntries` (computed), `saving`, `editingId` |
| `SettingsPage` | `theme.mode`, `settings.apiUrl`, `settings.defaultModel`, `savingProfile`, `changingPwd`, `apiUrlDraft`, `canApplyUrl` (computed) |

---

## 8. Services backend appelés (récap mapping)

| Service front | Endpoint backend | Méthode |
|---|---|---|
| `AuthService.login` | `POST /auth/login` | |
| `AuthService.register` | `POST /auth/register` | |
| `AuthService.tryRefresh` | `POST /auth/refresh` | |
| `AuthService.logout` | `POST /auth/logout` | |
| `AuthService.loadProfile` | `GET /users/me` | |
| `UserService.updateMe` | `PATCH /users/me` | |
| `UserService.changePassword` | `POST /users/me/password` | |
| `ChatService.refresh` | `GET /chats?page=0&size=100&sort=lastMessageAt,desc` | signal cache |
| `ChatService.get` | `GET /chats/{id}` | |
| `ChatService.create` | `POST /chats` | optimistic |
| `ChatService.update` | `PATCH /chats/{id}` | optimistic |
| `ChatService.delete` | `DELETE /chats/{id}` | optimistic |
| `MessageService.history` | `GET /chats/{id}/messages?page&size` | |
| `MessageService.sendStream` | `POST /chats/{id}/messages` (text/event-stream) | via `SseStreamService.stream()` |
| `KbService.refresh` | `GET /kbs` | |
| `KbService.get` | `GET /kbs/{id}` | |
| `KbService.create` | `POST /kbs` | |
| `KbService.delete` | `DELETE /kbs/{id}` | |
| `DocumentService.upload` | `POST /kbs/{kbId}/documents` (multipart) | `reportProgress: true` |
| `DocumentService.list` | `GET /kbs/{kbId}/documents?page&size` | |
| `DocumentService.get` | `GET /documents/{id}` | poll status |
| `DocumentService.delete` | `DELETE /documents/{id}` | |
| `MemoryService.refresh` | `GET /memory/facts` + `GET /memory/entries` | parallèle |
| `MemoryService.upsertFact` | `POST /memory/facts` | |
| `MemoryService.deleteFact` | `DELETE /memory/facts/{id}` | |
| `MemoryService.deleteEntry` | `DELETE /memory/entries/{id}` | |

---

## 9. Composants partagés

### `AppShellComponent`

`<ion-split-pane contentId="main-content" when="(min-width: 992px)">` → menu visible permanent au-dessus de 992 px, en overlay en dessous.

`<ion-menu>` contient :
- Brand mark (gradient indigo/teal)
- Bouton "Nouvelle conversation" (link `/chats/new`)
- Liste de nav links avec `routerLinkActive="active"`
- Footer avec avatar utilisateur (initiales) + bouton logout

`<ion-router-outlet id="main-content">` rend la page enfant.

Pour **ajouter une page** dans le menu : éditer `navLinks` du `AppShellComponent` :
```ts
{ label: 'Ma page', icon: 'star-outline', path: '/ma-route' },
```

### `SourcePillComponent`

`@Input() src: SourceEvent`. Affiche un badge cliquable avec :
- Index (1, 2, 3…)
- Icône document
- Nom du fichier
- Page si dispo

Click → ouvre un `IonModal` avec :
- Titre "Source N"
- Nom du document + page + score
- Snippet complet du chunk

### `ToolCallAccordionComponent`

`@Input() calls: ToolCall[]`. Affiche une liste collapsible :
- Numéro d'itération en bulle
- Nom du tool en mono
- Summary résumé
- Status : spinner / ✓ vert / ✗ rouge
- Click sur une ligne → expand JSON des arguments + summary

### `RelativeDatePipe`

Standalone pipe : transforme une date ISO en "à l'instant", "il y a 2 min", "il y a 3 j", date locale.

---

## 10. Bugs résolus en cours de développement

| Bug | Cause | Solution |
|---|---|---|
| Input chat invisible | `ion-textarea` ne s'affichait pas dans flex container (shadow DOM) | Remplacé par `<textarea>` HTML natif stylé |
| Composer 30% visible | `.page { height: 100% }` débordait | Passé à `<ion-footer>` natif Ionic |
| Composer totalement invisible | `:host { display: contents }` empêchait IonRouterOutlet de poser ion-page | Retiré le `:host` du chat-detail |
| Bouton send toujours désactivé | `draft = ''` string, pas signal → `computed(canSend)` ne se recalculait jamais | `draft = signal('')` + `[value]/(input)` |
| `ERR_INCOMPLETE_CHUNKED_ENCODING` | Spring SseEmitter ferme sans marqueur final, Angular HttpClient ne supporte pas | Switch vers `fetch()` + `ReadableStream` |
| Erreur "Conversation introuvable" sur reload | Token expiré pendant navigation | Auth interceptor + tryRefresh |
| Question disparaît + erreur après réponse | Backend lève `AsyncRequestNotUsableException` après end du stream | `handleError` résilient : si du contenu a déjà été reçu → reload history + pas de toast |
| Streaming pas en live avec dolphin-mistral | Backend buffer en attendant `Final Answer:` qui n'arrive jamais | Auto-détection après 60 chars (côté backend `AgentService.handleDelta`) |
| Cache TypeScript stale | Ionic serve ne voyait pas les nouveaux fichiers | Kill + restart ionic serve |
| CORS 403 depuis 8100 | Profil windows-local hardcodait `4200,5173,3000` | `${FRONT_ORIGIN:...,http://localhost:8100}` |
| Budget CSS Angular dépassé | Limite 4 KB par composant en prod | Porté à 16 KB dans `angular.json` |

---

## 11. Commandes utiles

### Dev local

```powershell
cd C:\Users\Administrateur\Pictures\llmproject\frontend

# Dev server avec hot reload (ouvre http://localhost:8100)
ionic serve

# Ou sans ouvrir le navigateur automatique
ionic serve --no-open --port 8100

# Build prod (output www/)
ionic build --prod

# Tests unitaires Karma
npm test
```

### Mobile (Phase F7, à venir)

```powershell
# Ajouter la plateforme Android
ionic capacitor add android

# Sync du build vers le projet natif
ionic capacitor sync android

# Ouvrir dans Android Studio
ionic capacitor open android

# Live reload sur device physique
ionic capacitor run android --livereload --external
```

### Variables d'environnement frontend

Aucune env var côté frontend (sauf via Settings UI). Les valeurs persistées dans `localStorage` :

| Clé | Rôle | Valeur défaut |
|---|---|---|
| `laa.theme` | Mode thème | `dark` |
| `laa.apiUrl` | Override URL backend | `environment.apiUrl` |
| `laa.defaultModel` | Modèle préféré (UI hint) | vide |
| `access_token` | JWT access | `null` |
| `refresh_token` | JWT refresh | `null` |

---

## 12. Performance et bundles

### Bundle initial (dev mode)

| Chunk | Taille brute | Taille gzip |
|---|---|---|
| `main.js` | 35 kB | ~12 kB |
| `runtime.js` | 13 kB | ~5 kB |
| Initial total | ~1.4 MB | ~280 kB |

### Chunks lazy par page

| Page | Taille |
|---|---|
| `chat-list-page` | 30 kB |
| `chat-detail-page` | 94 kB (gros à cause du CSS inline + logique stream) |
| `chat-create-page` | 34 kB |
| `kb-list-page` | 25 kB |
| `kb-detail-page` | 35 kB |
| `kb-create-page` | 21 kB |
| `memory-page` | 32 kB |
| `settings-page` | 49 kB |
| `app-shell-component` | 18 kB |

Total transmis pour un parcours complet : ~360 kB.

### Optimisations possibles

- Externaliser les `styles: [...]` en `.scss` séparés (gain ~30 kB sur main + chunks plus petits)
- Tree-shaking des icônes Ionic (déjà fait via `addIcons(...)`)
- Lazy load les modals (déjà fait via standalone components)
- En F8 : Service Worker PWA pour cache statique

---

## 13. Sécurité

### Mesures actuelles

- **Auth JWT** stateless, refresh avec rotation
- **Bearer dans header**, pas dans URL
- **Tokens en Capacitor Preferences** sur mobile (chiffrement OS-level prévu pour F7 avec `@capacitor/preferences-secure`)
- **CORS** strict côté backend (whitelist explicite)
- **HTTPS** obligatoire en prod (à activer avec reverse proxy)

### Pour F7 polish

- Passer à `@capacitor/preferences-secure` (Keychain iOS / KeyStore Android)
- Désactiver `console.log` en prod
- Audit `npm audit fix`
- Helmet équivalent pour PWA service worker (`Content-Security-Policy`)
- Markdown LLM : pour l'instant `{{ m.content }}` (text content). En F7+ : `marked` + `DOMPurify` pour rendre Markdown avec sanitization XSS.

---

## 14. Roadmap restante (F7 / F8)

| Sprint | Périmètre | Estimation |
|---|---|---|
| **F7 — Mobile Android** | `ionic capacitor add android`, icons + splash, build APK debug, fix layouts mobile, test sur device physique | 4 j |
| **F8 — iOS + PWA prod + signing** | `cap add ios` (Mac requis), service worker PWA, manifest, signing release Android (Play Store), TestFlight iOS | 5 j |
| **Polish optionnel** | Markdown rendering (marked + DOMPurify), recherche dans historique, export Markdown, voice input | … |

---

## 15. Référence rapide — modifications futures

| Je veux… | Où regarder |
|---|---|
| Changer la palette de couleurs | `src/theme/variables.scss` (`--laa-brand`, `--laa-accent`, etc.) |
| Modifier le prompt système (côté backend) | `backend/src/main/resources/prompts/system_normal.md` |
| Ajouter une nouvelle page | Créer `features/xxx/xxx.page.ts`, ajouter route dans `app.routes.ts`, ajouter dans `navLinks` de `app-shell.component.ts` |
| Ajouter un mode de chat | `chat.types.ts` (enum) + `chat-create.page.ts` (carte mode) + `chat-list.page.ts` (chip color) + `chat-detail.page.ts` (badge) |
| Ajouter un type d'événement SSE | `core/api/sse-event.types.ts` (union) + `chat-detail.page.ts` handleEvent switch + un composant UI dédié si besoin |
| Ajouter un nouveau service backend → endpoint REST | Créer `xxx.service.ts` injecté `HttpClient`, exposer signals, importer dans la page |
| Personnaliser le toast | `core/ui/toast.service.ts` |
| Changer la fréquence de polling docs | `kb-detail.page.ts` → `pollTimer = setInterval(..., 2000)` |
| Modifier le streaming Auto-détection | `backend/AgentService.java` → `DIRECT_MODE_DETECTION_CHARS` |
| Réactiver l'auto-consolidation mémoire | `AGENT_MEMORY_CONSOLIDATE=true` ou via UI (à faire si on l'ajoute) |
| Build Mobile APK | Voir §11 commandes (Phase F7) |

---

*Dernière mise à jour : fin F6. Toutes les pages web sont fonctionnelles avec dark mode + streaming live + auth + RAG + mémoire + paramètres.*
