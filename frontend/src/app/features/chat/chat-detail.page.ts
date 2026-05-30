import {
  Component, OnInit, ViewChild, computed, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonBackButton, IonSpinner, IonMenuButton, IonFooter,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  arrowUpOutline, stopOutline, copyOutline, checkmarkOutline,
} from 'ionicons/icons';
import { Subscription } from 'rxjs';

import { ChatService } from './chat.service';
import { MessageService } from './message.service';
import { ChatSessionDto, MessageDto } from './chat.types';
import { ToastService } from '../../core/ui/toast.service';
import { RelativeDatePipe } from '../../shared/pipes/relative-date.pipe';
import { SourceEvent, SseEvent } from '../../core/api/sse-event.types';
import { SourcePillComponent } from '../../shared/components/source-pill.component';
import {
  ToolCallAccordionComponent, ToolCall,
} from '../../shared/components/tool-call-accordion.component';

@Component({
  selector: 'app-chat-detail',
  standalone: true,
  imports: [
    CommonModule, RelativeDatePipe,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonBackButton, IonSpinner, IonMenuButton, IonFooter,
    SourcePillComponent, ToolCallAccordionComponent,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }

    .mode-badge {
      display: inline-flex; align-items: center;
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600; letter-spacing: 0.02em;
      margin-left: 8px;
    }
    .mode-NORMAL     { background: rgba(99,102,241,0.16);  color: var(--laa-brand-soft); }
    .mode-EXPERT     { background: rgba(245,158,11,0.16);  color: #fbbf24; }
    .mode-FACT_CHECK { background: rgba(16,185,129,0.16);  color: #34d399; }

    .messages-inner {
      max-width: 860px; margin: 0 auto;
      padding: 24px 16px 16px;
      display: flex; flex-direction: column; gap: 16px;
    }

    .bubble {
      display: flex; flex-direction: column;
      max-width: 86%;
      padding: 12px 16px;
      border-radius: var(--laa-radius-lg);
      line-height: 1.55;
      word-wrap: break-word;
      animation: pop-in 0.15s ease-out;
    }
    @keyframes pop-in {
      from { opacity: 0; transform: translateY(4px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    .bubble.USER {
      align-self: flex-end;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-brand-soft) 100%);
      color: #fff;
      border-bottom-right-radius: 6px;
    }
    .bubble.ASSISTANT {
      align-self: flex-start;
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.1);
      border-bottom-left-radius: 6px;
    }
    .bubble.streaming {
      box-shadow: 0 0 0 2px rgba(var(--ion-color-primary-rgb), 0.16);
    }
    .bubble-content { white-space: pre-wrap; }
    .bubble-content .cursor {
      display: inline-block;
      width: 8px; height: 1em;
      background: var(--ion-color-primary);
      vertical-align: text-bottom;
      animation: blink 1s steps(2) infinite;
      margin-left: 2px;
      border-radius: 2px;
    }
    @keyframes blink { 50% { opacity: 0; } }
    .bubble-meta {
      display: flex; gap: 8px; align-items: center;
      font-size: 0.7rem; opacity: 0.65;
      margin-top: 6px;
    }
    .bubble.USER .bubble-meta { justify-content: flex-end; }

    /* Actions bar (copy) — bulle USER ou ASSISTANT */
    .bubble-actions {
      display: flex; gap: 4px;
      margin-top: 6px;
      opacity: 0;
      transition: opacity 0.15s;
    }
    .bubble:hover .bubble-actions,
    .bubble.streaming .bubble-actions { opacity: 1; }
    @media (pointer: coarse) {
      /* Mobile : toujours visibles (pas de hover possible) */
      .bubble-actions { opacity: 1; }
    }
    .bubble.USER .bubble-actions { justify-content: flex-end; }

    .copy-btn {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 4px 8px;
      background: transparent;
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.15);
      border-radius: var(--laa-radius-sm);
      color: var(--ion-color-step-500);
      font-size: 0.72rem;
      cursor: pointer;
      transition: background 0.12s, color 0.12s, border-color 0.12s;
    }
    .copy-btn:hover {
      background: rgba(var(--ion-color-primary-rgb), 0.08);
      color: var(--ion-color-primary);
      border-color: rgba(var(--ion-color-primary-rgb), 0.3);
    }
    .copy-btn ion-icon { font-size: 14px; }
    .copy-btn.copied {
      color: var(--ion-color-success);
      border-color: rgba(var(--ion-color-success-rgb), 0.4);
      background: rgba(var(--ion-color-success-rgb), 0.08);
    }
    /* Variante claire pour le fond USER (gradient indigo) */
    .bubble.USER .copy-btn {
      color: rgba(255, 255, 255, 0.78);
      border-color: rgba(255, 255, 255, 0.28);
    }
    .bubble.USER .copy-btn:hover {
      color: #ffffff;
      background: rgba(255, 255, 255, 0.12);
      border-color: rgba(255, 255, 255, 0.55);
    }
    .bubble.USER .copy-btn.copied {
      color: #d1fae5;
      background: rgba(16, 185, 129, 0.25);
      border-color: rgba(16, 185, 129, 0.5);
    }
    .bubble-sources {
      display: flex; gap: 6px; flex-wrap: wrap;
      margin-top: 10px;
    }

    .typing {
      align-self: flex-start;
      display: inline-flex; gap: 4px; padding: 14px 16px;
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.1);
      border-radius: var(--laa-radius-lg);
      border-bottom-left-radius: 6px;
    }
    .typing span {
      width: 6px; height: 6px; border-radius: 50%;
      background: var(--ion-color-step-200);
      animation: typing 1.2s infinite;
    }
    .typing span:nth-child(2) { animation-delay: 0.2s; }
    .typing span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes typing {
      0%, 60%, 100% { opacity: 0.3; transform: translateY(0); }
      30% { opacity: 1; transform: translateY(-3px); }
    }

    ion-footer {
      background: var(--ion-toolbar-background, var(--ion-card-background));
      border-top: 1px solid rgba(var(--ion-color-primary-rgb), 0.10);
    }
    ion-footer::before { display: none; }
    .composer-bar { padding: 10px 12px env(safe-area-inset-bottom); }
    .composer {
      max-width: 860px; margin: 0 auto;
      display: flex; gap: 8px; align-items: flex-end;
    }
    .composer textarea.native {
      flex: 1; width: 100%;
      min-height: 44px; max-height: 160px;
      padding: 12px 14px;
      background: var(--ion-card-background);
      color: var(--ion-text-color);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.18);
      border-radius: var(--laa-radius-lg);
      font-family: var(--laa-font-sans);
      font-size: 0.95rem; line-height: 1.4;
      resize: none; outline: none;
      transition: border-color 0.15s, box-shadow 0.15s;
    }
    .composer textarea.native:focus {
      border-color: var(--ion-color-primary);
      box-shadow: 0 0 0 3px rgba(var(--ion-color-primary-rgb), 0.16);
    }
    .composer textarea.native:disabled { opacity: 0.6; cursor: not-allowed; }
    .composer textarea.native::placeholder { color: var(--ion-color-step-500); }
    .send-btn {
      --background: var(--ion-color-primary);
      --background-hover: var(--ion-color-primary-shade);
      --border-radius: var(--laa-radius-md);
      --padding-start: 16px; --padding-end: 16px;
      margin: 0; height: 44px; min-width: 56px;
    }
    .send-btn[disabled] { --background: var(--ion-color-step-100); --color: var(--ion-color-step-500); }
    .stop-btn {
      --background: var(--ion-color-danger);
      --background-hover: #b91c1c;
      --border-radius: var(--laa-radius-md);
      --padding-start: 16px; --padding-end: 16px;
      margin: 0; height: 44px; min-width: 56px;
    }

    .empty-chat {
      max-width: 480px; margin: 60px auto;
      text-align: center; padding: 16px;
      color: var(--ion-color-step-500);
    }
    .empty-chat h3 {
      font-size: 1.1rem; color: var(--ion-text-color); margin: 0 0 6px;
    }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
          <ion-back-button defaultHref="/chats"></ion-back-button>
        </ion-buttons>
        <ion-title>
          {{ session()?.title || 'Conversation' }}
          @if (session(); as s) {
            <span class="mode-badge" [class]="'mode-' + s.mode">{{ s.mode }}</span>
          }
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content #content>
      <div class="messages-inner">
        @if (loading()) {
          <div class="empty-chat">
            <ion-spinner name="dots"></ion-spinner>
          </div>
        } @else if (messages().length === 0 && !streamingMessage()) {
          <div class="empty-chat">
            <h3>✨ Lance la conversation</h3>
            <p>Pose une question, demande une explication, génère du code…</p>
          </div>
        } @else {
          @for (m of messages(); track m.id) {
            <div class="bubble" [class]="m.role">
              <div class="bubble-content">{{ m.content }}</div>
              <div class="bubble-meta">
                @if (m.latencyMs !== null && m.role === 'ASSISTANT') {
                  <span>{{ (m.latencyMs / 1000) | number:'1.1-1' }} s</span>
                  <span>·</span>
                }
                <span>{{ m.createdAt | relativeDate }}</span>
              </div>
              @if (m.content) {
                <div class="bubble-actions">
                  <button
                    type="button"
                    class="copy-btn"
                    [class.copied]="copiedId() === m.id"
                    (click)="copy(m.content, m.id)"
                    [attr.aria-label]="m.role === 'USER' ? 'Copier mon message' : 'Copier la réponse'">
                    @if (copiedId() === m.id) {
                      <ion-icon name="checkmark-outline"></ion-icon>
                      <span>Copié</span>
                    } @else {
                      <ion-icon name="copy-outline"></ion-icon>
                      <span>Copier</span>
                    }
                  </button>
                </div>
              }
            </div>
          }

          @if (streamingMessage(); as live) {
            <div class="bubble ASSISTANT streaming">
              @if (toolCalls().length > 0) {
                <tool-call-accordion [calls]="toolCalls()"></tool-call-accordion>
              }

              @if (live.text.length === 0 && toolCalls().length === 0 && sources().length === 0) {
                <div class="typing"><span></span><span></span><span></span></div>
              } @else if (live.text.length > 0) {
                <div class="bubble-content">{{ live.text }}<span class="cursor"></span></div>
              }

              @if (sources().length > 0) {
                <div class="bubble-sources">
                  @for (s of sources(); track s.data.index) {
                    <source-pill [src]="s"></source-pill>
                  }
                </div>
              }

              @if (live.text.length > 0) {
                <div class="bubble-actions">
                  <button
                    type="button"
                    class="copy-btn"
                    [class.copied]="copiedId() === -1"
                    (click)="copy(live.text, -1)"
                    aria-label="Copier la réponse en cours">
                    @if (copiedId() === -1) {
                      <ion-icon name="checkmark-outline"></ion-icon>
                      <span>Copié</span>
                    } @else {
                      <ion-icon name="copy-outline"></ion-icon>
                      <span>Copier</span>
                    }
                  </button>
                </div>
              }
            </div>
          }
        }
      </div>
    </ion-content>

    <ion-footer>
      <div class="composer-bar">
        <div class="composer">
          <textarea
            class="native"
            [value]="draft()"
            placeholder="Écris ton message…  (Entrée pour envoyer, Maj+Entrée pour aller à la ligne)"
            rows="1"
            maxlength="4000"
            (input)="onInput($event)"
            (keydown.enter)="onEnter($event)"
            [disabled]="sending() || !session()"></textarea>

          @if (sending()) {
            <ion-button class="stop-btn" (click)="cancel()" aria-label="Arrêter">
              <ion-icon name="stop-outline" slot="icon-only"></ion-icon>
            </ion-button>
          } @else {
            <ion-button
              class="send-btn"
              (click)="send()"
              [disabled]="!canSend()"
              aria-label="Envoyer">
              <ion-icon name="arrow-up-outline" slot="icon-only"></ion-icon>
            </ion-button>
          }
        </div>
      </div>
    </ion-footer>
  `,
})
export class ChatDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly chats = inject(ChatService);
  private readonly msgs = inject(MessageService);
  private readonly toast = inject(ToastService);

  readonly session = signal<ChatSessionDto | null>(null);
  readonly messages = signal<MessageDto[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly draft = signal('');

  /** Message en cours de stream (texte agrégé au fil des event:token). */
  readonly streamingMessage = signal<{ text: string } | null>(null);
  readonly sources = signal<SourceEvent[]>([]);
  readonly toolCalls = signal<ToolCall[]>([]);

  /** ID du message dont le contenu vient d'être copié (pour feedback "Copié ✓"). */
  readonly copiedId = signal<number | null>(null);
  private copyTimer?: ReturnType<typeof setTimeout>;

  readonly canSend = computed(() => this.draft().trim().length > 0);

  @ViewChild('content') content?: { scrollToBottom: (duration?: number) => Promise<void> };

  private streamSub?: Subscription;

  constructor() {
    addIcons({ arrowUpOutline, stopOutline, copyOutline, checkmarkOutline });
  }

  async copy(text: string, id: number): Promise<void> {
    if (!text) return;
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
      } else {
        // Fallback ancien navigateur / contexte non sécurisé
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
      }
      this.copiedId.set(id);
      if (this.copyTimer) clearTimeout(this.copyTimer);
      this.copyTimer = setTimeout(() => this.copiedId.set(null), 1500);
    } catch (err) {
      await this.toast.error(err, 'Copie impossible');
    }
  }

  async ngOnInit(): Promise<void> {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) { await this.router.navigate(['/chats']); return; }
    const id = Number(idParam);
    try {
      const [session, history] = await Promise.all([
        this.chats.get(id),
        this.msgs.history(id, 0, 100),
      ]);
      this.session.set(session);
      this.messages.set(history.content);
      requestAnimationFrame(() => this.scrollBottom());
    } catch (err) {
      await this.toast.error(err, 'Conversation introuvable.');
      await this.router.navigate(['/chats']);
    } finally {
      this.loading.set(false);
    }
  }

  onInput(event: Event): void {
    const t = event.target as HTMLTextAreaElement;
    this.draft.set(t.value);
    t.style.height = 'auto';
    t.style.height = Math.min(t.scrollHeight, 160) + 'px';
  }

  onEnter(event: Event): void {
    const e = event as KeyboardEvent;
    if (e.shiftKey) return;
    e.preventDefault();
    this.send();
  }

  send(): void {
    if (!this.canSend() || this.sending() || !this.session()) return;

    const content = this.draft().trim();
    const sessionId = this.session()!.id;
    this.draft.set('');
    this.resetStreamState();
    this.sending.set(true);

    const optimistic: MessageDto = {
      id: -Date.now(),
      sessionId,
      role: 'USER',
      content,
      tokensIn: null, tokensOut: null, latencyMs: null,
      clientRequestId: null,
      createdAt: new Date().toISOString(),
    };
    this.messages.update(list => [...list, optimistic]);
    this.streamingMessage.set({ text: '' });
    requestAnimationFrame(() => this.scrollBottom());

    this.streamSub = this.msgs.sendStream(sessionId, content).subscribe({
      next: evt => this.handleEvent(evt),
      error: err => this.handleError(err, optimistic.id),
      complete: () => this.handleComplete(sessionId),
    });
  }

  cancel(): void {
    this.streamSub?.unsubscribe();
    this.streamSub = undefined;
    this.sending.set(false);
    this.streamingMessage.set(null);
  }

  private handleEvent(evt: SseEvent): void {
    switch (evt.event) {
      case 'source':
        this.sources.update(list => [...list, evt]);
        break;

      case 'tool_start':
        this.toolCalls.update(list => [
          ...list,
          {
            iteration: evt.data.iteration,
            name: evt.data.name,
            arguments: evt.data.arguments ?? {},
            status: 'running',
          },
        ]);
        this.scrollBottom();
        break;

      case 'tool_end':
        this.toolCalls.update(list =>
          list.map(t =>
            t.iteration === evt.data.iteration && t.name === evt.data.name
              ? {
                  ...t,
                  status: evt.data.success ? 'success' : 'error',
                  summary: evt.data.summary,
                }
              : t,
          ),
        );
        break;

      case 'token': {
        const txt = evt.data?.text ?? '';
        if (!txt) break;
        this.streamingMessage.update(m => (m ? { text: m.text + txt } : { text: txt }));
        this.scrollBottom();
        break;
      }

      case 'final':
        // Le complete suit immédiatement, géré dans handleComplete
        break;

      case 'error':
        this.toast.error(new Error(evt.data?.message ?? 'Erreur stream'));
        break;
    }
  }

  private async handleComplete(sessionId: number): Promise<void> {
    try {
      const history = await this.msgs.history(sessionId, 0, 100);
      this.messages.set(history.content);
      this.chats.touchLocal(sessionId);
    } catch (err) {
      await this.toast.error(err, 'Impossible de rafraîchir l’historique.');
    } finally {
      this.streamingMessage.set(null);
      this.sending.set(false);
      this.streamSub = undefined;
      requestAnimationFrame(() => this.scrollBottom());
    }
  }

  private async handleError(err: unknown, optimisticId: number): Promise<void> {
    // Si on a déjà reçu du contenu (le LLM a répondu mais le backend ferme la
    // connexion de façon brutale après le dernier event "final" — bug connu de
    // SseEmitter Spring), on garde la conversation et on recharge l'historique
    // pour récupérer la vraie version persistée. Sinon (échec réel avant toute
    // réponse), on retire la bulle USER optimiste et on affiche l'erreur.
    const hadContent = (this.streamingMessage()?.text.length ?? 0) > 0
                    || this.toolCalls().length > 0
                    || this.sources().length > 0;

    this.streamingMessage.set(null);
    this.sending.set(false);
    this.streamSub = undefined;

    const sessionId = this.session()?.id;
    if (hadContent && sessionId !== undefined) {
      try {
        const history = await this.msgs.history(sessionId, 0, 100);
        this.messages.set(history.content);
        this.chats.touchLocal(sessionId);
        return; // succès silencieux — on n'affiche pas l'erreur
      } catch {
        /* tombe sur la branche erreur */
      }
    }

    this.messages.update(list => list.filter(m => m.id !== optimisticId));
    await this.toast.error(err, 'Envoi impossible.');
  }

  private resetStreamState(): void {
    this.sources.set([]);
    this.toolCalls.set([]);
    this.streamingMessage.set(null);
  }

  private scrollBottom(): void {
    this.content?.scrollToBottom?.(120);
  }
}
