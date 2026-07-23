import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';

/** Acoes do admin sobre uma reserva ja existente (ver BookingAdminController no backend). */
@Injectable({ providedIn: 'root' })
export class BookingAdminService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(TokenStorageService);

  marcarComparecimento(bookingId: number, compareceu: boolean): Observable<void> {
    return this.http.patch<void>(`${this.tenantBaseUrl()}/bookings/${bookingId}/comparecimento`, { compareceu });
  }

  cancelar(bookingId: number): Observable<void> {
    return this.http.patch<void>(`${this.tenantBaseUrl()}/bookings/${bookingId}/cancelar`, null);
  }

  private tenantBaseUrl(): string {
    return `${environment.apiUrl}/admin/tenants/${this.tokenStorage.obterTenantId()}`;
  }
}
