import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { FormationService } from '../../../core/services/formation.service';
import { CategorieService } from '../../../core/services/categorie.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { LearnerStateService } from '../../../core/services/learner-state.service';
import { Formation, Niveau } from '../../../models/formation.model';
import { Categorie } from '../../../models/categorie.model';
import { Recommendation } from '../../../models/recommendation.model';

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './catalogue.component.html',
  styleUrl: './catalogue.component.css'
})
export class CatalogueComponent implements OnInit, OnDestroy {
  private readonly formationService = inject(FormationService);
  private readonly categorieService = inject(CategorieService);
  private readonly recommendationService = inject(RecommendationService);
  readonly learnerState = inject(LearnerStateService);
  private readonly route = inject(ActivatedRoute);

  readonly formations = signal<Formation[]>([]);
  readonly categories = signal<Categorie[]>([]);
  readonly recommendationsMap = signal<Map<number, Recommendation>>(new Map());
  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  // Filter state (bound to form controls)
  searchTerm = '';
  selectedCategorieId: string = '';
  selectedNiveau: string = '';

  // Sorting state
  sortBy: 'titre' | 'prix-asc' | 'prix-desc' | 'duree' | 'relevance' = 'titre';

  readonly niveaux: Niveau[] = ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'];

  readonly sortedFormations = computed(() => {
    const list = [...this.formations()];
    switch (this.sortBy) {
      case 'relevance':
        return list.sort((a, b) => {
          const scoreA = this.recommendationsMap().get(a.id ?? -1)?.score ?? 0;
          const scoreB = this.recommendationsMap().get(b.id ?? -1)?.score ?? 0;
          return scoreB - scoreA;
        });
      case 'prix-asc':  return list.sort((a, b) => (a.prix ?? 0) - (b.prix ?? 0));
      case 'prix-desc': return list.sort((a, b) => (b.prix ?? 0) - (a.prix ?? 0));
      case 'duree':     return list.sort((a, b) => (a.dureeHeures ?? 0) - (b.dureeHeures ?? 0));
      default:          return list.sort((a, b) => a.titre.localeCompare(b.titre));
    }
  });

  private readonly searchSubject = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Load recommendations if learner is logged in
    const currentLearner = this.learnerState.currentLearner();
    if (currentLearner && currentLearner.id) {
      this.recommendationService.getMyRecommendations(20, 0).subscribe({
        next: recs => {
          const map = new Map<number, Recommendation>();
          recs.forEach(r => {
            if (r.formation.id) map.set(r.formation.id, r);
          });
          this.recommendationsMap.set(map);
        },
        error: () => {}
      });
    }

    // Load categories
    this.categorieService.getAll().subscribe({
      next: cats => this.categories.set(cats),
      error: () => {}
    });

    // Read query params for pre-filtering from homepage quick links
    this.route.queryParamMap.subscribe(params => {
      const niveau = params.get('niveau');
      if (niveau) {
        this.selectedNiveau = niveau;
      }
      this.doSearch();
    });

    // Debounced search on text changes
    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => this.doSearch());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFilterChange(): void {
    this.doSearch();
  }

  onSearchChange(): void {
    this.searchSubject.next();
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedCategorieId = '';
    this.selectedNiveau = '';
    this.doSearch();
  }

  private doSearch(): void {
    this.loading.set(true);
    this.error.set(null);

    const categorieId = this.selectedCategorieId ? Number(this.selectedCategorieId) : null;

    this.formationService.search(
      this.searchTerm || null,
      categorieId,
      this.selectedNiveau || null
    ).subscribe({
      next: formations => {
        this.formations.set(formations);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les formations. Vérifiez que le serveur est disponible.');
        this.loading.set(false);
      }
    });
  }

  hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.selectedCategorieId || this.selectedNiveau);
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

  trackByFormation(_: number, f: Formation): number {
    return f.id ?? 0;
  }
}
