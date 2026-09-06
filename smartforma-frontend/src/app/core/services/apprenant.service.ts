import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Apprenant, ApprenantDto } from '../../models/apprenant.model';
import { SKIP_ERROR_TOAST } from '../http/skip-error-toast';

@Injectable({
  providedIn: 'root'
})
export class ApprenantService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/apprenants`;

  getAll(): Observable<Apprenant[]> {
    return this.http.get<Apprenant[]>(this.baseUrl);
  }

  getById(id: number, options?: { silent?: boolean }): Observable<Apprenant> {
    const context = options?.silent
      ? new HttpContext().set(SKIP_ERROR_TOAST, true)
      : undefined;
    return this.http.get<Apprenant>(`${this.baseUrl}/${id}`, context ? { context } : {});
  }

  getMe(options?: { silent?: boolean }): Observable<Apprenant> {
    const context = options?.silent
      ? new HttpContext().set(SKIP_ERROR_TOAST, true)
      : undefined;
    return this.http.get<Apprenant>(`${this.baseUrl}/me`, context ? { context } : {});
  }

  create(dto: ApprenantDto): Observable<Apprenant> {
    return this.http.post<Apprenant>(this.baseUrl, dto);
  }

  update(id: number, dto: ApprenantDto): Observable<Apprenant> {
    return this.http.put<Apprenant>(`${this.baseUrl}/${id}`, dto);
  }

  updateMe(dto: ApprenantDto): Observable<Apprenant> {
    return this.http.put<Apprenant>(`${this.baseUrl}/me`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
