import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CriarReservaPortalRequest, PortalReserva } from '../models/cliente.model';

/**
 * Agendamento pelo cliente autenticado do portal - a busca de servicos,
 * profissionais e horarios reaproveita PublicBookingService (mesmos
 * endpoints publicos usados pela pagina /agendar/:slug); so a criacao da
 * reserva e o historico usam /api/portal/reservas, autenticado.
 */
@Injectable({ providedIn: 'root' })
export class PortalBookingService {
  private readonly http = inject(HttpClient);

  criarReserva(request: CriarReservaPortalRequest): Observable<PortalReserva> {
    return this.http.post<PortalReserva>(`${environment.apiUrl}/portal/reservas`, request);
  }

  listarMinhasReservas(): Observable<PortalReserva[]> {
    return this.http.get<PortalReserva[]>(`${environment.apiUrl}/portal/reservas`);
  }
}
