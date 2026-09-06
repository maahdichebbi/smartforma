import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Categorie, CategorieDto } from '../../models/categorie.model';

@Injectable({
  providedIn: 'root'
})
export class CategorieService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categories`;

  getAll(): Observable<Categorie[]> {
    return this.http.get<Categorie[]>(this.baseUrl);
  }

  getById(id: number): Observable<Categorie> {
    return this.http.get<Categorie>(`${this.baseUrl}/${id}`);
  }

  create(dto: CategorieDto): Observable<Categorie> {
    return this.http.post<Categorie>(this.baseUrl, dto);
  }

  update(id: number, dto: CategorieDto): Observable<Categorie> {
    return this.http.put<Categorie>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
