import { Component, input } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-booking-page',
  imports: [MatToolbarModule, MatCardModule],
  templateUrl: './booking-page.html',
  styleUrl: './booking-page.scss',
})
export class BookingPage {
  // preenchido automaticamente a partir do :slug da rota (ver
  // withComponentInputBinding em app.config.ts)
  slug = input.required<string>();
}
