import { HttpContextToken } from '@angular/common/http';

/** When true, the error interceptor must not show a toast (background hydrate, etc.). */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);
