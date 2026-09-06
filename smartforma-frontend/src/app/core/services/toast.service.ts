import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: number;
  type: 'success' | 'error' | 'info' | 'warning';
  title?: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<ToastMessage[]>([]);

  show(type: 'success' | 'error' | 'info' | 'warning', message: string, title?: string, durationMs = 4500): void {
    const id = this.nextId++;
    const toast: ToastMessage = { id, type, message, title };
    this.toasts.update(current => [...current, toast]);

    if (durationMs > 0) {
      setTimeout(() => this.remove(id), durationMs);
    }
  }

  success(message: string, title = 'Succès'): void {
    this.show('success', message, title);
  }

  error(message: string, title = 'Erreur'): void {
    this.show('error', message, title, 6000);
  }

  info(message: string, title = 'Information'): void {
    this.show('info', message, title);
  }

  warning(message: string, title = 'Attention'): void {
    this.show('warning', message, title, 5000);
  }

  remove(id: number): void {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}
