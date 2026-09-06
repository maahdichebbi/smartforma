import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Recommendation } from '../../models/recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/recommendations`;

  /**
   * Retrieves personalized, explainable recommendations for a specific learner.
   */
  getRecommendationsForLearner(
    apprenantId: number,
    limit: number = 4,
    minScore: number = 20
  ): Observable<Recommendation[]> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('minScore', minScore.toString());

    return this.http.get<Recommendation[]>(`${this.baseUrl}/apprenant/${apprenantId}`, { params });
  }
}
