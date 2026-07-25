import { Component, input } from '@angular/core';

@Component({
  selector: 'app-brand-mark',
  imports: [],
  templateUrl: './brand-mark.html',
  styleUrl: './brand-mark.scss',
})
export class BrandMark {
  readonly variant = input<'default' | 'compact'>('default');
}
