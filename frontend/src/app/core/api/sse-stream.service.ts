import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TokenStorageService } from '../auth/token-storage.service';
import { AuthService } from '../auth/auth.service';
import { SseEvent, parseSseBlock } from './sse-event.types';

/**
 * Streaming SSE générique. N'utilise PAS `EventSource` :
 *   - EventSource ne supporte pas le header Authorization (JWT impossible)
 *   - Peu fiable dans les WebViews Capacitor
 *
 * On utilise fetch() + ReadableStream avec parsing incrémental des blocs
 * `event:foo\ndata:{...}\n\n`. Auth Bearer ajoutée manuellement avec retry
 * 401 → tryRefresh → replay une fois.
 *
 * AbortController câblé sur le teardown de l'Observable : si le consumer
 * unsubscribe, la requête HTTP est coupée immédiatement.
 */
@Injectable({ providedIn: 'root' })
export class SseStreamService {
  private readonly tokens = inject(TokenStorageService);
  private readonly auth = inject(AuthService);

  stream(url: string, body: unknown): Observable<SseEvent> {
    return new Observable<SseEvent>(subscriber => {
      const controller = new AbortController();

      (async () => {
        try {
          let token = await this.tokens.getAccessToken();
          let res = await this.doFetch(url, body, token, controller.signal);

          if (res.status === 401) {
            const refreshed = await this.auth.tryRefresh();
            if (refreshed) {
              token = await this.tokens.getAccessToken();
              res = await this.doFetch(url, body, token, controller.signal);
            }
          }
          if (!res.ok) {
            subscriber.error(new Error(`HTTP ${res.status} ${res.statusText}`));
            return;
          }
          if (!res.body) {
            subscriber.error(new Error('Empty response body'));
            return;
          }

          const reader = res.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          // eslint-disable-next-line no-constant-condition
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });

            let idx: number;
            while ((idx = buffer.indexOf('\n\n')) >= 0) {
              const block = buffer.slice(0, idx);
              buffer = buffer.slice(idx + 2);
              const evt = parseSseBlock(block);
              if (evt) subscriber.next(evt);
            }
          }
          // Flush résiduel (parfois pas de \n\n final)
          if (buffer.trim()) {
            const evt = parseSseBlock(buffer);
            if (evt) subscriber.next(evt);
          }
          subscriber.complete();
        } catch (err) {
          // AbortError attendu sur unsubscribe — silencieux
          if ((err as Error)?.name === 'AbortError') {
            subscriber.complete();
            return;
          }
          subscriber.error(err);
        }
      })();

      return () => controller.abort();
    });
  }

  private async doFetch(
    url: string,
    body: unknown,
    accessToken: string | null,
    signal: AbortSignal,
  ): Promise<Response> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json; charset=utf-8',
      Accept: 'text/event-stream',
    };
    if (accessToken) headers['Authorization'] = `Bearer ${accessToken}`;
    return await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal,
    });
  }
}
