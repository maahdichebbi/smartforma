import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormationService } from '../../../core/services/formation.service';
import { CategorieService } from '../../../core/services/categorie.service';
import { SessionService } from '../../../core/services/session.service';
import { ExportService } from '../../../core/services/export.service';
import { Formation } from '../../../models/formation.model';
import { Categorie } from '../../../models/categorie.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  private readonly formationService = inject(FormationService);
  private readonly categorieService = inject(CategorieService);
  private readonly sessionService = inject(SessionService);
  private readonly exportService = inject(ExportService);

  readonly formations = signal<Formation[]>([]);
  readonly categories = signal<Categorie[]>([]);
  readonly loading = signal<boolean>(true);

  readonly totalFormations = signal<number>(0);
  readonly totalCategories = signal<number>(0);
  readonly totalChapters = signal<number>(0);
  readonly totalSessions = signal<number>(0);

  // Export states
  readonly exportingCatalog = signal<boolean>(false);
  readonly exportingApprenants = signal<boolean>(false);
  readonly exportingSessions = signal<boolean>(false);

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.categorieService.getAll().subscribe({
      next: cats => {
        this.categories.set(cats);
        this.totalCategories.set(cats.length);
      }
    });

    this.sessionService.getAll().subscribe({
      next: sessions => {
        this.totalSessions.set(sessions.length);
      }
    });

    this.formationService.getAll().subscribe({
      next: formations => {
        this.formations.set(formations);
        this.totalFormations.set(formations.length);
        // Count total chapters across all formations
        const chapterCount = formations.reduce(
          (sum, f) => sum + (f.chapitres?.length ?? 0), 0
        );
        this.totalChapters.set(chapterCount);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  exportCatalogPdf(): void {
    this.exportingCatalog.set(true);
    this.exportService.exportCatalogPdf().subscribe({
      next: () => this.exportingCatalog.set(false),
      error: () => this.exportingCatalog.set(false)
    });
  }

  exportApprenantsExcel(): void {
    this.exportingApprenants.set(true);
    this.exportService.exportApprenantsExcel().subscribe({
      next: () => this.exportingApprenants.set(false),
      error: () => this.exportingApprenants.set(false)
    });
  }

  exportSessionsExcel(): void {
    this.exportingSessions.set(true);
    this.exportService.exportSessionsExcel().subscribe({
      next: () => this.exportingSessions.set(false),
      error: () => this.exportingSessions.set(false)
    });
  }
}
