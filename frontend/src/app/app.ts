import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';

type Status = 'loading' | 'up' | 'down';

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly name = signal('');
  protected readonly greeting = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly version = signal<string | null>(null);
  protected readonly status = signal<Status>('loading');

  ngOnInit(): void {
    this.api.health().subscribe({
      next: () => this.status.set('up'),
      error: () => this.status.set('down'),
    });
    this.api.version().subscribe({
      next: (v) => this.version.set(v),
      error: () => this.version.set(null),
    });
  }

  protected greet(): void {
    this.sending.set(true);
    this.error.set(null);
    this.api.hello(this.name().trim()).subscribe({
      next: (message) => {
        this.greeting.set(message);
        this.sending.set(false);
      },
      error: () => {
        this.greeting.set(null);
        this.error.set("Le serveur n'a pas répondu. Réessayez dans un instant.");
        this.sending.set(false);
      },
    });
  }
}
