import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreateKbRequest, KnowledgeBaseDto } from './kb.types';

@Injectable({ providedIn: 'root' })
export class KbService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/kbs`;

  private readonly _kbs = signal<KnowledgeBaseDto[]>([]);
  readonly kbs = this._kbs.asReadonly();
  readonly count = computed(() => this._kbs().length);

  private readonly _loading = signal(false);
  readonly loading = this._loading.asReadonly();

  async refresh(): Promise<void> {
    this._loading.set(true);
    try {
      const list = await firstValueFrom(
        this.http.get<KnowledgeBaseDto[]>(this.base),
      );
      this._kbs.set(list);
    } finally {
      this._loading.set(false);
    }
  }

  get(id: number): Promise<KnowledgeBaseDto> {
    return firstValueFrom(this.http.get<KnowledgeBaseDto>(`${this.base}/${id}`));
  }

  async create(request: CreateKbRequest): Promise<KnowledgeBaseDto> {
    const created = await firstValueFrom(
      this.http.post<KnowledgeBaseDto>(this.base, request),
    );
    this._kbs.update(list => [created, ...list]);
    return created;
  }

  async delete(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.base}/${id}`));
    this._kbs.update(list => list.filter(k => k.id !== id));
  }
}
