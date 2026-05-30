import { Component, OnDestroy, OnInit, ViewChild, ElementRef, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonBackButton, IonSpinner, IonProgressBar,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  cloudUploadOutline, documentTextOutline, trashOutline, checkmarkCircle,
  closeCircle, refreshOutline, alertCircle,
} from 'ionicons/icons';

import { KbService } from './kb.service';
import { DocumentService } from './document.service';
import { KnowledgeBaseDto, DocumentDto, DocumentStatus } from './kb.types';
import { ToastService } from '../../core/ui/toast.service';
import { RelativeDatePipe } from '../../shared/pipes/relative-date.pipe';

interface UploadInProgress {
  fileName: string;
  percent: number;
  error?: string;
}

@Component({
  selector: 'app-kb-detail',
  standalone: true,
  imports: [
    CommonModule, RelativeDatePipe,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonBackButton, IonSpinner, IonProgressBar,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }

    .wrap {
      max-width: 920px; margin: 0 auto;
      padding: 24px 16px 96px;
    }

    .kb-header {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.1);
      border-radius: var(--laa-radius-lg);
      padding: 20px;
      display: flex; gap: 14px;
      margin-bottom: 24px;
    }
    .kb-icon {
      width: 48px; height: 48px;
      display: grid; place-items: center;
      border-radius: 12px;
      color: #fff;
      flex-shrink: 0;
    }
    .kb-icon ion-icon { font-size: 24px; }
    .kb-info h1 { margin: 0 0 4px; font-size: 1.2rem; font-weight: 700; }
    .kb-info p { margin: 0; font-size: 0.85rem; color: var(--ion-color-step-500); }
    .kb-info .specs {
      margin-top: 8px; display: flex; gap: 12px;
      font-size: 0.75rem; color: var(--ion-color-step-500);
    }

    /* Zone d'upload */
    .upload-zone {
      border: 2px dashed rgba(var(--ion-color-primary-rgb), 0.3);
      border-radius: var(--laa-radius-lg);
      padding: 32px 20px;
      text-align: center;
      cursor: pointer;
      transition: background 0.15s, border-color 0.15s;
      margin-bottom: 24px;
      background: rgba(var(--ion-color-primary-rgb), 0.04);
    }
    .upload-zone.dragover {
      border-color: var(--ion-color-primary);
      background: rgba(var(--ion-color-primary-rgb), 0.10);
    }
    .upload-zone .up-icon {
      width: 56px; height: 56px;
      display: grid; place-items: center;
      margin: 0 auto 12px;
      border-radius: 16px;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-accent) 100%);
      color: #fff;
    }
    .upload-zone .up-icon ion-icon { font-size: 28px; }
    .upload-zone h3 { margin: 0 0 4px; font-size: 1rem; font-weight: 600; }
    .upload-zone p { margin: 0; font-size: 0.82rem; color: var(--ion-color-step-500); }
    .upload-zone .browse-link {
      color: var(--ion-color-primary);
      font-weight: 600;
      text-decoration: underline;
    }

    /* Uploads en cours */
    .uploads-list { margin-bottom: 16px; }
    .upload-row {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-md);
      padding: 12px 14px;
      margin-bottom: 8px;
    }
    .upload-row .name {
      display: flex; justify-content: space-between; align-items: center;
      font-size: 0.85rem; font-weight: 500;
      margin-bottom: 6px;
    }
    .upload-row .percent { color: var(--ion-color-step-500); font-size: 0.78rem; }
    ion-progress-bar { --progress-background: var(--ion-color-primary); height: 6px; border-radius: 3px; }

    /* Liste des documents */
    h2.section-title {
      font-size: 1rem; font-weight: 600;
      text-transform: uppercase; letter-spacing: 0.06em;
      color: var(--ion-color-step-500);
      margin: 24px 0 12px;
    }

    .doc-row {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-md);
      padding: 14px 16px;
      display: flex; align-items: center; gap: 12px;
      margin-bottom: 8px;
    }
    .doc-row .doc-icon {
      width: 36px; height: 36px;
      display: grid; place-items: center;
      border-radius: 10px;
      background: rgba(var(--ion-color-primary-rgb), 0.10);
      color: var(--ion-color-primary);
      flex-shrink: 0;
    }
    .doc-row .doc-info { flex: 1; min-width: 0; }
    .doc-row .doc-name {
      font-weight: 600; font-size: 0.92rem;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .doc-row .doc-meta {
      font-size: 0.75rem; color: var(--ion-color-step-500);
      display: flex; gap: 8px; align-items: center; margin-top: 2px;
    }
    .status-pill {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 3px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600;
    }
    .status-UPLOADED { background: rgba(99,102,241,0.16); color: #818cf8; }
    .status-PARSING  { background: rgba(245,158,11,0.16); color: #fbbf24; }
    .status-INDEXED  { background: rgba(16,185,129,0.16); color: #34d399; }
    .status-FAILED   { background: rgba(239,68,68,0.16); color: #f87171; }
    .status-pill ion-spinner { width: 12px; height: 12px; }
    .doc-actions {
      display: flex; gap: 4px;
    }
    .delete-btn {
      --color: var(--ion-color-step-500);
      --background-hover: rgba(239,68,68,0.1);
      --color-hover: var(--ion-color-danger);
    }
    .empty-docs {
      text-align: center; padding: 32px 16px;
      color: var(--ion-color-step-500);
    }
    .error-banner {
      background: rgba(239,68,68,0.08);
      border: 1px solid rgba(239,68,68,0.25);
      border-radius: var(--laa-radius-md);
      padding: 10px 14px;
      font-size: 0.82rem;
      color: var(--ion-color-danger);
      margin-top: 6px;
    }

    input[type="file"] { display: none; }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button defaultHref="/kbs"></ion-back-button>
        </ion-buttons>
        <ion-title>{{ kb()?.name || 'KB' }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <div class="wrap">
        @if (loading()) {
          <div style="display:grid; place-items:center; padding: 60px;">
            <ion-spinner name="dots"></ion-spinner>
          </div>
        } @else if (kb(); as k) {
          <div class="kb-header">
            <div class="kb-icon" [style.background]="domainGradient(k.domain)">
              <ion-icon name="document-text-outline"></ion-icon>
            </div>
            <div class="kb-info">
              <h1>{{ k.name }}</h1>
              <p>{{ k.description || 'Pas de description' }}</p>
              <div class="specs">
                <span>{{ domainLabel(k.domain) }}</span>
                <span>· {{ k.embeddingModel }} ({{ k.embeddingDim }} dim)</span>
                <span>· {{ k.createdAt | relativeDate }}</span>
              </div>
            </div>
          </div>

          <!-- Zone upload drag-drop -->
          <div
            class="upload-zone"
            [class.dragover]="isDragOver()"
            (click)="fileInput.click()"
            (dragover)="onDragOver($event)"
            (dragleave)="onDragLeave($event)"
            (drop)="onDrop($event)">
            <div class="up-icon">
              <ion-icon name="cloud-upload-outline"></ion-icon>
            </div>
            <h3>Glisse tes fichiers ici</h3>
            <p>ou <span class="browse-link">choisis depuis ton ordinateur</span></p>
            <p style="margin-top:6px;">PDF, DOCX, TXT, MD (max 25 Mo par fichier)</p>
            <input
              type="file"
              #fileInput
              multiple
              accept=".pdf,.docx,.txt,.md,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,text/markdown"
              (change)="onFilesPicked($event)"/>
          </div>

          @if (uploads().length > 0) {
            <h2 class="section-title">Upload en cours</h2>
            <div class="uploads-list">
              @for (u of uploads(); track u.fileName) {
                <div class="upload-row">
                  <div class="name">
                    <span>{{ u.fileName }}</span>
                    <span class="percent">{{ u.percent }}%</span>
                  </div>
                  <ion-progress-bar [value]="u.percent / 100"></ion-progress-bar>
                  @if (u.error) {
                    <div class="error-banner">{{ u.error }}</div>
                  }
                </div>
              }
            </div>
          }

          <h2 class="section-title">Documents ({{ documents().length }})</h2>
          @if (documents().length === 0) {
            <div class="empty-docs">
              <p>Aucun document encore. Uploade ton premier PDF pour activer le RAG.</p>
            </div>
          } @else {
            @for (d of documents(); track d.id) {
              <div class="doc-row">
                <div class="doc-icon">
                  <ion-icon name="document-text-outline"></ion-icon>
                </div>
                <div class="doc-info">
                  <div class="doc-name">{{ d.fileName }}</div>
                  <div class="doc-meta">
                    <span class="status-pill" [class]="'status-' + d.status">
                      @if (d.status === 'PARSING') {
                        <ion-spinner name="dots"></ion-spinner> Indexation…
                      } @else if (d.status === 'INDEXED') {
                        <ion-icon name="checkmark-circle"></ion-icon> Indexé
                      } @else if (d.status === 'FAILED') {
                        <ion-icon name="close-circle"></ion-icon> Échec
                      } @else {
                        En attente
                      }
                    </span>
                    @if (d.pageCount !== null) { <span>· {{ d.pageCount }} p.</span> }
                    @if (d.chunkCount !== null) { <span>· {{ d.chunkCount }} chunks</span> }
                    <span>· {{ formatSize(d.sizeBytes) }}</span>
                    <span>· {{ d.createdAt | relativeDate }}</span>
                  </div>
                  @if (d.status === 'FAILED' && d.error) {
                    <div class="error-banner">{{ d.error }}</div>
                  }
                </div>
                <div class="doc-actions">
                  <ion-button
                    fill="clear"
                    size="small"
                    class="delete-btn"
                    (click)="confirmDelete(d)"
                    aria-label="Supprimer">
                    <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
                  </ion-button>
                </div>
              </div>
            }
          }
        }
      </div>
    </ion-content>
  `,
})
export class KbDetailPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly kbService = inject(KbService);
  private readonly docService = inject(DocumentService);
  private readonly toast = inject(ToastService);

  readonly kb = signal<KnowledgeBaseDto | null>(null);
  readonly documents = signal<DocumentDto[]>([]);
  readonly uploads = signal<UploadInProgress[]>([]);
  readonly loading = signal(true);
  readonly isDragOver = signal(false);

  private pollTimer?: ReturnType<typeof setInterval>;
  private kbId = 0;

  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  constructor() {
    addIcons({
      cloudUploadOutline, documentTextOutline, trashOutline, checkmarkCircle,
      closeCircle, refreshOutline, alertCircle,
    });
  }

  async ngOnInit(): Promise<void> {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) { await this.router.navigate(['/kbs']); return; }
    this.kbId = Number(idParam);

    try {
      const [kb, page] = await Promise.all([
        this.kbService.get(this.kbId),
        this.docService.list(this.kbId),
      ]);
      this.kb.set(kb);
      this.documents.set(page.content);
      this.maybeStartPolling();
    } catch (err) {
      await this.toast.error(err, 'KB introuvable.');
      await this.router.navigate(['/kbs']);
    } finally {
      this.loading.set(false);
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ─── Upload ─────────────────────────────────────────────────────────

  onDragOver(ev: DragEvent): void {
    ev.preventDefault();
    this.isDragOver.set(true);
  }
  onDragLeave(ev: DragEvent): void {
    ev.preventDefault();
    this.isDragOver.set(false);
  }
  onDrop(ev: DragEvent): void {
    ev.preventDefault();
    this.isDragOver.set(false);
    const files = ev.dataTransfer?.files;
    if (files) this.uploadFiles(Array.from(files));
  }
  onFilesPicked(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    if (!input.files) return;
    this.uploadFiles(Array.from(input.files));
    input.value = ''; // permet de re-uploader le même fichier
  }

  private uploadFiles(files: File[]): void {
    for (const file of files) this.uploadOne(file);
  }

  private uploadOne(file: File): void {
    if (file.size > 25 * 1024 * 1024) {
      this.toast.error(new Error(`${file.name} dépasse 25 Mo`));
      return;
    }
    const inProgress: UploadInProgress = { fileName: file.name, percent: 0 };
    this.uploads.update(list => [...list, inProgress]);

    this.docService.upload(this.kbId, file).subscribe({
      next: ev => {
        if (!ev) return;
        if (ev.type === 'progress') {
          this.uploads.update(list =>
            list.map(u => u.fileName === file.name ? { ...u, percent: ev.percent } : u),
          );
        } else if (ev.type === 'done') {
          this.documents.update(list => [ev.document, ...list]);
          this.uploads.update(list => list.filter(u => u.fileName !== file.name));
          this.maybeStartPolling();
        }
      },
      error: async err => {
        this.uploads.update(list =>
          list.map(u =>
            u.fileName === file.name ? { ...u, error: this.errToText(err), percent: 0 } : u),
        );
        await this.toast.error(err, `Upload échoué : ${file.name}`);
      },
    });
  }

  private errToText(err: unknown): string {
    if (err instanceof Error) return err.message;
    if (typeof err === 'object' && err && 'message' in err) return String((err as any).message);
    return 'Erreur upload';
  }

  // ─── Polling status ───────────────────────────────────────────────

  private maybeStartPolling(): void {
    const inFlight = this.documents().some(d =>
      d.status === 'UPLOADED' || d.status === 'PARSING',
    );
    if (inFlight && !this.pollTimer) {
      this.pollTimer = setInterval(() => this.pollOnce(), 2000);
    }
    if (!inFlight) this.stopPolling();
  }

  private async pollOnce(): Promise<void> {
    const pending = this.documents().filter(d =>
      d.status === 'UPLOADED' || d.status === 'PARSING',
    );
    if (pending.length === 0) {
      this.stopPolling();
      return;
    }
    for (const d of pending) {
      try {
        const updated = await this.docService.get(d.id);
        this.documents.update(list =>
          list.map(x => x.id === d.id ? updated : x),
        );
      } catch {
        // un échec de poll occasionnel n'arrête pas la routine
      }
    }
    this.maybeStartPolling();
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = undefined;
    }
  }

  // ─── Delete ────────────────────────────────────────────────────────

  async confirmDelete(d: DocumentDto): Promise<void> {
    if (!confirm(`Supprimer "${d.fileName}" ?`)) return;
    try {
      await this.docService.delete(d.id);
      this.documents.update(list => list.filter(x => x.id !== d.id));
      await this.toast.success('Document supprimé');
    } catch (err) {
      await this.toast.error(err);
    }
  }

  // ─── Utils ─────────────────────────────────────────────────────────

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }
  domainLabel(d: string): string {
    return {
      GENERIC: 'Général', SCIENCE: 'Science', BIOLOGY: 'Biologie',
      CHEMISTRY: 'Chimie', MATHEMATICS: 'Maths', MEDICAL: 'Médical',
      COMPUTER_SCIENCE: 'Informatique',
    }[d] ?? d;
  }
  domainGradient(d: string): string {
    return ({
      GENERIC:           'linear-gradient(135deg, #6366f1, #818cf8)',
      SCIENCE:           'linear-gradient(135deg, #6366f1, #14b8a6)',
      BIOLOGY:           'linear-gradient(135deg, #10b981, #34d399)',
      CHEMISTRY:         'linear-gradient(135deg, #f59e0b, #fbbf24)',
      MATHEMATICS:       'linear-gradient(135deg, #8b5cf6, #a78bfa)',
      MEDICAL:           'linear-gradient(135deg, #ef4444, #f87171)',
      COMPUTER_SCIENCE:  'linear-gradient(135deg, #06b6d4, #67e8f9)',
    } as Record<string, string>)[d] ?? 'linear-gradient(135deg, #6366f1, #818cf8)';
  }
}
