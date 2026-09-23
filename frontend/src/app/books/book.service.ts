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
  createdAt: string;
  updatedAt: string;
}

export type BookInput = Pick<Book, 'title' | 'author' | 'readOn' | 'rating' | 'comment'>;

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly http = inject(HttpClient);

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
}
