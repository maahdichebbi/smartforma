import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CategorieService } from '../../../core/services/categorie.service';
import { ToastService } from '../../../core/services/toast.service';
import { Categorie, CategorieDto } from '../../../models/categorie.model';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.css'
})
export class CategoryListComponent implements OnInit {
  private readonly categorieService = inject(CategorieService);
  private readonly toastService = inject(ToastService);

  readonly categories = signal<Categorie[]>([]);
  readonly loading = signal<boolean>(true);

  // Form state
  readonly showForm = signal<boolean>(false);
  readonly editingId = signal<number | null>(null);
  readonly saving = signal<boolean>(false);

  // Form fields
  formNom = '';
  formDescription = '';

  // Delete confirmation
  readonly deletingId = signal<number | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadCategories();
  }

  private loadCategories(): void {
    this.loading.set(true);
    this.categorieService.getAll().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openCreateForm(): void {
    this.editingId.set(null);
    this.formNom = '';
    this.formDescription = '';
    this.showForm.set(true);
  }

  openEditForm(cat: Categorie): void {
    this.editingId.set(cat.id!);
    this.formNom = cat.nom;
    this.formDescription = cat.description ?? '';
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.formNom = '';
    this.formDescription = '';
  }

  submitForm(): void {
    if (!this.formNom.trim()) return;

    const dto: CategorieDto = {
      nom: this.formNom.trim(),
      description: this.formDescription.trim() || undefined
    };

    this.saving.set(true);

    if (this.editingId()) {
      this.categorieService.update(this.editingId()!, dto).subscribe({
        next: () => {
          this.toastService.success('Catégorie modifiée avec succès.', 'Modifié');
          this.cancelForm();
          this.loadCategories();
        },
        error: (err) => {
          this.saving.set(false);
          // Global interceptor handles toast
        }
      });
    } else {
      this.categorieService.create(dto).subscribe({
        next: () => {
          this.toastService.success('Catégorie créée avec succès.', 'Créé');
          this.cancelForm();
          this.loadCategories();
        },
        error: (err) => {
          this.saving.set(false);
        }
      });
    }
  }

  requestDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(id: number): void {
    this.deletingId.set(id);
    this.categorieService.delete(id).subscribe({
      next: () => {
        this.toastService.success('Catégorie supprimée.', 'Supprimé');
        this.confirmDeleteId.set(null);
        this.deletingId.set(null);
        this.loadCategories();
      },
      error: () => {
        this.confirmDeleteId.set(null);
        this.deletingId.set(null);
      }
    });
  }

  trackByCategory(_: number, cat: Categorie): number {
    return cat.id ?? 0;
  }
}
