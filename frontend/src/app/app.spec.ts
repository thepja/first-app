import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    http = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function render(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('affiche la version et l’état du serveur', async () => {
    http.expectOne('/health').flush('OK');
    http.expectOne('/version').flush('1.2.3');
    await render();

    expect(el.querySelector('.status')?.textContent).toContain('Serveur disponible');
    expect(el.querySelector('footer')?.textContent).toContain('Version 1.2.3');
  });

  it('signale un serveur indisponible', async () => {
    http.expectOne('/health').flush('', { status: 503, statusText: 'Unavailable' });
    http.expectOne('/version').flush('', { status: 503, statusText: 'Unavailable' });
    await render();

    expect(el.querySelector('.status')?.textContent).toContain('Serveur indisponible');
    expect(el.querySelector('footer')?.textContent).toContain('Version inconnue');
  });

  it('envoie le prénom et affiche la réponse', async () => {
    http.expectOne('/health').flush('OK');
    http.expectOne('/version').flush('1.2.3');

    const input = el.querySelector('input') as HTMLInputElement;
    input.value = '  Alice ';
    input.dispatchEvent(new Event('input'));
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = http.expectOne((r) => r.url === '/hello');
    expect(req.request.params.get('name')).toBe('Alice');
    req.flush('Hello, Alice!');
    await render();

    expect(el.querySelector('.greeting')?.textContent).toContain('Hello, Alice!');
  });

  it('affiche une erreur si l’appel échoue', async () => {
    http.expectOne('/health').flush('OK');
    http.expectOne('/version').flush('1.2.3');

    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne((r) => r.url === '/hello').flush('', { status: 500, statusText: 'Error' });
    await render();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain("n'a pas répondu");
    expect(el.querySelector('.greeting')).toBeNull();
  });
});
