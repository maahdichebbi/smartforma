import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Chapitre, ChapitreDto } from '../../models/chapitre.model';

@Injectable({
  providedIn: 'root'
})
export class ChapitreService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Backend nests chapters under the formation:
   *   /api/v1/formations/{formationId}/chapitres
   */
  private baseUrl(formationId: number): string {
    return `${this.apiUrl}/formations/${formationId}/chapitres`;
  }

  getByFormation(formationId: number): Observable<Chapitre[]> {
    return this.http.get<Chapitre[]>(this.baseUrl(formationId));
  }

  getById(formationId: number, chapitreId: number): Observable<Chapitre> {
    return this.http.get<Chapitre>(`${this.baseUrl(formationId)}/${chapitreId}`);
  }

  create(formationId: number, dto: ChapitreDto): Observable<Chapitre> {
    return this.http.post<Chapitre>(this.baseUrl(formationId), dto);
  }

  update(formationId: number, chapitreId: number, dto: ChapitreDto): Observable<Chapitre> {
    return this.http.put<Chapitre>(`${this.baseUrl(formationId)}/${chapitreId}`, dto);
  }

  delete(formationId: number, chapitreId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(formationId)}/${chapitreId}`);
  }
}
