import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
  IonIcon, IonMenuButton, IonSegment, IonSegmentButton, IonLabel,
  IonSpinner, IonRefresher, IonRefresherContent,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  trashOutline, addOutline, bulbOutline, personCircleOutline, sparklesOutline,
  closeOutline, checkmarkOutline, pencilOutline, timeOutline, layersOutline,
  flameOutline, downloadOutline,
} from 'ionicons/icons';

import { MemoryService } from './memory.service';
import { MemoryEntryDto, MemoryKind, UserFactDto } from './memory.types';
import { ToastService } from '../../core/ui/toast.service';
import { RelativeDatePipe } from '../../shared/pipes/relative-date.pipe';

type Tab = 'profile' | 'entries';

@Component({
  selector: 'app-memory',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RelativeDatePipe,
    IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonButtons,
    IonIcon, IonMenuButton, IonSegment, IonSegmentButton, IonLabel,
    IonSpinner, IonRefresher, IonRefresherContent,
  ],
  styles: [`
    ion-content { --background: var(--ion-background-color); }
    .wrap {
      max-width: 920px; margin: 0 auto;
      padding: 24px 16px 120px;
    }
    .page-header { margin-bottom: 16px; }
    .page-header h1 { font-size: 1.6rem; font-weight: 700; letter-spacing: -0.02em; margin: 0 0 4px; }
    .page-header p { color: var(--ion-color-step-500); margin: 0; }
    ion-segment {
      margin: 16px 0 8px;
      --background: var(--ion-color-step-50);
      border-radius: var(--laa-radius-md);
    }
    ion-segment-button { --color-checked: var(--ion-color-primary); }

    /* Add fact form */
    .add-fact {
      background: var(--ion-card-background);
      border: 1px dashed rgba(var(--ion-color-primary-rgb), 0.25);
      border-radius: var(--laa-radius-md);
      padding: 12px;
      display: flex; gap: 8px; flex-wrap: wrap;
      margin-bottom: 12px;
    }
    .add-fact input {
      flex: 1; min-width: 160px;
      padding: 10px 12px;
      background: var(--ion-color-step-50);
      color: var(--ion-text-color);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.12);
      border-radius: var(--laa-radius-sm);
      font-family: var(--laa-font-sans);
      font-size: 0.9rem;
      outline: none;
    }
    .add-fact input:focus {
      border-color: var(--ion-color-primary);
      box-shadow: 0 0 0 3px rgba(var(--ion-color-primary-rgb), 0.16);
    }

    /* Fact card */
    .fact-card {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-md);
      padding: 12px 14px;
      margin-bottom: 8px;
      display: flex; gap: 12px; align-items: flex-start;
    }
    .fact-icon {
      width: 32px; height: 32px;
      display: grid; place-items: center;
      border-radius: 8px;
      background: linear-gradient(135deg, var(--laa-brand) 0%, var(--laa-brand-soft) 100%);
      color: #fff; flex-shrink: 0;
    }
    .fact-icon ion-icon { font-size: 16px; }
    .fact-body { flex: 1; min-width: 0; }
    .fact-key {
      font-family: var(--laa-font-mono);
      font-size: 0.78rem;
      color: var(--ion-color-step-500);
      margin-bottom: 2px;
    }
    .fact-value {
      font-size: 0.95rem;
      font-weight: 500;
      word-wrap: break-word;
    }
    .fact-value-edit {
      width: 100%;
      padding: 8px 10px;
      background: var(--ion-color-step-50);
      color: var(--ion-text-color);
      border: 1px solid var(--ion-color-primary);
      border-radius: var(--laa-radius-sm);
      font-family: var(--laa-font-sans);
      font-size: 0.95rem;
      outline: none;
    }
    .fact-meta {
      display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
      font-size: 0.72rem; color: var(--ion-color-step-500);
      margin-top: 6px;
    }
    .src-chip, .conf-chip {
      padding: 2px 6px; border-radius: 999px;
      font-size: 0.65rem; font-weight: 600;
    }
    .src-auto   { background: rgba(245,158,11,0.16); color: #fbbf24; }
    .src-manual { background: rgba(16,185,129,0.16); color: #34d399; }
    .conf-chip  { background: rgba(99,102,241,0.16); color: #818cf8; }

    .fact-actions { display: flex; gap: 4px; }
    .icon-btn {
      --color: var(--ion-color-step-500);
      --padding-start: 6px; --padding-end: 6px;
      height: 32px;
    }
    .icon-btn.danger:hover { --color: var(--ion-color-danger); }
    .icon-btn.ok:hover { --color: var(--ion-color-success); }

    /* Entry card */
    .entry-card {
      background: var(--ion-card-background);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      border-radius: var(--laa-radius-md);
      padding: 14px 16px;
      margin-bottom: 8px;
    }
    .entry-header {
      display: flex; gap: 10px; align-items: center;
      margin-bottom: 8px;
    }
    .kind-chip {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 8px; border-radius: 999px;
      font-size: 0.7rem; font-weight: 600; letter-spacing: 0.02em;
    }
    .kind-EPISODIC { background: rgba(20,184,166,0.16); color: #34d399; }
    .kind-SEMANTIC { background: rgba(139,92,246,0.16); color: #a78bfa; }
    .imp-chip {
      display: inline-flex; align-items: center; gap: 3px;
      font-size: 0.72rem; color: var(--ion-color-step-500);
    }
    .entry-summary {
      font-size: 0.92rem; line-height: 1.5;
      margin-bottom: 6px;
    }
    .entry-meta {
      display: flex; gap: 8px; font-size: 0.72rem;
      color: var(--ion-color-step-500);
    }

    .empty {
      max-width: 480px; margin: 60px auto;
      text-align: center; padding: 16px;
    }
    .empty-icon {
      width: 64px; height: 64px;
      display: grid; place-items: center;
      border-radius: 20px;
      margin: 0 auto 12px;
      background: linear-gradient(135deg, rgba(99,102,241,0.18), rgba(20,184,166,0.18));
      color: var(--ion-color-primary);
    }
    .empty-icon ion-icon { font-size: 28px; }
    .empty h2 { font-size: 1.1rem; font-weight: 700; margin: 0 0 4px; }
    .empty p { color: var(--ion-color-step-500); margin: 0; }
  `],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>Mémoire</ion-title>
        <ion-buttons slot="end">
          <ion-button
            (click)="confirmPurge()"
            color="danger"
            [disabled]="totalCount() === 0"
            aria-label="Tout effacer">
            <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content [fullscreen]="true">
      <ion-refresher slot="fixed" (ionRefresh)="onRefresh($event)">
        <ion-refresher-content></ion-refresher-content>
      </ion-refresher>

      <div class="wrap">
        <div class="page-header">
          <h1>Ce que je sais de toi</h1>
          <p>Profil structuré et souvenirs des conversations passées. Tu peux ajouter, modifier ou oublier ce que tu veux.</p>
        </div>

        <ion-segment [value]="tab()" (ionChange)="tab.set($any($event).detail.value)">
          <ion-segment-button value="profile">
            <ion-icon name="person-circle-outline"></ion-icon>
            <ion-label>Profil ({{ mem.factsCount() }})</ion-label>
          </ion-segment-button>
          <ion-segment-button value="entries">
            <ion-icon name="bulb-outline"></ion-icon>
            <ion-label>Souvenirs ({{ mem.entriesCount() }})</ion-label>
          </ion-segment-button>
        </ion-segment>

        @if (mem.loading()) {
          <div style="display:grid; place-items:center; padding: 40px;">
            <ion-spinner name="dots"></ion-spinner>
          </div>
        } @else if (tab() === 'profile') {
          <!-- Form d'ajout manuel -->
          <div class="add-fact">
            <input
              type="text"
              [(ngModel)]="newKey"
              placeholder="Clé (ex. métier)"
              maxlength="128"/>
            <input
              type="text"
              [(ngModel)]="newValue"
              placeholder="Valeur (ex. développeur)"
              maxlength="4000"
              (keydown.enter)="addFact()"/>
            <ion-button
              fill="solid"
              [disabled]="!canAdd() || saving()"
              (click)="addFact()">
              <ion-icon slot="start" name="add-outline"></ion-icon>
              Ajouter
            </ion-button>
          </div>

          @if (mem.facts().length === 0) {
            <div class="empty">
              <div class="empty-icon"><ion-icon name="person-circle-outline"></ion-icon></div>
              <h2>Aucun fait stocké</h2>
              <p>Ajoute des préférences ou contexte sur toi pour personnaliser tes conversations.</p>
            </div>
          } @else {
            @for (f of mem.facts(); track f.id) {
              <div class="fact-card">
                <div class="fact-icon">
                  <ion-icon name="sparkles-outline"></ion-icon>
                </div>
                <div class="fact-body">
                  <div class="fact-key">{{ f.factKey }}</div>
                  @if (editingId() === f.id) {
                    <input
                      class="fact-value-edit"
                      [(ngModel)]="editValue"
                      (keydown.enter)="saveEdit(f)"
                      (keydown.escape)="cancelEdit()"
                      maxlength="4000"/>
                  } @else {
                    <div class="fact-value">{{ f.factValue }}</div>
                  }
                  <div class="fact-meta">
                    @if (f.source) {
                      <span class="src-chip" [class]="'src-' + f.source">{{ f.source }}</span>
                    }
                    @if (f.confidence !== null) {
                      <span class="conf-chip">conf {{ (f.confidence * 100) | number:'1.0-0' }}%</span>
                    }
                    <span>maj {{ f.updatedAt | relativeDate }}</span>
                  </div>
                </div>
                <div class="fact-actions">
                  @if (editingId() === f.id) {
                    <ion-button class="icon-btn ok" fill="clear" size="small" (click)="saveEdit(f)" aria-label="Valider">
                      <ion-icon name="checkmark-outline" slot="icon-only"></ion-icon>
                    </ion-button>
                    <ion-button class="icon-btn" fill="clear" size="small" (click)="cancelEdit()" aria-label="Annuler">
                      <ion-icon name="close-outline" slot="icon-only"></ion-icon>
                    </ion-button>
                  } @else {
                    <ion-button class="icon-btn" fill="clear" size="small" (click)="startEdit(f)" aria-label="Modifier">
                      <ion-icon name="pencil-outline" slot="icon-only"></ion-icon>
                    </ion-button>
                    <ion-button class="icon-btn danger" fill="clear" size="small" (click)="confirmDeleteFact(f)" aria-label="Oublier">
                      <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
                    </ion-button>
                  }
                </div>
              </div>
            }
          }
        } @else {
          <!-- Entries (souvenirs) -->
          <ion-segment [value]="kindFilter()" (ionChange)="kindFilter.set($any($event).detail.value)" style="margin: 8px 0;">
            <ion-segment-button value="all"><ion-label>Tous</ion-label></ion-segment-button>
            <ion-segment-button value="EPISODIC"><ion-label>Épisodiques</ion-label></ion-segment-button>
            <ion-segment-button value="SEMANTIC"><ion-label>Sémantiques</ion-label></ion-segment-button>
          </ion-segment>

          @if (filteredEntries().length === 0) {
            <div class="empty">
              <div class="empty-icon"><ion-icon name="bulb-outline"></ion-icon></div>
              <h2>Aucun souvenir</h2>
              <p>Les souvenirs sont créés automatiquement après chaque conversation (si la consolidation auto est activée).</p>
            </div>
          } @else {
            @for (e of filteredEntries(); track e.id) {
              <div class="entry-card">
                <div class="entry-header">
                  <span class="kind-chip" [class]="'kind-' + e.kind">
                    @if (e.kind === 'EPISODIC') {
                      <ion-icon name="time-outline"></ion-icon> épisodique
                    } @else {
                      <ion-icon name="layers-outline"></ion-icon> sémantique
                    }
                  </span>
                  @if (e.importance !== null) {
                    <span class="imp-chip">
                      <ion-icon name="flame-outline"></ion-icon> {{ (e.importance * 100) | number:'1.0-0' }}%
                    </span>
                  }
                  <div style="flex: 1;"></div>
                  <ion-button class="icon-btn danger" fill="clear" size="small" (click)="confirmDeleteEntry(e)" aria-label="Oublier">
                    <ion-icon name="trash-outline" slot="icon-only"></ion-icon>
                  </ion-button>
                </div>
                <div class="entry-summary">{{ e.summary }}</div>
                <div class="entry-meta">
                  @if (e.sourceSessionId !== null) {
                    <span>session #{{ e.sourceSessionId }}</span>
                  }
                  <span>{{ e.createdAt | relativeDate }}</span>
                  @if (e.lastAccessedAt) {
                    <span>· consulté {{ e.lastAccessedAt | relativeDate }}</span>
                  }
                </div>
              </div>
            }
          }
        }
      </div>
    </ion-content>
  `,
})
export class MemoryPage implements OnInit {
  readonly mem = inject(MemoryService);
  private readonly toast = inject(ToastService);

  readonly tab = signal<Tab>('profile');
  readonly kindFilter = signal<MemoryKind | 'all'>('all');

  // Form ajout fait
  newKey = '';
  newValue = '';
  readonly saving = signal(false);
  readonly canAdd = computed(() => false); // recalculé manuellement

  // Édition inline
  readonly editingId = signal<number | null>(null);
  editValue = '';

  readonly totalCount = computed(() => this.mem.factsCount() + this.mem.entriesCount());

  readonly filteredEntries = computed(() => {
    const k = this.kindFilter();
    const list = this.mem.entries();
    return k === 'all' ? list : list.filter(e => e.kind === k);
  });

  constructor() {
    addIcons({
      trashOutline, addOutline, bulbOutline, personCircleOutline, sparklesOutline,
      closeOutline, checkmarkOutline, pencilOutline, timeOutline, layersOutline,
      flameOutline, downloadOutline,
    });
    // canAdd doit lire les inputs réactivement — on simule en re-computant à chaque tick
    // Astuce simple : utiliser un getter custom
    Object.defineProperty(this, 'canAdd', {
      value: () => this.newKey.trim().length > 0 && this.newValue.trim().length > 0,
    });
  }

  async ngOnInit(): Promise<void> {
    try { await this.mem.refresh(); }
    catch (err) { await this.toast.error(err, 'Impossible de charger la mémoire.'); }
  }

  async onRefresh(ev: Event): Promise<void> {
    try { await this.mem.refresh(); }
    catch (err) { await this.toast.error(err); }
    finally { (ev.target as HTMLIonRefresherElement).complete(); }
  }

  async addFact(): Promise<void> {
    const key = this.newKey.trim();
    const value = this.newValue.trim();
    if (!key || !value || this.saving()) return;
    this.saving.set(true);
    try {
      await this.mem.upsertFact({ factKey: key, factValue: value });
      this.newKey = '';
      this.newValue = '';
      await this.toast.success('Fait ajouté ✓');
    } catch (err) {
      await this.toast.error(err);
    } finally {
      this.saving.set(false);
    }
  }

  startEdit(f: UserFactDto): void {
    this.editingId.set(f.id);
    this.editValue = f.factValue;
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.editValue = '';
  }

  async saveEdit(f: UserFactDto): Promise<void> {
    const newVal = this.editValue.trim();
    if (!newVal || newVal === f.factValue) {
      this.cancelEdit();
      return;
    }
    try {
      await this.mem.upsertFact({ factKey: f.factKey, factValue: newVal });
      await this.toast.success('Mis à jour ✓');
    } catch (err) {
      await this.toast.error(err);
    } finally {
      this.cancelEdit();
    }
  }

  async confirmDeleteFact(f: UserFactDto): Promise<void> {
    if (!confirm(`Oublier "${f.factKey} : ${f.factValue}" ?`)) return;
    try {
      await this.mem.deleteFact(f.id);
      await this.toast.success('Oublié');
    } catch (err) {
      await this.toast.error(err);
    }
  }

  async confirmDeleteEntry(e: MemoryEntryDto): Promise<void> {
    if (!confirm(`Oublier ce souvenir ?`)) return;
    try {
      await this.mem.deleteEntry(e.id);
      await this.toast.success('Souvenir oublié');
    } catch (err) {
      await this.toast.error(err);
    }
  }

  async confirmPurge(): Promise<void> {
    if (!confirm(`Tout effacer : ${this.mem.factsCount()} fait(s) + ${this.mem.entriesCount()} souvenir(s) ?`)) return;
    try {
      await this.mem.purgeAll();
      await this.toast.success('Mémoire vidée');
    } catch (err) {
      await this.toast.error(err);
    }
  }
}
