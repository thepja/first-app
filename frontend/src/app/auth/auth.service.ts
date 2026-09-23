import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, firstValueFrom, map, of, tap } from 'rxjs';

export interface User {
  email: string;
  displayName: string;
}

export interface Registration {
  email: string;
  password: string;
  displayName: string;
}

/**
 * Session de l'utilisateur. L'authentification repose sur un cookie de session HttpOnly posé par le serveur :
 * aucun jeton n'est stocké côté navigateur.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly current = signal<User | null>(null);
  private loaded?: Promise<User | null>;

  readonly user = this.current.asReadonly();
  readonly loggedIn = computed(() => this.current() !== null);

  /** Récupère l'utilisateur de la session en cours (une seule fois par chargement de page). */
  ensureLoaded(): Promise<User | null> {
    this.loaded ??= firstValueFrom(
      this.http.get<User>('/api/auth/me').pipe(
        catchError(() => of(null)),
        tap((user) => this.current.set(user)),
      ),
    );
    return this.loaded;
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>('/api/auth/login', { email, password }).pipe(tap((u) => this.setUser(u)));
  }

  register(registration: Registration): Observable<User> {
    return this.http.post<User>('/api/auth/register', registration).pipe(tap((u) => this.setUser(u)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}).pipe(
      map(() => undefined),
      catchError(() => of(undefined)),
      tap(() => this.clear()),
    );
  }

  /** La session a expiré côté serveur. */
  clear(): void {
    this.setUser(null);
  }

  private setUser(user: User | null): void {
    this.current.set(user);
    this.loaded = Promise.resolve(user);
  }
}

/** Message d'erreur lisible à partir d'une réponse ProblemDetail du serveur. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Le serveur ne répond pas. Réessayez dans un instant.';
    }
    const detail = (error.error as { detail?: string } | null)?.detail;
    if (detail && error.status !== 400) {
      return detail;
    }
  }
  return fallback;
}
