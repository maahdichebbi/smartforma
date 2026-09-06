import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container" *ngIf="toastService.toasts().length > 0">
      <div
        *ngFor="let toast of toastService.toasts()"
        class="toast"
        [ngClass]="'toast-' + toast.type"
      >
        <div class="toast-content">
          <div class="toast-title" *ngIf="toast.title">{{ toast.title }}</div>
          <div class="toast-message">{{ toast.message }}</div>
        </div>
        <button class="toast-close" (click)="toastService.remove(toast.id)" aria-label="Fermer">
          &times;
        </button>
      </div>
    </div>
  `
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);
}
