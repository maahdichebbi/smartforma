import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthUser, LoginRequest, RegisterRequest } from '../../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly TOKEN_KEY = 'smartforma_token';
  private readonly USER_KEY = 'smartforma_user';

  readonly currentUser = signal<AuthUser | null>(this.loadUser());
  readonly isAuthenticated = computed(() => !!this.currentUser() && !!this.getToken());
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');
  readonly isLearner = computed(() => this.currentUser()?.role === 'LEARNER');

  login(request: LoginRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.baseUrl}/login`, request).pipe(
      tap(user => this.persist(user))
    );
  }

  register(request: RegisterRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.baseUrl}/register`, request).pipe(
      tap(user => this.persist(user))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  homePath(): string {
    return this.isAdmin() ? '/admin' : '/';
  }

  /** Keep JWT user snapshot in sync after a profile update. */
  patchCurrentUser(partial: Partial<AuthUser>): void {
    const current = this.currentUser();
    if (!current) {
      return;
    }
    this.persist({ ...current, ...partial, token: current.token });
  }

  private persist(user: AuthUser): void {
    localStorage.setItem(this.TOKEN_KEY, user.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      const token = localStorage.getItem(this.TOKEN_KEY);
      if (!raw || !token) {
        return null;
      }
      return { ...JSON.parse(raw), token };
    } catch {
      return null;
    }
  }
}
