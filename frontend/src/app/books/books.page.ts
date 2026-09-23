import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, concatMap, map, of } from 'rxjs';
import { errorMessage } from '../auth/auth.service';
import { ImageResizer } from '../shared/image-resizer';
import { StarRating } from '../shared/star-rating';
import { Book, BookInput, BookService } from './book.service';

@Component({
  selector: 'app-books-page',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, StarRating],
  templateUrl: './books.page.html',
  styleUrl: './books.page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BooksPage implements OnInit {
  private readonly api = inject(BookService);
  private readonly resizer = inject(ImageResizer);

  protected readonly books = signal<Book[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  /** null : formulaire fermé ; 'new' : ajout ; sinon id du livre modifié. */
  protected readonly editing = signal<number | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly today = new Date().toISOString().slice(0, 10);

  /** Couverture du formulaire : image réduite prête à envoyer, aperçu affiché, suppression demandée. */
  protected readonly coverImage = signal<Blob | null>(null);
  protected readonly coverPreview = signal<string | null>(null);
  protected readonly removeCover = signal(false);
  protected readonly preparingCover = signal(false);
  private previewObjectUrl: string | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revokePreview());
  }

  protected readonly average = computed(() => {
    const list = this.books();
    return list.length ? list.reduce((sum, b) => sum + b.rating, 0) / list.length : null;
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    author: ['', Validators.maxLength(200)],
    readOn: [''],
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', Validators.maxLength(5000)],
  });

  ngOnInit(): void {
    this.api.list().subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: (e: unknown) => {
        this.error.set(errorMessage(e, 'Impossible de charger vos lectures.'));
        this.loading.set(false);
      },
    });
  }

  protected add(): void {
    this.form.reset({ title: '', author: '', readOn: this.today, rating: 0, comment: '' });
    this.resetCover(null);
    this.formError.set(null);
    this.editing.set('new');
  }

  protected edit(book: Book): void {
    this.form.reset({
      title: book.title,
      author: book.author ?? '',
      readOn: book.readOn ?? '',
      rating: book.rating,
      comment: book.comment ?? '',
    });
    this.resetCover(book.coverUrl);
    this.formError.set(null);
    this.editing.set(book.id);
  }

  protected cancel(): void {
    this.resetCover(null);
    this.editing.set(null);
  }

  protected async chooseCover(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // permet de rechoisir le même fichier
    if (!file) {
      return;
    }
    this.preparingCover.set(true);
    this.formError.set(null);
    try {
      const image = await this.resizer.toJpeg(file);
      this.revokePreview();
      this.previewObjectUrl = URL.createObjectURL(image);
      this.coverImage.set(image);
      this.coverPreview.set(this.previewObjectUrl);
      this.removeCover.set(false);
    } catch {
      this.formError.set("Cette image n'a pas pu être lue. Choisissez une image JPEG, PNG ou WebP.");
    } finally {
      this.preparingCover.set(false);
    }
  }

  protected clearCover(): void {
    this.revokePreview();
    this.coverImage.set(null);
    this.coverPreview.set(null);
    this.removeCover.set(true);
  }

  protected save(): void {
    const value = this.form.getRawValue();
    if (!value.title.trim()) {
      this.formError.set('Le titre est obligatoire.');
      return;
    }
    if (value.rating < 1) {
      this.formError.set('Choisissez une note de 1 à 5 étoiles.');
      return;
    }
    if (this.form.invalid) {
      this.formError.set('Vérifiez les champs saisis.');
      return;
    }
    const input: BookInput = {
      title: value.title.trim(),
      author: value.author.trim() || null,
      readOn: value.readOn || null,
      rating: value.rating,
      comment: value.comment.trim() || null,
    };
    const target = this.editing();
    const request = (
      target === 'new' || target === null ? this.api.create(input) : this.api.update(target, input)
    ).pipe(concatMap((saved) => this.saveCover(saved)));

    this.saving.set(true);
    this.formError.set(null);
    request.subscribe({
      next: (saved) => {
        this.books.update((list) => sortBooks([saved, ...list.filter((b) => b.id !== saved.id)]));
        this.resetCover(null);
        this.editing.set(null);
        this.saving.set(false);
      },
      error: (e: unknown) => {
        this.formError.set(errorMessage(e, 'Enregistrement impossible. Vérifiez les champs saisis.'));
        this.saving.set(false);
      },
    });
  }

  protected remove(book: Book): void {
    if (!confirm(`Supprimer « ${book.title} » de vos lectures ?`)) {
      return;
    }
    this.api.delete(book.id).subscribe({
      next: () => this.books.update((list) => list.filter((b) => b.id !== book.id)),
      error: (e: unknown) => this.error.set(errorMessage(e, 'Suppression impossible.')),
    });
  }

  /** Envoie ou supprime la couverture après l'enregistrement du livre. */
  private saveCover(saved: Book): Observable<Book> {
    const image = this.coverImage();
    if (image) {
      return this.api.uploadCover(saved.id, image);
    }
    if (this.removeCover() && saved.coverUrl) {
      return this.api.deleteCover(saved.id).pipe(map(() => ({ ...saved, coverUrl: null })));
    }
    return of(saved);
  }

  private resetCover(currentUrl: string | null): void {
    this.revokePreview();
    this.coverImage.set(null);
    this.coverPreview.set(currentUrl);
    this.removeCover.set(false);
  }

  private revokePreview(): void {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
  }
}

/** Même ordre que le serveur : lectures les plus récentes d'abord, livres sans date en dernier. */
function sortBooks(books: Book[]): Book[] {
  return [...books].sort((a, b) => {
    if (a.readOn !== b.readOn) {
      if (!a.readOn) return 1;
      if (!b.readOn) return -1;
      return b.readOn.localeCompare(a.readOn);
    }
    return b.id - a.id;
  });
}
