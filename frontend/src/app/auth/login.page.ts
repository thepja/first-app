import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from './auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card narrow">
      <h1>Connexion</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label for="email">Adresse e-mail</label>
        <input id="email" type="email" formControlName="email" autocomplete="username" />

        <label for="password">Mot de passe</label>
        <input id="password" type="password" formControlName="password" autocomplete="current-password" />

        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
        <button type="submit" class="primary" [disabled]="sending()">Se connecter</button>
      </form>
      <p class="hint">Pas encore de compte ? <a routerLink="/register">Créer un compte</a></p>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly error = signal<string | null>(null);
  protected readonly sending = signal(false);

  protected submit(): void {
    if (this.form.invalid) {
      this.error.set('Saisissez votre adresse e-mail et votre mot de passe.');
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.sending.set(true);
    this.error.set(null);
    this.auth.login(email, password).subscribe({
      next: () => void this.router.navigate(['/books']),
      error: (e: unknown) => {
        this.error.set(errorMessage(e, 'Connexion impossible.'));
        this.sending.set(false);
      },
    });
  }
}
