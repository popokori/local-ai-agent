import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ChatSessionDto, CreateChatRequest, UpdateChatRequest, PageResponse,
} from './chat.types';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  private readonly _sessions = signal<ChatSessionDto[]>([]);
  readonly sessions = this._sessions.asReadonly();
  readonly count = computed(() => this._sessions().length);

  private readonly _loading = signal(false);
  readonly loading = this._loading.asReadonly();

  async refresh(): Promise<void> {
    this._loading.set(true);
    try {
      // Pagination future : pour MVP on prend page=0 size=100 (largement suffisant single-user)
      const page = await firstValueFrom(
        this.http.get<PageResponse<ChatSessionDto>>(
          `${this.base}/chats?page=0&size=100&sort=lastMessageAt,desc`,
        ),
      );
      this._sessions.set(page.content);
    } finally {
      this._loading.set(false);
    }
  }

  get(id: number): Promise<ChatSessionDto> {
    return firstValueFrom(this.http.get<ChatSessionDto>(`${this.base}/chats/${id}`));
  }

  async create(request: CreateChatRequest): Promise<ChatSessionDto> {
    const created = await firstValueFrom(
      this.http.post<ChatSessionDto>(`${this.base}/chats`, request),
    );
    this._sessions.update(list => [created, ...list]);
    return created;
  }

  async update(id: number, request: UpdateChatRequest): Promise<ChatSessionDto> {
    const updated = await firstValueFrom(
      this.http.patch<ChatSessionDto>(`${this.base}/chats/${id}`, request),
    );
    this._sessions.update(list => list.map(s => (s.id === id ? updated : s)));
    return updated;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.base}/chats/${id}`));
    this._sessions.update(list => list.filter(s => s.id !== id));
  }

  /** Met à jour localement le lastMessageAt après un nouveau message. */
  touchLocal(id: number): void {
    const now = new Date().toISOString();
    this._sessions.update(list => {
      const idx = list.findIndex(s => s.id === id);
      if (idx < 0) return list;
      const updated = { ...list[idx], lastMessageAt: now };
      // Replace + push to top
      const without = list.filter(s => s.id !== id);
      return [updated, ...without];
    });
  }
}
