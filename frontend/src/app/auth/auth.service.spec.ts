import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService, errorMessage } from './auth.service';
import { HttpErrorResponse } from '@angular/common/http';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('charge la session une seule fois', async () => {
    const first = auth.ensureLoaded();
    const second = auth.ensureLoaded();
    http.expectOne('/api/auth/me').flush({ email: 'a@example.com', displayName: 'Alice' });

    expect(await first).toEqual({ email: 'a@example.com', displayName: 'Alice' });
    expect(await second).toEqual(await first);
    expect(auth.loggedIn()).toBe(true);
  });

  it('considère un 401 comme « non connecté »', async () => {
    const loaded = auth.ensureLoaded();
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(await loaded).toBeNull();
    expect(auth.loggedIn()).toBe(false);
  });

  it('met à jour l’utilisateur à la connexion puis à la déconnexion', async () => {
    auth.login('a@example.com', 'secret123').subscribe();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.body).toEqual({ email: 'a@example.com', password: 'secret123' });
    login.flush({ email: 'a@example.com', displayName: 'Alice' });
    expect(auth.user()?.displayName).toBe('Alice');
    expect(await auth.ensureLoaded()).not.toBeNull();

    auth.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.loggedIn()).toBe(false);
  });

  it('extrait le message des erreurs serveur', () => {
    const conflict = new HttpErrorResponse({ status: 409, error: { detail: 'Un compte existe déjà.' } });
    const invalid = new HttpErrorResponse({ status: 400, error: { detail: 'Invalid request content.' } });
    expect(errorMessage(conflict, 'défaut')).toBe('Un compte existe déjà.');
    expect(errorMessage(invalid, 'défaut')).toBe('défaut');
    expect(errorMessage(new HttpErrorResponse({ status: 0 }), 'défaut')).toContain('ne répond pas');
  });
});
