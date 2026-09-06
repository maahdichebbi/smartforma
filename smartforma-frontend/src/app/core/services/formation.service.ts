import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Formation, FormationDto, Niveau } from '../../models/formation.model';

@Injectable({
  providedIn: 'root'
})
export class FormationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/formations`;

  getAll(): Observable<Formation[]> {
    return this.http.get<Formation[]>(this.baseUrl);
  }

  getById(id: number): Observable<Formation> {
    return this.http.get<Formation>(`${this.baseUrl}/${id}`);
  }

  /**
   * Unified search endpoint — all params are optional.
   * Backend: GET /api/v1/formations/search?titre=&categorieId=&niveau=
   */
  search(titre?: string | null, categorieId?: number | null, niveau?: string | null): Observable<Formation[]> {
    let params = new HttpParams();
    if (titre && titre.trim()) {
      params = params.set('titre', titre.trim());
    }
    if (categorieId) {
      params = params.set('categorieId', categorieId.toString());
    }
    if (niveau && niveau.trim()) {
      params = params.set('niveau', niveau.trim());
    }
    return this.http.get<Formation[]>(`${this.baseUrl}/search`, { params });
  }

  create(dto: FormationDto): Observable<Formation> {
    return this.http.post<Formation>(this.baseUrl, dto);
  }

  update(id: number, dto: FormationDto): Observable<Formation> {
    return this.http.put<Formation>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
