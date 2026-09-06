import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { SessionService } from '../../../core/services/session.service';
import { FormationService } from '../../../core/services/formation.service';
import { InscriptionService } from '../../../core/services/inscription.service';
import { ToastService } from '../../../core/services/toast.service';
import { ExportService } from '../../../core/services/export.service';
import { Session, SessionDto } from '../../../models/session.model';
import { Formation } from '../../../models/formation.model';
import { Inscription } from '../../../models/inscription.model';

export interface SessionStatusInfo {
  key: 'UPCOMING' | 'IN_PROGRESS' | 'FULL' | 'COMPLETED';
  label: string;
  badgeClass: string;
}

@Component({
  selector: 'app-admin-session-list',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule],
  templateUrl: './session-list.component.html',
  styleUrl: './session-list.component.css'
})
export class SessionAdminListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly sessionService = inject(SessionService);
  private readonly formationService = inject(FormationService);
  private readonly inscriptionService = inject(InscriptionService);
  private readonly toastService = inject(ToastService);
  private readonly exportService = inject(ExportService);

  readonly sessions = signal<Session[]>([]);
  readonly formations = signal<Formation[]>([]);
  readonly inscriptions = signal<Inscription[]>([]);
  readonly loading = signal<boolean>(true);

  // Export state
  readonly exportingPlanning = signal<boolean>(false);
  readonly exportingSessionId = signal<number | null>(null);
  readonly exportingFormat = signal<'pdf' | 'excel' | null>(null);

  // Filters
  filterFormationId: number | null = null;
  filterStatut: string = 'ALL';

  // Form State
  sessionForm!: FormGroup;
  readonly showForm = signal<boolean>(false);
  readonly editingId = signal<number | null>(null);
  readonly saving = signal<boolean>(false);
  readonly formError = signal<string | null>(null);

  // Delete State
  readonly confirmDeleteId = signal<number | null>(null);
  readonly deletingId = signal<number | null>(null);
  readonly deleteError = signal<string | null>(null);

  // Computed Filtered List
  readonly filteredSessions = computed(() => {
    let list = this.sessions();

    if (this.filterFormationId !== null && this.filterFormationId !== undefined) {
      list = list.filter(s => s.formation?.id === Number(this.filterFormationId));
    }

    if (this.filterStatut && this.filterStatut !== 'ALL') {
      list = list.filter(s => {
        const st = this.getSessionStatus(s);
        return st.key === this.filterStatut;
      });
    }

    return list;
  });

  ngOnInit(): void {
    this.initForm();
    this.loadData();
  }

  private initForm(): void {
    this.sessionForm = this.fb.group({
      formationId: [null, [Validators.required]],
      dateDebut: ['', [Validators.required]],
      dateFin: ['', [Validators.required]],
      heureDebut: [''],
      heureFin: [''],
      capacite: [15, [Validators.required, Validators.min(1)]]
    }, { validators: [this.dateOrderValidator, this.timeOrderValidator] });
  }

  // Custom Form Validators
  private dateOrderValidator(group: AbstractControl): ValidationErrors | null {
    const debut = group.get('dateDebut')?.value;
    const fin = group.get('dateFin')?.value;
    if (!debut || !fin) return null;

    if (new Date(fin) <= new Date(debut)) {
      return { dateOrderInvalid: true };
    }
    return null;
  }

  private timeOrderValidator(group: AbstractControl): ValidationErrors | null {
    const hDebut = group.get('heureDebut')?.value;
    const hFin = group.get('heureFin')?.value;
    if (!hDebut || !hFin) return null;

    if (hFin <= hDebut) {
      return { timeOrderInvalid: true };
    }
    return null;
  }

  loadData(): void {
    this.loading.set(true);
    this.deleteError.set(null);

    this.formationService.getAll().subscribe({
      next: forms => this.formations.set(forms)
    });

    this.inscriptionService.getAll().subscribe({
      next: inscrs => this.inscriptions.set(inscrs),
      error: () => this.inscriptions.set([])
    });

    this.sessionService.getAll().subscribe({
      next: sess => {
        this.sessions.set(sess);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  // ── Availability & Status Helpers ──────────────────────────────────────────

  getActiveInscriptions(sessionId: number): number {
    return this.inscriptions().filter(
      i => i.session?.id === sessionId && i.statut !== 'ANNULEE'
    ).length;
  }

  getAvailablePlaces(session: Session): number {
    const active = this.getActiveInscriptions(session.id ?? 0);
    return Math.max(0, session.capacite - active);
  }

  getOccupancyPercentage(session: Session): number {
    if (!session.capacite || session.capacite <= 0) return 0;
    const active = this.getActiveInscriptions(session.id ?? 0);
    return Math.min(100, Math.round((active / session.capacite) * 100));
  }

  getSessionStatus(session: Session): SessionStatusInfo {
    const today = new Date().toISOString().split('T')[0];

    if (today > session.dateFin) {
      return { key: 'COMPLETED', label: 'Terminée', badgeClass: 'badge-neutral' };
    }
    if (today >= session.dateDebut && today <= session.dateFin) {
      return { key: 'IN_PROGRESS', label: 'En cours', badgeClass: 'badge-success' };
    }
    if (this.getAvailablePlaces(session) <= 0) {
      return { key: 'FULL', label: 'Complète', badgeClass: 'badge-warning' };
    }
    return { key: 'UPCOMING', label: 'À venir', badgeClass: 'badge-info' };
  }

  // ── Form Interactions ──────────────────────────────────────────────────────

  openCreateForm(): void {
    this.editingId.set(null);
    this.formError.set(null);

    // Default start date: tomorrow, end date: in 14 days
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const inTwoWeeks = new Date();
    inTwoWeeks.setDate(inTwoWeeks.getDate() + 15);

    this.sessionForm.reset({
      formationId: this.formations().length > 0 ? this.formations()[0].id : null,
      dateDebut: tomorrow.toISOString().split('T')[0],
      dateFin: inTwoWeeks.toISOString().split('T')[0],
      heureDebut: '09:00',
      heureFin: '17:00',
      capacite: 15
    });

    this.showForm.set(true);
  }

  openEditForm(session: Session): void {
    this.editingId.set(session.id!);
    this.formError.set(null);

    this.sessionForm.patchValue({
      formationId: session.formation?.id ?? null,
      dateDebut: session.dateDebut,
      dateFin: session.dateFin,
      heureDebut: session.heureDebut ? session.heureDebut.substring(0, 5) : '',
      heureFin: session.heureFin ? session.heureFin.substring(0, 5) : '',
      capacite: session.capacite
    });

    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.formError.set(null);
  }

  submitForm(): void {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const val = this.sessionForm.value;

    // Check if start date is in past for new session
    const today = new Date().toISOString().split('T')[0];
    if (!this.editingId() && val.dateDebut < today) {
      this.formError.set('La date de début ne peut pas être dans le passé pour une nouvelle session.');
      return;
    }

    const dto: SessionDto = {
      formationId: Number(val.formationId),
      dateDebut: val.dateDebut,
      dateFin: val.dateFin,
      heureDebut: val.heureDebut ? (val.heureDebut.length === 5 ? `${val.heureDebut}:00` : val.heureDebut) : null,
      heureFin: val.heureFin ? (val.heureFin.length === 5 ? `${val.heureFin}:00` : val.heureFin) : null,
      capacite: Number(val.capacite)
    };

    this.saving.set(true);
    this.formError.set(null);

    if (this.editingId()) {
      this.sessionService.update(this.editingId()!, dto).subscribe({
        next: () => {
          this.toastService.success('Session mise à jour avec succès.', 'Modifiée');
          this.saving.set(false);
          this.cancelForm();
          this.loadData();
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err?.error?.message || 'Erreur lors de la modification de la session.';
          this.formError.set(msg);
        }
      });
    } else {
      this.sessionService.create(dto).subscribe({
        next: () => {
          this.toastService.success('Session programmée avec succès.', 'Créée');
          this.saving.set(false);
          this.cancelForm();
          this.loadData();
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err?.error?.message || 'Erreur lors de la création de la session.';
          this.formError.set(msg);
        }
      });
    }
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  requestDelete(id: number): void {
    this.deleteError.set(null);
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
    this.deleteError.set(null);
  }

  confirmDelete(id: number): void {
    this.deletingId.set(id);
    this.deleteError.set(null);

    this.sessionService.delete(id).subscribe({
      next: () => {
        this.toastService.success('Session supprimée.', 'Supprimée');
        this.confirmDeleteId.set(null);
        this.deletingId.set(null);
        this.loadData();
      },
      error: (err) => {
        this.deletingId.set(null);
        const msg = err?.error?.message ||
          'Impossible de supprimer cette session : elle contient des inscriptions actives.';
        this.deleteError.set(msg);
        this.toastService.error(msg, 'Suppression impossible');
      }
    });
  }

  resetFilters(): void {
    this.filterFormationId = null;
    this.filterStatut = 'ALL';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  formatTime(timeStr?: string | null): string {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    return `${parts[0]}h${parts[1]}`;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.sessionForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  exportPlanningExcel(): void {
    this.exportingPlanning.set(true);
    this.exportService.exportSessionsExcel().subscribe({
      next: () => this.exportingPlanning.set(false),
      error: () => this.exportingPlanning.set(false)
    });
  }

  exportAttendancePdf(sessionId: number): void {
    this.exportingSessionId.set(sessionId);
    this.exportingFormat.set('pdf');
    this.exportService.exportSessionAttendancePdf(sessionId).subscribe({
      next: () => {
        this.exportingSessionId.set(null);
        this.exportingFormat.set(null);
      },
      error: () => {
        this.exportingSessionId.set(null);
        this.exportingFormat.set(null);
      }
    });
  }

  exportAttendanceExcel(sessionId: number): void {
    this.exportingSessionId.set(sessionId);
    this.exportingFormat.set('excel');
    this.exportService.exportSessionAttendanceExcel(sessionId).subscribe({
      next: () => {
        this.exportingSessionId.set(null);
        this.exportingFormat.set(null);
      },
      error: () => {
        this.exportingSessionId.set(null);
        this.exportingFormat.set(null);
      }
    });
  }

  trackBySession(_: number, s: Session): number {
    return s.id ?? 0;
  }
}
