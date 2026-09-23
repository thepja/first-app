import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Accès à l'API du serveur Java (même origine en production, proxy en développement). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  hello(name: string): Observable<string> {
    const params = new HttpParams().set('name', name);
    return this.http.get('/hello', { params, responseType: 'text' });
  }

  version(): Observable<string> {
    return this.http.get('/version', { responseType: 'text' });
  }

  health(): Observable<string> {
    return this.http.get('/health', { responseType: 'text' });
  }
}
