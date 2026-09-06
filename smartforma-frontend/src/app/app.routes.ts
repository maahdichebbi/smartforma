import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },

  // Public catalogue
  {
    path: '',
    loadComponent: () =>
      import('./features/learner/home/home.component').then(m => m.LearnerHomeComponent)
  },
  {
    path: 'formations',
    loadComponent: () =>
      import('./features/learner/catalogue/catalogue.component').then(m => m.CatalogueComponent)
  },
  {
    path: 'formations/:id',
    loadComponent: () =>
      import('./features/learner/formation-detail/formation-detail.component').then(m => m.FormationDetailComponent)
  },
  {
    path: 'mes-inscriptions',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['LEARNER'] },
    loadComponent: () =>
      import('./features/learner/my-inscriptions/my-inscriptions.component').then(m => m.MyInscriptionsComponent)
  },

  // Admin — fully separated from learner experience
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
  },
  {
    path: 'admin/formations',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/formations/formation-list.component').then(m => m.FormationAdminListComponent)
  },
  {
    path: 'admin/formations/new',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/formations/formation-form.component').then(m => m.FormationAdminFormComponent)
  },
  {
    path: 'admin/formations/:id/edit',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/formations/formation-form.component').then(m => m.FormationAdminFormComponent)
  },
  {
    path: 'admin/categories',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/categories/category-list.component').then(m => m.CategoryListComponent)
  },
  {
    path: 'admin/sessions',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/sessions/session-list.component').then(m => m.SessionAdminListComponent)
  },
  {
    path: 'admin/inscriptions',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/inscriptions/inscription-list.component').then(m => m.InscriptionAdminListComponent)
  },

  { path: '**', redirectTo: '' }
];
