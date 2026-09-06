import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Session, SessionDto } from '../../models/session.model';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/sessions`;

  getAll(): Observable<Session[]> {
    return this.http.get<Session[]>(this.baseUrl);
  }

  getById(id: number): Observable<Session> {
    return this.http.get<Session>(`${this.baseUrl}/${id}`);
  }

  getByFormation(formationId: number): Observable<Session[]> {
    return this.http.get<Session[]>(`${this.baseUrl}/formation/${formationId}`);
  }

  create(dto: SessionDto): Observable<Session> {
    return this.http.post<Session>(this.baseUrl, dto);
  }

  update(id: number, dto: SessionDto): Observable<Session> {
    return this.http.put<Session>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
