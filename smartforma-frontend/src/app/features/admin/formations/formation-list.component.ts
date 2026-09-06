import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FormationService } from '../../../core/services/formation.service';
import { CategorieService } from '../../../core/services/categorie.service';
import { ToastService } from '../../../core/services/toast.service';
import { Formation, Niveau } from '../../../models/formation.model';
import { Categorie } from '../../../models/categorie.model';

@Component({
  selector: 'app-admin-formation-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './formation-list.component.html',
  styleUrl: './formation-list.component.css'
})
export class FormationAdminListComponent implements OnInit {
  private readonly formationService = inject(FormationService);
  private readonly categorieService = inject(CategorieService);
  private readonly toastService = inject(ToastService);

  readonly formations = signal<Formation[]>([]);
  readonly categories = signal<Categorie[]>([]);
  readonly loading = signal<boolean>(true);

  // Filters
  searchTerm = '';
  selectedCategorieId: number | null = null;
  selectedNiveau: Niveau | '' = '';

  // Delete state
  readonly confirmDeleteId = signal<number | null>(null);
  readonly deletingId = signal<number | null>(null);
  readonly deleteErrorMessage = signal<string | null>(null);

  // Filtered formations computed
  readonly filteredFormations = computed(() => {
    let list = this.formations();
    const query = this.searchTerm.toLowerCase().trim();

    if (query) {
      list = list.filter(f =>
        f.titre.toLowerCase().includes(query) ||
        (f.description && f.description.toLowerCase().includes(query))
      );
    }

    if (this.selectedCategorieId !== null && this.selectedCategorieId !== undefined) {
      list = list.filter(f => f.categorie?.id === Number(this.selectedCategorieId));
    }

    if (this.selectedNiveau) {
      list = list.filter(f => f.niveau === this.selectedNiveau);
    }

    return list;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.deleteErrorMessage.set(null);

    this.categorieService.getAll().subscribe({
      next: cats => this.categories.set(cats)
    });

    this.formationService.getAll().subscribe({
      next: forms => {
        this.formations.set(forms);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedCategorieId = null;
    this.selectedNiveau = '';
  }

  requestDelete(id: number): void {
    this.deleteErrorMessage.set(null);
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
    this.deleteErrorMessage.set(null);
  }

  confirmDelete(id: number): void {
    this.deletingId.set(id);
    this.deleteErrorMessage.set(null);

    this.formationService.delete(id).subscribe({
      next: () => {
        this.toastService.success('La formation et ses chapitres ont été supprimés.', 'Supprimé');
        this.confirmDeleteId.set(null);
        this.deletingId.set(null);
        this.loadData();
      },
      error: (err) => {
        this.deletingId.set(null);
        // If foreign key constraint or sessions attached
        const msg = err?.error?.message ||
          'Impossible de supprimer cette formation car des sessions ou inscriptions y sont rattachées.';
        this.deleteErrorMessage.set(msg);
        this.toastService.error(msg, 'Suppression impossible');
      }
    });
  }

  getNiveauBadgeClass(niveau: Niveau): string {
    switch (niveau) {
      case 'DEBUTANT': return 'badge-success';
      case 'INTERMEDIAIRE': return 'badge-warning';
      case 'AVANCE': return 'badge-danger';
      default: return 'badge-neutral';
    }
  }

  getNiveauLabel(niveau: Niveau): string {
    switch (niveau) {
      case 'DEBUTANT': return 'Débutant';
      case 'INTERMEDIAIRE': return 'Intermédiaire';
      case 'AVANCE': return 'Avancé';
      default: return niveau;
    }
  }

  trackByFormation(_: number, f: Formation): number {
    return f.id ?? 0;
  }
}
