import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'relativeDate', standalone: true, pure: true })
export class RelativeDatePipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) return '';
    const d = typeof value === 'string' ? new Date(value) : value;
    const diff = Date.now() - d.getTime();
    const sec = Math.floor(diff / 1000);
    if (sec < 5) return 'à l’instant';
    if (sec < 60) return `il y a ${sec}s`;
    const min = Math.floor(sec / 60);
    if (min < 60) return `il y a ${min} min`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `il y a ${hr} h`;
    const days = Math.floor(hr / 24);
    if (days < 7) return `il y a ${days} j`;
    return d.toLocaleDateString();
  }
}
