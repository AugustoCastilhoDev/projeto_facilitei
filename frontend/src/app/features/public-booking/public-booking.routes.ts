import { Routes } from '@angular/router';
import { BookingPage } from './booking-page/booking-page';

export const PUBLIC_BOOKING_ROUTES: Routes = [
  { path: '', component: BookingPage },
  // resumivel apos F5: ver BookingPage.ngOnInit e PublicBookingController.buscar no backend.
  { path: 'reserva/:bookingId', component: BookingPage },
];
