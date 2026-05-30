import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MessageDto, PageResponse } from './chat.types';
import { SseStreamService } from '../../core/api/sse-stream.service';
import { SseEvent } from '../../core/api/sse-event.types';

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly http = inject(HttpClient);
  private readonly sse = inject(SseStreamService);
  private readonly base = environment.apiUrl;

  /** Historique paginé d'une session. */
  history(sessionId: number, page = 0, size = 50): Promise<PageResponse<MessageDto>> {
    return firstValueFrom(
      this.http.get<PageResponse<MessageDto>>(
        `${this.base}/chats/${sessionId}/messages?page=${page}&size=${size}`,
      ),
    );
  }

  /**
   * Envoi avec streaming SSE. Renvoie un Observable d'événements typés.
   *   - 'token'      → fragment de texte (Final Answer côté agent)
   *   - 'source'     → passage RAG retrouvé (avant les tokens)
   *   - 'tool_start' → outil appelé par l'agent (iter, name, args)
   *   - 'tool_end'   → résultat de l'outil (success, summary)
   *   - 'final'      → fin de génération avec messageId persisté en DB
   *   - 'error'      → erreur backend
   *
   * Unsubscribe → AbortController coupe la requête HTTP côté front.
   */
  sendStream(sessionId: number, content: string, clientRequestId?: string): Observable<SseEvent> {
    return this.sse.stream(
      `${this.base}/chats/${sessionId}/messages`,
      { content, ...(clientRequestId ? { clientRequestId } : {}) },
    );
  }
}
