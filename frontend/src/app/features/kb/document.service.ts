import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable, firstValueFrom, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DocumentDto } from './kb.types';
import { PageResponse } from '../chat/chat.types';

/** Événements émis pendant un upload. */
export type UploadEvent =
  | { type: 'progress'; loaded: number; total: number; percent: number }
  | { type: 'done'; document: DocumentDto };

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  /**
   * Upload multipart d'un document dans une KB. Renvoie un Observable qui émet
   * la progression puis le document persisté côté backend.
   */
  upload(kbId: number, file: File): Observable<UploadEvent> {
    const form = new FormData();
    form.append('file', file, file.name);
    const req = new HttpRequest('POST', `${this.base}/kbs/${kbId}/documents`, form, {
      reportProgress: true,
    });
    return this.http.request<DocumentDto>(req).pipe(
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          const total = event.total ?? file.size;
          const loaded = event.loaded;
          const percent = total > 0 ? Math.round((loaded / total) * 100) : 0;
          return { type: 'progress', loaded, total, percent } as UploadEvent;
        }
        if (event.type === HttpEventType.Response && event.body) {
          return { type: 'done', document: event.body } as UploadEvent;
        }
        // Type intermédiaire : ignoré côté caller via filter()
        return null as unknown as UploadEvent;
      }),
    );
  }

  list(kbId: number, page = 0, size = 100): Promise<PageResponse<DocumentDto>> {
    return firstValueFrom(
      this.http.get<PageResponse<DocumentDto>>(
        `${this.base}/kbs/${kbId}/documents?page=${page}&size=${size}`,
      ),
    );
  }

  get(id: number): Promise<DocumentDto> {
    return firstValueFrom(
      this.http.get<DocumentDto>(`${this.base}/documents/${id}`),
    );
  }

  delete(id: number): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.base}/documents/${id}`),
    );
  }
}
