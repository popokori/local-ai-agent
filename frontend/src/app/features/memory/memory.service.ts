import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MemoryEntryDto, UpsertFactRequest, UserFactDto } from './memory.types';

@Injectable({ providedIn: 'root' })
export class MemoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/memory`;

  private readonly _facts = signal<UserFactDto[]>([]);
  readonly facts = this._facts.asReadonly();
  readonly factsCount = computed(() => this._facts().length);

  private readonly _entries = signal<MemoryEntryDto[]>([]);
  readonly entries = this._entries.asReadonly();
  readonly entriesCount = computed(() => this._entries().length);

  private readonly _loading = signal(false);
  readonly loading = this._loading.asReadonly();

  async refresh(): Promise<void> {
    this._loading.set(true);
    try {
      const [facts, entries] = await Promise.all([
        firstValueFrom(this.http.get<UserFactDto[]>(`${this.base}/facts`)),
        firstValueFrom(this.http.get<MemoryEntryDto[]>(`${this.base}/entries`)),
      ]);
      this._facts.set(facts);
      this._entries.set(entries);
    } finally {
      this._loading.set(false);
    }
  }

  async upsertFact(request: UpsertFactRequest): Promise<UserFactDto> {
    const created = await firstValueFrom(
      this.http.post<UserFactDto>(`${this.base}/facts`, request),
    );
    this._facts.update(list => {
      const idx = list.findIndex(f => f.factKey === created.factKey);
      if (idx >= 0) {
        const copy = [...list];
        copy[idx] = created;
        return copy;
      }
      return [created, ...list];
    });
    return created;
  }

  async deleteFact(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.base}/facts/${id}`));
    this._facts.update(list => list.filter(f => f.id !== id));
  }

  async deleteEntry(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.base}/entries/${id}`));
    this._entries.update(list => list.filter(e => e.id !== id));
  }

  /** Purge tout : utilisé par le bouton "Tout effacer" de la page mémoire. */
  async purgeAll(): Promise<void> {
    const facts = this._facts();
    const entries = this._entries();
    await Promise.all([
      ...facts.map(f => this.deleteFact(f.id).catch(() => {})),
      ...entries.map(e => this.deleteEntry(e.id).catch(() => {})),
    ]);
    this._facts.set([]);
    this._entries.set([]);
  }
}
