import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonIcon, IonSpinner } from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  checkmarkCircle, closeCircle, chevronDownOutline, chevronUpOutline,
  buildOutline,
} from 'ionicons/icons';

export interface ToolCall {
  iteration: number;
  name: string;
  arguments: Record<string, unknown>;
  status: 'running' | 'success' | 'error';
  summary?: string;
}

@Component({
  selector: 'tool-call-accordion',
  standalone: true,
  imports: [CommonModule, IonIcon, IonSpinner],
  styles: [`
    :host { display: block; }
    .wrap {
      background: var(--ion-color-step-50);
      border: 1px solid rgba(var(--ion-color-primary-rgb), 0.12);
      border-radius: var(--laa-radius-md);
      margin-bottom: 8px;
      overflow: hidden;
    }
    .row {
      display: flex; align-items: center; gap: 10px;
      padding: 8px 12px;
      font-size: 0.82rem;
      cursor: pointer;
      transition: background 0.12s;
    }
    .row:hover { background: var(--ion-color-step-100); }
    .row + .row { border-top: 1px solid rgba(var(--ion-color-primary-rgb), 0.08); }
    .iter {
      width: 22px; height: 22px;
      display: grid; place-items: center;
      border-radius: 50%;
      background: var(--ion-color-step-100);
      font-size: 0.7rem; font-weight: 600;
      color: var(--ion-color-step-500);
      flex: 0 0 22px;
    }
    .name {
      font-family: var(--laa-font-mono);
      font-weight: 600;
      color: var(--ion-color-primary);
      flex: 0 0 auto;
    }
    .summary {
      flex: 1;
      color: var(--ion-color-step-500);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .status {
      flex: 0 0 auto;
      display: flex; align-items: center;
    }
    .status ion-icon { font-size: 18px; }
    .status .ok { color: var(--ion-color-success); }
    .status .ko { color: var(--ion-color-danger); }
    .chev {
      flex: 0 0 auto; color: var(--ion-color-step-500); font-size: 16px;
      transition: transform 0.2s;
    }
    .chev.open { transform: rotate(180deg); }
    .details {
      padding: 10px 14px 14px 44px;
      background: var(--ion-card-background);
      border-top: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
      font-size: 0.78rem;
    }
    .kv { margin: 6px 0; }
    .kv .k { color: var(--ion-color-step-500); }
    .kv .v {
      font-family: var(--laa-font-mono);
      color: var(--ion-text-color);
      word-break: break-all;
    }
    pre.json {
      background: var(--ion-color-step-50);
      border-radius: var(--laa-radius-sm);
      padding: 8px 10px;
      margin: 4px 0 0;
      font-family: var(--laa-font-mono);
      font-size: 0.75rem;
      overflow-x: auto;
      max-height: 200px;
    }
  `],
  template: `
    <div class="wrap">
      @for (t of calls(); track t.iteration; let idx = $index) {
        <div class="row" (click)="toggle(idx)">
          <div class="iter">{{ t.iteration }}</div>
          <div class="name">{{ t.name }}</div>
          <div class="summary">{{ t.summary ?? '…' }}</div>
          <div class="status">
            @if (t.status === 'running') {
              <ion-spinner name="dots" style="width:18px;height:18px;"></ion-spinner>
            } @else if (t.status === 'success') {
              <ion-icon name="checkmark-circle" class="ok"></ion-icon>
            } @else {
              <ion-icon name="close-circle" class="ko"></ion-icon>
            }
          </div>
          <ion-icon
            name="chevron-down-outline"
            class="chev"
            [class.open]="expanded()[idx]"></ion-icon>
        </div>
        @if (expanded()[idx]) {
          <div class="details">
            <div class="kv">
              <span class="k">Arguments :</span>
              <pre class="json">{{ formatJson(t.arguments) }}</pre>
            </div>
            @if (t.summary) {
              <div class="kv">
                <span class="k">Résumé :</span>
                <div class="v">{{ t.summary }}</div>
              </div>
            }
          </div>
        }
      }
    </div>
  `,
})
export class ToolCallAccordionComponent {
  readonly calls = input.required<ToolCall[]>();

  private readonly _expanded = new Map<number, boolean>();

  constructor() {
    addIcons({ checkmarkCircle, closeCircle, chevronDownOutline, chevronUpOutline, buildOutline });
  }

  expanded(): boolean[] {
    return this.calls().map((_, i) => this._expanded.get(i) ?? false);
  }

  toggle(idx: number): void {
    this._expanded.set(idx, !this._expanded.get(idx));
  }

  formatJson(obj: unknown): string {
    try {
      return JSON.stringify(obj, null, 2);
    } catch {
      return String(obj);
    }
  }
}
