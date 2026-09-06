import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly showPassword = signal(false);
  readonly capsLockOn = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  onPasswordKey(event: KeyboardEvent): void {
    this.capsLockOn.set(event.getModifierState?.('CapsLock') ?? false);
  }

  onPasswordBlur(): void {
    this.capsLockOn.set(false);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: user => {
        this.submitting.set(false);
        this.toast.success(
          user.role === 'ADMIN' ? 'Bienvenue dans l\'espace administrateur.' : 'Connexion réussie.',
          'Connecté'
        );
        void this.router.navigateByUrl(this.resolveRedirect(user.role));
      },
      error: () => this.submitting.set(false)
    });
  }

  private resolveRedirect(role: string): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('//')) {
      if (role === 'ADMIN' && returnUrl.startsWith('/admin')) {
        return returnUrl;
      }
      if (role === 'LEARNER' && !returnUrl.startsWith('/admin')) {
        return returnUrl;
      }
    }
    return this.auth.homePath();
  }
}
