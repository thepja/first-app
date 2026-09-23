import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let http: HttpTestingController;
  let el: HTMLElement;
  let fixture: ReturnType<typeof TestBed.createComponent<LoginPage>>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(LoginPage);
    http = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function type(selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): void {
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
  }

  it('connecte l’utilisateur et ouvre ses lectures', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    type('#email', 'a@example.com');
    type('#password', 'secret123');
    submit();

    http.expectOne('/api/auth/login').flush({ email: 'a@example.com', displayName: 'Alice' });
    expect(navigate).toHaveBeenCalledWith(['/books']);
  });

  it('affiche le message du serveur en cas d’échec', async () => {
    type('#email', 'a@example.com');
    type('#password', 'mauvais');
    submit();

    http
      .expectOne('/api/auth/login')
      .flush({ detail: 'Adresse e-mail ou mot de passe incorrect.' }, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('mot de passe incorrect');
  });

  it('ne sollicite pas le serveur si le formulaire est incomplet', async () => {
    submit();
    await fixture.whenStable();

    http.expectNone('/api/auth/login');
    expect(el.querySelector('[role="alert"]')).not.toBeNull();
  });
});
