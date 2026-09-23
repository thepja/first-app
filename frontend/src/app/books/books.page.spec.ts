import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { Book } from './book.service';
import { BooksPage } from './books.page';

const DUNE: Book = {
  id: 1,
  title: 'Dune',
  author: 'Frank Herbert',
  readOn: '2026-03-14',
  rating: 5,
  comment: 'Un classique.',
  createdAt: '2026-03-14T10:00:00Z',
  updatedAt: '2026-03-14T10:00:00Z',
};

describe('BooksPage', () => {
  let http: HttpTestingController;
  let el: HTMLElement;
  let fixture: ReturnType<typeof TestBed.createComponent<BooksPage>>;

  beforeAll(() => registerLocaleData(localeFr));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BooksPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(BooksPage);
    http = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function render(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function type(selector: string, value: string): void {
    const input = el.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('affiche les livres avec leur note et le résumé', async () => {
    http.expectOne('/api/books').flush([DUNE, { ...DUNE, id: 2, title: 'Fondation', rating: 3, comment: null }]);
    await render();

    const titles = [...el.querySelectorAll('.book h3')].map((h) => h.textContent?.trim());
    expect(titles).toEqual(['Dune', 'Fondation']);
    expect(el.querySelector('.summary')?.textContent).toContain('2 livres · note moyenne 4,0 / 5');
    expect(el.querySelector('.comment')?.textContent).toContain('Un classique.');
    expect(el.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('5 sur 5');
  });

  it('invite à ajouter un premier livre', async () => {
    http.expectOne('/api/books').flush([]);
    await render();
    expect(el.textContent).toContain('aucun livre');
  });

  it('ajoute un livre avec sa note et son commentaire', async () => {
    http.expectOne('/api/books').flush([]);
    await render();

    (el.querySelector('button.primary') as HTMLButtonElement).click();
    await render();
    type('#title', '  Dune ');
    type('#author', 'Frank Herbert');
    type('#comment', 'Un classique.');
    (el.querySelectorAll('input[type="radio"]')[3] as HTMLInputElement).click(); // 4 étoiles
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const req = http.expectOne({ method: 'POST', url: '/api/books' });
    expect(req.request.body).toMatchObject({
      title: 'Dune',
      author: 'Frank Herbert',
      rating: 4,
      comment: 'Un classique.',
    });
    req.flush({ ...DUNE, rating: 4 });
    await render();

    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelector('.book h3')?.textContent).toContain('Dune');
  });

  it('exige une note avant d’enregistrer', async () => {
    http.expectOne('/api/books').flush([]);
    await render();

    (el.querySelector('button.primary') as HTMLButtonElement).click();
    await render();
    type('#title', 'Dune');
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    await render();

    http.expectNone({ method: 'POST', url: '/api/books' });
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('note');
  });

  it('supprime un livre après confirmation', async () => {
    http.expectOne('/api/books').flush([DUNE]);
    await render();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    (el.querySelector('button.danger') as HTMLButtonElement).click();
    http.expectOne({ method: 'DELETE', url: '/api/books/1' }).flush(null, { status: 204, statusText: 'No Content' });
    await render();

    expect(el.querySelector('.book')).toBeNull();
  });
});
