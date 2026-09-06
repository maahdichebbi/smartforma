import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../../models/auth.model';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data['roles'] ?? []) as UserRole[];

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const role = auth.currentUser()?.role;
  if (role && roles.includes(role)) {
    return true;
  }

  return router.createUrlTree([auth.homePath()]);
};
