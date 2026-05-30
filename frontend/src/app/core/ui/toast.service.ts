import { Injectable, inject } from '@angular/core';
import { ToastController } from '@ionic/angular/standalone';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly ctrl = inject(ToastController);

  async info(message: string, duration = 2200): Promise<void> {
    await this.show(message, duration, 'medium');
  }

  async success(message: string, duration = 2000): Promise<void> {
    await this.show(message, duration, 'success');
  }

  async error(err: unknown, fallback = 'Une erreur est survenue'): Promise<void> {
    await this.show(this.toMessage(err, fallback), 3500, 'danger');
  }

  private async show(message: string, duration: number, color: string): Promise<void> {
    const t = await this.ctrl.create({
      message,
      duration,
      color,
      position: 'bottom',
      cssClass: 'laa-toast',
      buttons: [{ text: 'OK', role: 'cancel' }],
    });
    await t.present();
  }

  private toMessage(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) return 'Backend inaccessible. Vérifie que le serveur tourne.';
      if (err.error?.validationErrors?.length) {
        return err.error.validationErrors
          .map((v: any) => `${v.field} : ${v.message}`).join(' • ');
      }
      if (err.error?.message) return err.error.message;
      return `${err.status} ${err.statusText || ''}`.trim();
    }
    if (err instanceof Error) return err.message;
    return fallback;
  }
}
