import { Component, OnInit, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FormationService } from '../../../core/services/formation.service';
import { CategorieService } from '../../../core/services/categorie.service';
import { LearnerStateService } from '../../../core/services/learner-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { ApprenantService } from '../../../core/services/apprenant.service';
import { ToastService } from '../../../core/services/toast.service';
import { Recommendation } from '../../../models/recommendation.model';
import { Apprenant, ApprenantDto, Niveau } from '../../../models/apprenant.model';

@Component({
  selector: 'app-learner-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class LearnerHomeComponent implements OnInit {
  private readonly formationService = inject(FormationService);
  private readonly categorieService = inject(CategorieService);
  private readonly recommendationService = inject(RecommendationService);
  private readonly apprenantService = inject(ApprenantService);
  private readonly toastService = inject(ToastService);
  readonly learnerState = inject(LearnerStateService);
  readonly auth = inject(AuthService);

  readonly formationCount = signal<number>(0);
  readonly categoryCount = signal<number>(0);
  readonly loading = signal<boolean>(true);

  // Recommendations state
  readonly recommendations = signal<Recommendation[]>([]);
  readonly loadingRecs = signal<boolean>(false);
  readonly selectedRecModal = signal<Recommendation | null>(null);

  // Profile Edit modal state
  readonly showProfileModal = signal<boolean>(false);
  readonly savingProfile = signal<boolean>(false);
  readonly niveaux: Niveau[] = ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'];
  profileModel: ApprenantDto = {
    nom: '',
    prenom: '',
    email: '',
    competences: '',
    interets: '',
    niveau: 'DEBUTANT'
  };

  constructor() {
    // Whenever the active learner changes, reload personalized recommendations
    effect(() => {
      const learner = this.learnerState.currentLearner();
      if (learner && learner.id) {
        this.loadRecommendations();
      } else {
        this.recommendations.set([]);
      }
    });
  }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.formationService.getAll().subscribe({
      next: formations => this.formationCount.set(formations.length)
    });
    this.categorieService.getAll().subscribe({
      next: cats => {
        this.categoryCount.set(cats.length);
        this.loading.set(false);
      }
    });
  }

  loadRecommendations(): void {
    this.loadingRecs.set(true);
    this.recommendationService.getMyRecommendations(4, 15).subscribe({
      next: recs => {
        this.recommendations.set(recs);
        this.loadingRecs.set(false);
      },
      error: () => {
        this.recommendations.set([]);
        this.loadingRecs.set(false);
      }
    });
  }

  openExplainModal(rec: Recommendation): void {
    this.selectedRecModal.set(rec);
  }

  closeExplainModal(): void {
    this.selectedRecModal.set(null);
  }

  openProfileModal(): void {
    const learner = this.learnerState.currentLearner();
    if (!learner) return;

    this.profileModel = {
      nom: learner.nom || '',
      prenom: learner.prenom || '',
      email: learner.email || '',
      competences: learner.competences || '',
      interets: learner.interets || '',
      niveau: learner.niveau || 'DEBUTANT'
    };
    this.showProfileModal.set(true);
  }

  closeProfileModal(): void {
    this.showProfileModal.set(false);
  }

  saveProfile(): void {
    if (!this.learnerState.currentLearner()) return;

    this.savingProfile.set(true);
    this.apprenantService.updateMe(this.profileModel).subscribe({
      next: updated => {
        this.learnerState.setLearner(updated);
        this.savingProfile.set(false);
        this.closeProfileModal();
        this.toastService.success('Profil mis à jour ! Recommandations recalculées.');
        this.loadRecommendations();
      },
      error: err => {
        this.savingProfile.set(false);
        this.toastService.error('Erreur lors de la mise à jour du profil : ' + (err.error?.message || err.message));
      }
    });
  }

  getScoreBadgeClass(score: number): string {
    if (score >= 70) return 'badge-high';
    if (score >= 40) return 'badge-med';
    return 'badge-low';
  }
}
