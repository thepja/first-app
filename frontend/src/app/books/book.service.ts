import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Book {
  id: number;
  title: string;
  author: string | null;
  readOn: string | null; // AAAA-MM-JJ
  rating: number;
  comment: string | null;
  category: string | null; // code d'une catégorie
  coverUrl: string | null; // change à chaque nouvelle couverture (mise en cache sûre)
  createdAt: string;
  updatedAt: string;
}

export type BookInput = Pick<Book, 'title' | 'author' | 'readOn' | 'rating' | 'comment' | 'category'>;

export interface Category {
  code: string;
  label: string;
}

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  list(): Observable<Book[]> {
    return this.http.get<Book[]>('/api/books');
  }

  create(book: BookInput): Observable<Book> {
    return this.http.post<Book>('/api/books', book);
  }

  update(id: number, book: BookInput): Observable<Book> {
    return this.http.put<Book>(`/api/books/${id}`, book);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/books/${id}`);
  }

  uploadCover(id: number, image: Blob): Observable<Book> {
    const form = new FormData();
    form.append('file', image, 'cover.jpg');
    return this.http.put<Book>(`/api/books/${id}/cover`, form);
  }

  deleteCover(id: number): Observable<void> {
    return this.http.delete<void>(`/api/books/${id}/cover`);
  }
}
