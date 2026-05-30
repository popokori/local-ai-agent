import { Component, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  IonIcon, IonModal, IonHeader, IonToolbar, IonTitle, IonButtons,
  IonButton, IonContent, ModalController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { closeOutline, documentOutline } from 'ionicons/icons';

import { SourceEvent } from '../../core/api/sse-event.types';

@Component({
  selector: 'source-pill',
  standalone: true,
  imports: [
    CommonModule,
    IonIcon, IonModal, IonHeader, IonToolbar, IonTitle, IonButtons,
    IonButton, IonContent,
  ],
  styles: [`
    :host { display: inline-block; }
    .pill {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 4px 10px 4px 8px;
      border-radius: 999px;
      background: rgba(var(--ion-color-secondary-rgb), 0.16);
      color: var(--ion-color-secondary);
      font-size: 0.75rem;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid rgba(var(--ion-color-secondary-rgb), 0.25);
      transition: background 0.15s;
    }
    .pill:hover { background: rgba(var(--ion-color-secondary-rgb), 0.28); }
    .pill ion-icon { font-size: 14px; }
    .pill .pill-index {
      width: 18px; height: 18px;
      display: grid; place-items: center;
      border-radius: 50%;
      background: var(--ion-color-secondary);
      color: #fff;
      font-size: 0.65rem;
    }
    .modal-body {
      padding: 16px;
      font-family: var(--laa-font-sans);
    }
    .modal-body h3 {
      margin: 0 0 4px; font-size: 1rem; font-weight: 600;
    }
    .modal-body .meta {
      font-size: 0.85rem; color: var(--ion-color-step-500);
      margin-bottom: 16px;
    }
    .modal-body .snippet {
      white-space: pre-wrap; line-height: 1.5;
      background: var(--ion-color-step-50);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.1);
      border-radius: var(--laa-radius-md);
      padding: 12px 14px;
    }
  `],
  template: `
    <button class="pill" type="button" (click)="modal.present()" [attr.aria-label]="'Source ' + src().data.index">
      <span class="pill-index">{{ src().data.index }}</span>
      <ion-icon name="document-outline"></ion-icon>
      <span>{{ src().data.documentName }}</span>
      @if (src().data.page) {
        <span>· p. {{ src().data.page }}</span>
      }
    </button>

    <ion-modal #modal>
      <ng-template>
        <ion-header>
          <ion-toolbar>
            <ion-title>Source {{ src().data.index }}</ion-title>
            <ion-buttons slot="end">
              <ion-button (click)="modal.dismiss()" aria-label="Fermer">
                <ion-icon name="close-outline" slot="icon-only"></ion-icon>
              </ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content>
          <div class="modal-body">
            <h3>{{ src().data.documentName }}</h3>
            <div class="meta">
              @if (src().data.page) { <span>Page {{ src().data.page }}</span> · }
              <span>Score : {{ src().data.score }}</span>
            </div>
            <div class="snippet">{{ src().data.snippet }}</div>
          </div>
        </ion-content>
      </ng-template>
    </ion-modal>
  `,
})
export class SourcePillComponent {
  readonly src = input.required<SourceEvent>();

  constructor() {
    addIcons({ closeOutline, documentOutline });
  }
}
