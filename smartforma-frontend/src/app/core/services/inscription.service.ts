import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Inscription, SessionStats } from '../../models/inscription.model';

@Injectable({
  providedIn: 'root'
})
export class InscriptionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inscriptions`;

  getAll(): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(this.baseUrl);
  }

  getById(id: number): Observable<Inscription> {
    return this.http.get<Inscription>(`${this.baseUrl}/${id}`);
  }

  getByApprenant(apprenantId: number): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.baseUrl}/apprenant/${apprenantId}`);
  }

  getBySession(sessionId: number): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.baseUrl}/session/${sessionId}`);
  }

  inscrire(sessionId: number): Observable<Inscription> {
    return this.http.post<Inscription>(`${this.baseUrl}/session/${sessionId}`, {});
  }

  confirmer(id: number): Observable<Inscription> {
    return this.http.patch<Inscription>(`${this.baseUrl}/${id}/confirmer`, {});
  }

  annuler(id: number): Observable<Inscription> {
    return this.http.patch<Inscription>(`${this.baseUrl}/${id}/annuler`, {});
  }

  getWaitingList(sessionId: number): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.baseUrl}/session/${sessionId}/attente`);
  }

  getSessionStats(sessionId: number): Observable<import('../../models/inscription.model').SessionStats> {
    return this.http.get<import('../../models/inscription.model').SessionStats>(`${this.baseUrl}/session/${sessionId}/stats`);
  }
}
