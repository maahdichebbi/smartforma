import { Injectable, inject, signal, effect } from '@angular/core';
import { Apprenant } from '../../models/apprenant.model';
import { AuthService } from './auth.service';
import { ApprenantService } from './apprenant.service';

/**
 * Learner profile used by catalogue / inscriptions / MLA.
 * Identity always comes from the authenticated Compte — never from a navbar picker.
 */
@Injectable({
  providedIn: 'root'
})
export class LearnerStateService {
  private readonly auth = inject(AuthService);
  private readonly apprenantService = inject(ApprenantService);
  private readonly LEGACY_KEY = 'smartforma_current_learner';

  readonly currentLearner = signal<Apprenant | null>(null);

  constructor() {
    localStorage.removeItem(this.LEGACY_KEY);

    effect(() => {
      const user = this.auth.currentUser();
      if (user?.role !== 'LEARNER' || user.apprenantId == null) {
        this.currentLearner.set(null);
        return;
      }

      const apprenantId = user.apprenantId;
      const existing = this.currentLearner();
      if (existing?.id !== apprenantId) {
        this.currentLearner.set({
          id: apprenantId,
          nom: user.nom ?? '',
          prenom: user.prenom ?? '',
          email: user.email
        });
        this.apprenantService.getById(apprenantId, { silent: true }).subscribe({
          next: profile => this.currentLearner.set(profile),
          error: () => {}
        });
      }
    });
  }

  setLearner(learner: Apprenant | null): void {
    this.currentLearner.set(learner);
    if (learner) {
      this.auth.patchCurrentUser({
        nom: learner.nom,
        prenom: learner.prenom,
        email: learner.email,
        apprenantId: learner.id ?? this.auth.currentUser()?.apprenantId ?? null
      });
    }
  }
}
