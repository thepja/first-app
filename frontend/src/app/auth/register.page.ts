import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, errorMessage } from './auth.service';

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card narrow">
      <h1>Créer un compte</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label for="displayName">Prénom ou pseudo</label>
        <input id="displayName" type="text" formControlName="displayName" autocomplete="nickname" maxlength="80" />

        <label for="email">Adresse e-mail</label>
        <input id="email" type="email" formControlName="email" autocomplete="email" maxlength="254" />

        <label for="password">Mot de passe</label>
        <input
          id="password"
          type="password"
          formControlName="password"
          autocomplete="new-password"
          aria-describedby="password-hint"
          maxlength="128"
        />
        <p id="password-hint" class="hint">8 caractères minimum. Une phrase facile à retenir fait un bon mot de passe.</p>

        @if (error(); as message) {
          <p class="error" role="alert">{{ message }}</p>
        }
        <button type="submit" class="primary" [disabled]="sending()">Créer mon compte</button>
      </form>
      <p class="hint">Déjà inscrit ? <a routerLink="/login">Se connecter</a></p>
    </section>
  `,
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    displayName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });
  protected readonly error = signal<string | null>(null);
  protected readonly sending = signal(false);

  protected submit(): void {
    const c = this.form.controls;
    if (this.form.invalid || !c.displayName.value.trim()) {
      this.error.set(
        c.password.hasError('minlength')
          ? 'Le mot de passe doit faire au moins 8 caractères.'
          : c.email.invalid
            ? 'Saisissez une adresse e-mail valide.'
            : 'Tous les champs sont obligatoires.',
      );
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigate(['/books']),
      error: (e: unknown) => {
        this.error.set(errorMessage(e, 'Vérifiez les informations saisies.'));
        this.sending.set(false);
      },
    });
  }
}
