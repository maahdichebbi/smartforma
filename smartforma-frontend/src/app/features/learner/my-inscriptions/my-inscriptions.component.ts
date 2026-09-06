import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { InscriptionService } from '../../../core/services/inscription.service';
import { LearnerStateService } from '../../../core/services/learner-state.service';
import { ToastService } from '../../../core/services/toast.service';
import { Inscription, StatutInscription } from '../../../models/inscription.model';

@Component({
  selector: 'app-my-inscriptions',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-inscriptions.component.html',
  styleUrl: './my-inscriptions.component.css'
})
export class MyInscriptionsComponent implements OnInit {
  private readonly inscriptionService = inject(InscriptionService);
  readonly learnerState = inject(LearnerStateService);
  private readonly toastService = inject(ToastService);

  readonly inscriptions = signal<Inscription[]>([]);
  readonly loading = signal<boolean>(true);
  readonly activeTab = signal<'ALL' | 'CONFIRMEE' | 'EN_ATTENTE' | 'ANNULEE'>('ALL');

  // Cancel state
  readonly confirmCancelId = signal<number | null>(null);
  readonly cancellingId = signal<number | null>(null);

  // Computed stats
  readonly confirmedCount = computed(() =>
    this.inscriptions().filter(i => i.statut === 'CONFIRMEE').length
  );
  readonly waitingCount = computed(() =>
    this.inscriptions().filter(i => i.statut === 'EN_ATTENTE').length
  );
  readonly cancelledCount = computed(() =>
    this.inscriptions().filter(i => i.statut === 'ANNULEE').length
  );

  // Filtered registrations
  readonly filteredInscriptions = computed(() => {
    const tab = this.activeTab();
    if (tab === 'ALL') return this.inscriptions();
    return this.inscriptions().filter(i => i.statut === tab);
  });

  constructor() {
    // Automatically reload when active learner changes
    effect(() => {
      const learner = this.learnerState.currentLearner();
      if (learner?.id) {
        this.loadInscriptions();
      } else {
        this.inscriptions.set([]);
        this.loading.set(false);
      }
    });
  }

  ngOnInit(): void {
    const learner = this.learnerState.currentLearner();
    if (learner?.id) {
      this.loadInscriptions();
    } else {
      this.loading.set(false);
    }
  }

  loadInscriptions(): void {
    this.loading.set(true);
    this.inscriptionService.getMine().subscribe({
      next: list => {
        this.inscriptions.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  requestCancel(id: number): void {
    this.confirmCancelId.set(id);
  }

  abortCancel(): void {
    this.confirmCancelId.set(null);
  }

  confirmCancel(inscr: Inscription): void {
    if (!inscr.id) return;
    this.cancellingId.set(inscr.id);

    this.inscriptionService.annuler(inscr.id).subscribe({
      next: (res) => {
        this.cancellingId.set(null);
        this.confirmCancelId.set(null);

        if (res.messagePromotion) {
          this.toastService.info(res.messagePromotion, 'Mise à jour de la session');
        } else {
          this.toastService.success('Inscription annulée avec succès.', 'Annulée');
        }

        if (this.learnerState.currentLearner()?.id) {
          this.loadInscriptions();
        }
      },
      error: (err) => {
        this.cancellingId.set(null);
        this.confirmCancelId.set(null);
        const msg = err?.error?.message || 'Erreur lors de l\'annulation.';
        this.toastService.error(msg, 'Erreur');
      }
    });
  }

  formatDate(dateStr?: string | null): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatTime(timeStr?: string | null): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    return `${parts[0]}h${parts[1]}`;
  }

  trackByInscr(_: number, i: Inscription): number {
    return i.id ?? 0;
  }
}
