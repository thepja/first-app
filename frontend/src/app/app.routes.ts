import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './auth/auth.guards';

// Toute nouvelle route doit aussi être ajoutée à SpaController (rechargement de page côté serveur)
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'books' },
  {
    path: 'login',
    title: 'Connexion · Mes lectures',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: 'Créer un compte · Mes lectures',
    canActivate: [guestGuard],
    loadComponent: () => import('./auth/register.page').then((m) => m.RegisterPage),
  },
  {
    path: 'books',
    title: 'Mes lectures',
    canActivate: [authGuard],
    loadComponent: () => import('./books/books.page').then((m) => m.BooksPage),
  },
  { path: '**', redirectTo: 'books' },
];
