import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-toolbar',
  imports: [],
  templateUrl: './page-toolbar.html',
  styleUrl: './page-toolbar.scss',
})
export class PageToolbar {
  readonly title = input<string | null>(null);
}
