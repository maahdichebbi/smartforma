import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css'
})
export class NavbarComponent {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly menuOpen = signal(false);

  initials(): string {
    const user = this.auth.currentUser();
    if (!user) return '?';
    const p = (user.prenom || '').trim();
    const n = (user.nom || '').trim();
    if (p && n) return (p[0] + n[0]).toUpperCase();
    if (p) return p.slice(0, 2).toUpperCase();
    return user.email.slice(0, 2).toUpperCase();
  }

  displayName(): string {
    return this.auth.currentUser()?.prenom || this.auth.currentUser()?.email || '';
  }

  roleLabel(): string {
    return this.auth.isAdmin() ? 'Admin' : 'Apprenant';
  }

  toggleMenu(event: Event): void {
    event.stopPropagation();
    this.menuOpen.update(open => !open);
  }

  logout(): void {
    this.menuOpen.set(false);
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }

  @HostListener('document:click')
  closeMenu(): void {
    this.menuOpen.set(false);
  }
}
