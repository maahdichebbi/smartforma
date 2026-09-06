import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { SKIP_ERROR_TOAST } from '../http/skip-error-toast';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (req.context.get(SKIP_ERROR_TOAST)) {
        return throwError(() => error);
      }
      let errorMessage = 'Une erreur inattendue est survenue.';

      if (error.status === 0) {
        errorMessage = 'Impossible de contacter le serveur. Vérifiez que le backend Spring Boot est en ligne.';
      } else if (error.status === 400) {
        if (error.error && typeof error.error === 'object') {
          if (error.error.message) {
            errorMessage = error.error.message;
          } else {
            // Validation errors map (field -> message)
            const validationMessages = Object.entries(error.error)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join('\n');
            errorMessage = validationMessages || 'Données invalides.';
          }
        } else if (typeof error.error === 'string') {
          errorMessage = error.error;
        }
      } else if (error.status === 401) {
        errorMessage = error.error?.message || 'Authentification requise.';
      } else if (error.status === 403) {
        errorMessage = error.error?.message || 'Vous n\'êtes pas autorisé à accéder à cette ressource.';
      } else if (error.status === 404) {
        errorMessage = error.error?.message || 'Ressource introuvable (404).';
      } else if (error.status === 409) {
        errorMessage = error.error?.message || 'Conflit de données : cette ressource existe déjà.';
      } else if (error.status === 500) {
        errorMessage = error.error?.message || 'Erreur interne du serveur (500).';
      } else if (error.error?.message) {
        errorMessage = error.error.message;
      }

      toastService.error(errorMessage);
      return throwError(() => error);
    })
  );
};
