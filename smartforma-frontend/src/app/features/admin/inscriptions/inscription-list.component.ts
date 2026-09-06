import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InscriptionService } from '../../../core/services/inscription.service';
import { FormationService } from '../../../core/services/formation.service';
import { SessionService } from '../../../core/services/session.service';
import { ToastService } from '../../../core/services/toast.service';
import { ExportService } from '../../../core/services/export.service';
import { Inscription, StatutInscription } from '../../../models/inscription.model';
import { Formation } from '../../../models/formation.model';
import { Session } from '../../../models/session.model';

@Component({
  selector: 'app-admin-inscription-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './inscription-list.component.html',
  styleUrl: './inscription-list.component.css'
})
export class InscriptionAdminListComponent implements OnInit {
  private readonly inscriptionService = inject(InscriptionService);
  private readonly formationService = inject(FormationService);
  private readonly sessionService = inject(SessionService);
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);

  readonly inscriptions = signal<Inscription[]>([]);
  readonly formations = signal<Formation[]>([]);
  readonly sessions = signal<Session[]>([]);
  readonly loading = signal<boolean>(true);
  readonly exporting = signal<boolean>(false);

  // Filters
  searchTerm = '';
  filterStatut: string = 'ALL';
  filterFormationId: number | null = null;

  // Actions state
  readonly actionInProgressId = signal<number | null>(null);
  readonly confirmCancelId = signal<number | null>(null);

  // Promotion Notification banner
  readonly lastPromotionMessage = signal<string | null>(null);

  // Computed statistics
  readonly totalInscriptions = computed(() => this.inscriptions().length);
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
    let list = this.inscriptions();
    const query = this.searchTerm.toLowerCase().trim();

    if (query) {
      list = list.filter(i =>
        (i.apprenant?.nom && i.apprenant.nom.toLowerCase().includes(query)) ||
        (i.apprenant?.prenom && i.apprenant.prenom.toLowerCase().includes(query)) ||
        (i.apprenant?.email && i.apprenant.email.toLowerCase().includes(query)) ||
        (i.session?.formation?.titre && i.session.formation.titre.toLowerCase().includes(query))
      );
    }

    if (this.filterStatut !== 'ALL') {
      list = list.filter(i => i.statut === this.filterStatut);
    }

    if (this.filterFormationId !== null && this.filterFormationId !== undefined) {
      list = list.filter(i => i.session?.formation?.id === Number(this.filterFormationId));
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);

    this.formationService.getAll().subscribe({
      next: forms => this.formations.set(forms)
    });

    this.sessionService.getAll().subscribe({
      next: sess => this.sessions.set(sess)
    });

    this.inscriptionService.getAll().subscribe({
      next: list => {
        this.inscriptions.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  confirmManual(inscr: Inscription): void {
    if (!inscr.id) return;
    this.actionInProgressId.set(inscr.id);

    this.inscriptionService.confirmer(inscr.id).subscribe({
      next: () => {
        this.actionInProgressId.set(null);
        this.toastService.success(
          `Inscription de ${inscr.apprenant?.prenom} ${inscr.apprenant?.nom} confirmée.`,
          'Confirmée'
        );
        this.loadData();
      },
      error: (err) => {
        this.actionInProgressId.set(null);
        const msg = err?.error?.message || 'Impossible de confirmer cette inscription.';
        this.toastService.error(msg, 'Erreur');
      }
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
    this.actionInProgressId.set(inscr.id);
    this.confirmCancelId.set(null);
    this.lastPromotionMessage.set(null);

    this.inscriptionService.annuler(inscr.id).subscribe({
      next: (res) => {
        this.actionInProgressId.set(null);
        if (res.messagePromotion) {
          this.lastPromotionMessage.set(res.messagePromotion);
          this.toastService.info(res.messagePromotion, 'Promotion automatique');
        } else {
          this.toastService.success('Inscription annulée.', 'Annulée');
        }
        this.loadData();
      },
      error: (err) => {
        this.actionInProgressId.set(null);
        const msg = err?.error?.message || 'Erreur lors de l\'annulation.';
        this.toastService.error(msg, 'Erreur');
      }
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterStatut = 'ALL';
    this.filterFormationId = null;
  }

  formatDate(dateStr?: string | null): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  exportInscriptionsExcel(): void {
    this.exporting.set(true);
    const statutParam = this.filterStatut !== 'ALL' ? this.filterStatut : undefined;
    this.exportService.exportInscriptionsExcel(statutParam).subscribe({
      next: () => this.exporting.set(false),
      error: () => this.exporting.set(false)
    });
  }

  trackByInscr(_: number, i: Inscription): number {
    return i.id ?? 0;
  }
}
