import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Booking, CriarBookingRequest } from '../models/booking.model';
import { Profissional } from '../models/profissional.model';
import { ServiceOffering } from '../models/service-offering.model';
import { Slot } from '../models/slot.model';
import { PublicTenant } from '../models/tenant.model';

/** Endpoints publicos (sem autenticacao) consumidos pela pagina de agendamento do cliente final. */
@Injectable({ providedIn: 'root' })
export class PublicBookingService {
  private readonly http = inject(HttpClient);

  buscarTenant(slug: string): Observable<PublicTenant> {
    return this.http.get<PublicTenant>(`${environment.apiUrl}/public/tenants/${slug}`);
  }

  listarServicos(slug: string): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`${environment.apiUrl}/public/tenants/${slug}/services`);
  }

  listarProfissionais(slug: string, serviceId: number): Observable<Profissional[]> {
    return this.http.get<Profissional[]>(`${environment.apiUrl}/public/tenants/${slug}/profissionais`, {
      params: { serviceId },
    });
  }

  listarSlotsDisponiveis(slug: string, data: string): Observable<Slot[]> {
    return this.http.get<Slot[]>(`${environment.apiUrl}/public/tenants/${slug}/slots`, { params: { data } });
  }

  criarBooking(slug: string, request: CriarBookingRequest): Observable<Booking> {
    return this.http.post<Booking>(`${environment.apiUrl}/public/tenants/${slug}/bookings`, request);
  }

  buscarBooking(slug: string, bookingId: number): Observable<Booking> {
    return this.http.get<Booking>(`${environment.apiUrl}/public/tenants/${slug}/bookings/${bookingId}`);
  }
}
