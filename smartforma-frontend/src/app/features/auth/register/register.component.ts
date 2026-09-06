import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Niveau } from '../../../models/apprenant.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly submitting = signal(false);
  readonly showPassword = signal(false);
  readonly capsLockOn = signal(false);
  readonly niveaux: Niveau[] = ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'];

  readonly form = this.fb.nonNullable.group({
    nom: ['', [Validators.required]],
    prenom: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    competences: [''],
    interets: [''],
    niveau: this.fb.nonNullable.control<Niveau>('DEBUTANT')
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

    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.auth.register({
      nom: value.nom,
      prenom: value.prenom,
      email: value.email,
      password: value.password,
      competences: value.competences || null,
      interets: value.interets || null,
      niveau: value.niveau
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.success('Votre compte apprenant a été créé.', 'Bienvenue');
        void this.router.navigateByUrl('/');
      },
      error: () => this.submitting.set(false)
    });
  }
}
