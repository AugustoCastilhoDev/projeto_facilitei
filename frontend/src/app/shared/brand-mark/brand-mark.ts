import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-brand-mark',
  imports: [MatIconModule],
  templateUrl: './brand-mark.html',
  styleUrl: './brand-mark.scss',
})
export class BrandMark {
  readonly variant = input<'default' | 'compact'>('default');
}
