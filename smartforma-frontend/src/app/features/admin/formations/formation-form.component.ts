import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { FormationService } from '../../../core/services/formation.service';
import { CategorieService } from '../../../core/services/categorie.service';
import { ChapitreService } from '../../../core/services/chapitre.service';
import { ToastService } from '../../../core/services/toast.service';
import { Formation, FormationDto, Niveau } from '../../../models/formation.model';
import { Categorie } from '../../../models/categorie.model';
import { Chapitre, ChapitreDto } from '../../../models/chapitre.model';

@Component({
  selector: 'app-formation-form',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule],
  templateUrl: './formation-form.component.html',
  styleUrl: './formation-form.component.css'
})
export class FormationAdminFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formationService = inject(FormationService);
  private readonly categorieService = inject(CategorieService);
  private readonly chapitreService = inject(ChapitreService);
  private readonly toastService = inject(ToastService);

  // Mode & IDs
  readonly isEditMode = signal<boolean>(false);
  readonly formationId = signal<number | null>(null);
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);

  // Data
  readonly categories = signal<Categorie[]>([]);
  readonly currentFormation = signal<Formation | null>(null);
  readonly chapitres = signal<Chapitre[]>([]);

  // Main Formation Form
  formationForm!: FormGroup;

  // Chapter Management (Phase D3)
  readonly showChapterForm = signal<boolean>(false);
  readonly editingChapterId = signal<number | null>(null);
  readonly savingChapter = signal<boolean>(false);
  readonly chapterErrorMessage = signal<string | null>(null);

  chapterTitre = '';
  chapterDescription = '';
  chapterOrdre = 1;

  // Delete Chapter Confirmation
  readonly confirmDeleteChapterId = signal<number | null>(null);
  readonly deletingChapterId = signal<number | null>(null);

  ngOnInit(): void {
    this.initForm();
    this.loadCategories();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.isEditMode.set(true);
      this.formationId.set(id);
      this.loadFormation(id);
    }
  }

  private initForm(): void {
    this.formationForm = this.fb.group({
      titre: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required]],
      prix: [0, [Validators.required, Validators.min(0)]],
      niveau: ['DEBUTANT' as Niveau, [Validators.required]],
      dureeHeures: [10, [Validators.required, Validators.min(1)]],
      categorieId: [null as number | null, [Validators.required]]
    });
  }

  private loadCategories(): void {
    this.categorieService.getAll().subscribe({
      next: cats => this.categories.set(cats)
    });
  }

  private loadFormation(id: number): void {
    this.loading.set(true);
    this.formationService.getById(id).subscribe({
      next: f => {
        this.currentFormation.set(f);
        this.formationForm.patchValue({
          titre: f.titre,
          description: f.description,
          prix: f.prix,
          niveau: f.niveau,
          dureeHeures: f.dureeHeures || 1,
          categorieId: f.categorie?.id || null
        });
        this.loadChapitres(id);
        this.loading.set(false);
      },
      error: () => {
        this.toastService.error('Formation introuvable.', 'Erreur');
        this.loading.set(false);
        this.router.navigate(['/admin/formations']);
      }
    });
  }

  loadChapitres(id: number): void {
    this.chapitreService.getByFormation(id).subscribe({
      next: chaps => {
        // Ensure sorted by ordre asc
        const sorted = [...chaps].sort((a, b) => a.ordre - b.ordre);
        this.chapitres.set(sorted);
      },
      error: () => this.chapitres.set([])
    });
  }

  // ── Formation Submit ────────────────────────────────────────────────────────
  onSubmitFormation(): void {
    if (this.formationForm.invalid) {
      this.formationForm.markAllAsTouched();
      return;
    }

    const formVal = this.formationForm.value;
    const dto: FormationDto = {
      titre: formVal.titre.trim(),
      description: formVal.description.trim(),
      prix: Number(formVal.prix),
      niveau: formVal.niveau,
      dureeHeures: Number(formVal.dureeHeures),
      categorieId: Number(formVal.categorieId)
    };

    this.saving.set(true);

    if (this.isEditMode() && this.formationId()) {
      this.formationService.update(this.formationId()!, dto).subscribe({
        next: (updated) => {
          this.currentFormation.set(updated);
          this.saving.set(false);
          this.toastService.success('Formation modifiée avec succès.', 'Succès');
        },
        error: () => this.saving.set(false)
      });
    } else {
      this.formationService.create(dto).subscribe({
        next: (created) => {
          this.saving.set(false);
          this.toastService.success('Formation créée avec succès ! Vous pouvez maintenant ajouter des chapitres.', 'Créée');
          if (created.id) {
            this.router.navigate(['/admin/formations', created.id, 'edit']);
          } else {
            this.router.navigate(['/admin/formations']);
          }
        },
        error: () => this.saving.set(false)
      });
    }
  }

  // ── Chapter Management (Phase D3) ───────────────────────────────────────────
  openAddChapter(): void {
    this.chapterErrorMessage.set(null);
    this.editingChapterId.set(null);
    this.chapterTitre = '';
    this.chapterDescription = '';
    // Auto-suggest next order
    const list = this.chapitres();
    const maxOrdre = list.length > 0 ? Math.max(...list.map(c => c.ordre)) : 0;
    this.chapterOrdre = maxOrdre + 1;
    this.showChapterForm.set(true);
  }

  openEditChapter(chap: Chapitre): void {
    this.chapterErrorMessage.set(null);
    this.editingChapterId.set(chap.id!);
    this.chapterTitre = chap.titre;
    this.chapterDescription = chap.description ?? '';
    this.chapterOrdre = chap.ordre;
    this.showChapterForm.set(true);
  }

  cancelChapterForm(): void {
    this.showChapterForm.set(false);
    this.editingChapterId.set(null);
    this.chapterErrorMessage.set(null);
  }

  submitChapter(): void {
    if (!this.chapterTitre.trim() || this.chapterOrdre < 1 || !this.formationId()) {
      return;
    }

    const fid = this.formationId()!;
    const dto: ChapitreDto = {
      titre: this.chapterTitre.trim(),
      description: this.chapterDescription.trim() || undefined,
      ordre: Number(this.chapterOrdre)
    };

    this.savingChapter.set(true);
    this.chapterErrorMessage.set(null);

    if (this.editingChapterId()) {
      this.chapitreService.update(fid, this.editingChapterId()!, dto).subscribe({
        next: () => {
          this.toastService.success('Chapitre modifié avec succès.', 'Modifié');
          this.savingChapter.set(false);
          this.cancelChapterForm();
          this.loadChapitres(fid);
        },
        error: (err) => {
          this.savingChapter.set(false);
          const msg = err?.error?.message || 'Erreur lors de la modification du chapitre.';
          this.chapterErrorMessage.set(msg);
        }
      });
    } else {
      this.chapitreService.create(fid, dto).subscribe({
        next: () => {
          this.toastService.success('Chapitre ajouté avec succès.', 'Ajouté');
          this.savingChapter.set(false);
          this.cancelChapterForm();
          this.loadChapitres(fid);
        },
        error: (err) => {
          this.savingChapter.set(false);
          const msg = err?.error?.message || 'Erreur lors de la création du chapitre.';
          this.chapterErrorMessage.set(msg);
        }
      });
    }
  }

  requestDeleteChapter(chapId: number): void {
    this.confirmDeleteChapterId.set(chapId);
  }

  cancelDeleteChapter(): void {
    this.confirmDeleteChapterId.set(null);
  }

  confirmDeleteChapter(chapId: number): void {
    if (!this.formationId()) return;

    this.deletingChapterId.set(chapId);
    this.chapitreService.delete(this.formationId()!, chapId).subscribe({
      next: () => {
        this.toastService.success('Chapitre supprimé.', 'Supprimé');
        this.confirmDeleteChapterId.set(null);
        this.deletingChapterId.set(null);
        this.loadChapitres(this.formationId()!);
      },
      error: () => {
        this.confirmDeleteChapterId.set(null);
        this.deletingChapterId.set(null);
      }
    });
  }

  // Field error helpers
  isFieldInvalid(name: string): boolean {
    const field = this.formationForm.get(name);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  trackByChapter(_: number, c: Chapitre): number {
    return c.id ?? 0;
  }
}
