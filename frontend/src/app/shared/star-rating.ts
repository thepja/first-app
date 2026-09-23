import { ChangeDetectionStrategy, Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Note de 1 à 5 étoiles : champ de formulaire (boutons radio accessibles au clavier) ou simple affichage. */
@Component({
  selector: 'app-star-rating',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => StarRating), multi: true }],
  template: `
    @if (readonly()) {
      <span class="stars" role="img" [attr.aria-label]="value() + ' sur 5'">
        @for (star of stars; track star) {
          <span [class.on]="star <= value()" aria-hidden="true">★</span>
        }
      </span>
    } @else {
      <fieldset class="stars input" [disabled]="disabled()">
        <legend class="visually-hidden">{{ label() }}</legend>
        @for (star of stars; track star) {
          <label [class.on]="star <= value()" [title]="star + ' sur 5'">
            <input
              type="radio"
              class="visually-hidden"
              [name]="name"
              [value]="star"
              [checked]="star === value()"
              (change)="select(star)"
              (blur)="onTouched()"
            />
            <span aria-hidden="true">★</span>
            <span class="visually-hidden">{{ star }} sur 5</span>
          </label>
        }
      </fieldset>
    }
  `,
  styles: `
    .stars { display: inline-flex; gap: 2px; border: 0; margin: 0; padding: 0; font-size: 1.25rem; color: var(--border); }
    .input { font-size: 1.75rem; }
    .input label { cursor: pointer; }
    .input label:has(input:focus-visible) { outline: 2px solid var(--accent); border-radius: 4px; }
    .on { color: var(--star); }
  `,
})
export class StarRating implements ControlValueAccessor {
  private static nextId = 0;

  readonly readonly = input(false);
  readonly label = input('Note');
  readonly rating = input<number | null>(null);

  protected readonly stars = [1, 2, 3, 4, 5];
  protected readonly name = `rating-${StarRating.nextId++}`;
  protected readonly selected = signal(0);
  protected readonly disabled = signal(false);
  protected onChange: (value: number) => void = () => {};
  protected onTouched: () => void = () => {};

  protected value(): number {
    return this.readonly() ? (this.rating() ?? 0) : this.selected();
  }

  protected select(star: number): void {
    this.selected.set(star);
    this.onChange(star);
  }

  writeValue(value: number | null): void {
    this.selected.set(value ?? 0);
  }

  registerOnChange(fn: (value: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }
}
