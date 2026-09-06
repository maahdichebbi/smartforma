import { Component, OnInit, Input, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

import { FormationService } from '../../../core/services/formation.service';
import { SessionService } from '../../../core/services/session.service';
import { InscriptionService } from '../../../core/services/inscription.service';
import { LearnerStateService } from '../../../core/services/learner-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

import { Formation } from '../../../models/formation.model';
import { Session } from '../../../models/session.model';
import { Inscription, SessionStats } from '../../../models/inscription.model';

@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './formation-detail.component.html',
  styleUrl: './formation-detail.component.css'
})
export class FormationDetailComponent implements OnInit {
  /** Route param — automatically bound via withComponentInputBinding() */
  @Input() id!: string;

  private readonly formationService = inject(FormationService);
  private readonly sessionService = inject(SessionService);
  private readonly inscriptionService = inject(InscriptionService);
  readonly learnerState = inject(LearnerStateService);
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  readonly formation = signal<Formation | null>(null);
  readonly sessions = signal<Session[]>([]);
  readonly loadingFormation = signal<boolean>(true);
  readonly loadingSessions = signal<boolean>(true);
  readonly errorFormation = signal<string | null>(null);

  /** Track registering state per session (sessionId → boolean) */
  readonly registeringSessionId = signal<number | null>(null);

  /** Track active/waiting inscriptions for current learner: sessionId -> Inscription */
  readonly myInscriptions = signal<Map<number, Inscription>>(new Map());

  /** Track real-time stats per session: sessionId -> SessionStats */
  readonly sessionStats = signal<Map<number, SessionStats>>(new Map());

  ngOnInit(): void {
    const formationId = Number(this.id);
    if (!formationId || isNaN(formationId)) {
      this.errorFormation.set('Identifiant de formation invalide.');
      this.loadingFormation.set(false);
      return;
    }
    this.loadFormation(formationId);
    this.loadSessions(formationId);
  }

  private loadFormation(id: number): void {
    this.formationService.getById(id).subscribe({
      next: f => {
        this.formation.set(f);
        this.loadingFormation.set(false);
      },
      error: () => {
        this.errorFormation.set('Cette formation est introuvable ou a été supprimée.');
        this.loadingFormation.set(false);
      }
    });
  }

  loadSessions(formationId: number): void {
    this.sessionService.getByFormation(formationId).subscribe({
      next: sessions => {
        this.sessions.set(sessions);
        this.loadingSessions.set(false);

        // Load stats for each session
        sessions.forEach(s => {
          if (s.id) {
            this.loadSessionStats(s.id);
          }
        });

        // If learner is selected, check their current inscriptions
        if (this.learnerState.currentLearner()?.id) {
          this.loadMyInscriptions();
        }
      },
      error: () => {
        this.loadingSessions.set(false);
      }
    });
  }

  private loadSessionStats(sessionId: number): void {
    this.inscriptionService.getSessionStats(sessionId).subscribe({
      next: stats => {
        this.sessionStats.update(map => {
          const newMap = new Map(map);
          newMap.set(sessionId, stats);
          return newMap;
        });
      },
      error: () => {}
    });
  }

  private loadMyInscriptions(): void {
    this.inscriptionService.getMine().subscribe({
      next: inscriptions => {
        const map = new Map<number, Inscription>();
        inscriptions
          .filter(i => i.statut !== 'ANNULEE' && i.session?.id != null)
          .forEach(i => map.set(i.session!.id!, i));
        this.myInscriptions.set(map);
      },
      error: () => {}
    });
  }

  registerForSession(session: Session): void {
    if (!this.auth.isLearner()) {
      this.toastService.warning('Connectez-vous avec un compte apprenant pour vous inscrire.');
      void this.router.navigate(['/login'], {
        queryParams: { returnUrl: this.router.url }
      });
      return;
    }
    if (!session.id) return;

    this.registeringSessionId.set(session.id);
    this.inscriptionService.inscrire(session.id).subscribe({
      next: (inscr) => {
        if (inscr.statut === 'CONFIRMEE') {
          this.toastService.success(inscr.message || 'Votre inscription est confirmée !', 'Place confirmée');
        } else if (inscr.statut === 'EN_ATTENTE') {
          this.toastService.info(
            inscr.message || `Session complète. Vous avez été ajouté à la liste d'attente (position #${inscr.positionFile}).`,
            'Liste d\'attente'
          );
        }

        // Update local map
        this.myInscriptions.update(m => {
          const newMap = new Map(m);
          newMap.set(session.id!, inscr);
          return newMap;
        });

        // Refresh session stats
        this.loadSessionStats(session.id!);
        this.registeringSessionId.set(null);
      },
      error: (err) => {
        this.registeringSessionId.set(null);
        // Error toast handled by interceptor
      }
    });
  }

  getMyInscription(session: Session): Inscription | undefined {
    return session.id ? this.myInscriptions().get(session.id) : undefined;
  }

  isConfirmed(session: Session): boolean {
    return this.getMyInscription(session)?.statut === 'CONFIRMEE';
  }

  isWaiting(session: Session): boolean {
    return this.getMyInscription(session)?.statut === 'EN_ATTENTE';
  }

  getWaitingPosition(session: Session): number | null | undefined {
    return this.getMyInscription(session)?.positionFile;
  }

  getStats(session: Session): SessionStats | undefined {
    return session.id ? this.sessionStats().get(session.id) : undefined;
  }

  getRemainingSeats(session: Session): number {
    const st = this.getStats(session);
    return st ? st.placesDisponibles : session.capacite;
  }

  getOccupiedSeats(session: Session): number {
    const st = this.getStats(session);
    return st ? Number(st.placesOccupees) : 0;
  }

  getWaitingCount(session: Session): number {
    const st = this.getStats(session);
    return st ? Number(st.nombreEnAttente) : 0;
  }

  isSessionFull(session: Session): boolean {
    const st = this.getStats(session);
    return st ? st.estComplete : false;
  }

  isSessionPast(session: Session): boolean {
    return new Date(session.dateDebut) <= new Date();
  }

  niveauLabel(n: string): string {
    switch (n) {
      case 'DEBUTANT': return 'Débutant';
      case 'INTERMEDIAIRE': return 'Intermédiaire';
      case 'AVANCE': return 'Avancé';
      default: return n;
    }
  }

  niveauBadgeClass(n: string): string {
    switch (n) {
      case 'DEBUTANT': return 'badge-success';
      case 'INTERMEDIAIRE': return 'badge-warning';
      case 'AVANCE': return 'badge-danger';
      default: return 'badge-neutral';
    }
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  formatTime(t: string | null | undefined): string {
    if (!t) return '';
    const parts = t.split(':');
    return `${parts[0]}h${parts[1]}`;
  }
}
